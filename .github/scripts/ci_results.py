"""CI matrix ownership, staged artifact identity, and fail-closed result validation.

Only XML test cases count as execution. No retries or skip allowances are applied.
Run from the repository root; Python's standard library is sufficient on CI.
"""

import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import tomllib
from typing import Final
import xml.etree.ElementTree as ET

ROOT: Final = Path(__file__).resolve().parents[2]
VERSION: Final = "2.0.0-dev"


class InvalidEvidence(RuntimeError):
    """An artifact violates the CI evidence contract."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise InvalidEvidence(message)


def matrix() -> dict[str, list[str]]:
    """Read each version from its production owner, including the Kotlin floor."""
    tools = tomllib.loads((ROOT / "mise.toml").read_text())["tools"]
    catalog = tomllib.loads((ROOT / "gradle/buildLibs.versions.toml").read_text())
    properties = dict(
        line.split("=", 1) for line in (ROOT / "gradle.properties").read_text().splitlines()
        if "=" in line
    )
    floor = properties["kotlinReleaseVersion"].strip() + ".0"
    current = catalog["libraries"]["plugin-kotlin-jvm"].rsplit(":", 1)[1]
    return {"gradle": tools["gradle"], "kotlin": list(dict.fromkeys([floor, current]))}


def expected_lanes() -> list[str]:
    versions = matrix()
    return ["host-21", "host-25", "host-27", "abi"] + [
        f"runtime-{v}" for v in versions["gradle"]
    ] + [f"kotlin-{v}" for v in versions["kotlin"]]


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def staged_identity() -> dict[str, str]:
    repository = ROOT / "build/verification-repository"
    jars = list(repository.glob(f"**/atrium-gradle-testkit-{VERSION}.jar"))
    require(len(jars) == 1, "Expected exactly one staged production JAR")
    return {"sourceSha": os.environ["GITHUB_SHA"], "libraryJarSha256": digest(jars[0])}


def stage() -> None:
    path = ROOT / "build/staged-identity.json"
    path.write_text(json.dumps(staged_identity(), indent=2) + "\n")


def check_stage() -> None:
    expected = json.loads((ROOT / "build/staged-identity.json").read_text())
    require(staged_identity() == expected, "Staged JAR or source SHA mismatch")


def record(lane: str, source: Path) -> None:
    """Copy raw evidence into one dimension-specific artifact, preserving paths."""
    target = ROOT / "build/ci-results" / lane
    target.mkdir(parents=True, exist_ok=True)
    for pattern in ["**/TEST-*.xml", "**/runtime.json", "**/jacoco*.xml", "**/output.txt"]:
        for path in source.glob(pattern):
            relative = path.relative_to(source)
            destination = target / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(path, destination)
    receipt = {"sourceSha": os.environ["GITHUB_SHA"], "lane": lane}
    if lane.startswith(("runtime-", "kotlin-")):
        check_stage()
        receipt.update(json.loads((ROOT / "build/staged-identity.json").read_text()))
    (target / "result.json").write_text(json.dumps(receipt, indent=2) + "\n")


def validate(directory: Path, sha: str, needs: dict[str, dict[str, str]]) -> list[dict[str, str | int]]:
    """Reject unsuccessful jobs, missing cells, stale provenance and bad XML."""
    require(set(needs) == {"build", "abi", "runtime", "kotlin"}, "Missing job result")
    for name, job in needs.items():
        require(job["result"] == "success", f"{name}: job result is {job['result']}")
    identities = list(directory.glob("staged-*/staged-identity.json"))
    require(len(identities) == 1, "Missing or duplicate staged identity")
    identity = json.loads(identities[0].read_text())
    require(identity["sourceSha"] == sha, "Staged source SHA mismatch")
    results = []
    for lane in expected_lanes():
        artifact = directory / f"results-{lane}-{sha}"
        require((artifact / "result.json").is_file(), f"{lane}: missing result artifact {artifact.name}")
        receipt = json.loads((artifact / "result.json").read_text())
        require(receipt["sourceSha"] == sha and receipt["lane"] == lane, f"{lane}: stale result")
        if lane == "abi":
            require((artifact / "abi.txt").read_text().strip() == sha, "Missing ABI success receipt")
            results.append({"lane": lane, "tests": 0, "artifact": artifact.name})
            continue
        xmls = list(artifact.rglob("TEST-*.xml"))
        require(bool(xmls), f"{lane}: missing XML results")
        count = 0
        for xml in xmls:
            root = ET.parse(xml).getroot()
            cases = root.findall(".//testcase")
            require(bool(cases), f"{lane}: zero executed tests in {xml.name}")
            for case in cases:
                require(not any(case.find(tag) is not None for tag in
                                ["failure", "error", "skipped", "flakyFailure", "rerunFailure"]),
                        f"{lane}: failed, flaky or skipped test")
            for suite in root.iter("testsuite"):
                require(all(int(suite.get(key, "0")) == 0 for key in
                            ["failures", "errors", "skipped"]), f"{lane}: unsuccessful XML suite")
                require(int(suite.get("tests", "0")) == len(suite.findall("testcase")),
                        f"{lane}: inconsistent XML test count")
            count += len(cases)
        if lane.startswith(("runtime-", "kotlin-")):
            require(receipt["libraryJarSha256"] == identity["libraryJarSha256"],
                    f"{lane}: wrong staged JAR")
        if lane.startswith("runtime-"):
            reports = list(artifact.rglob("runtime.json"))
            require(len(reports) == 1, f"{lane}: missing runtime provenance")
            runtime = json.loads(reports[0].read_text())
            version = lane.removeprefix("runtime-")
            require(all(runtime[key] == version for key in
                        ["selectedGradleVersion", "daemonGradleVersion", "fixtureGradleVersion"]),
                    f"{lane}: wrong client version")
            require(all(Path(p).name == f"gradle-test-kit-{version}.jar"
                        for p in runtime["testKitClientCodeSources"].values()),
                    f"{lane}: wrong client code source")
            require(all(runtime[key].split(".")[0] == "21" for key in
                        ["daemonJavaVersion", "testJavaVersion", "fixtureJavaVersion"]),
                    f"{lane}: wrong JVM")
            require(runtime["libraryJarSha256"] == identity["libraryJarSha256"],
                    f"{lane}: runtime loaded another JAR")
        if lane.startswith("kotlin-"):
            properties = {p.attrib["name"]: p.attrib["value"]
                          for p in ET.parse(xmls[0]).findall(".//property")}
            require(properties["compilerVersion"] == lane.removeprefix("kotlin-"),
                    f"{lane}: wrong compiler")
            require(properties["libraryJarSha256"] == identity["libraryJarSha256"],
                    f"{lane}: compiler consumed another JAR")
            require(properties["gradleVersion"] in matrix()["gradle"],
                    f"{lane}: undeclared Gradle distribution")
            require(properties["javaVersion"].split(".")[0] == "21", f"{lane}: wrong JVM")
            require(int(properties["behavioralChecks"]) > 0, f"{lane}: no behavioral probes")
        results.append({"lane": lane, "tests": count, "artifact": artifact.name})
    return results


def main() -> None:
    match sys.argv[1:]:
        case ["matrix"]:
            versions = matrix()
            print("gradle=" + json.dumps(versions["gradle"]))
            print("kotlin=" + json.dumps(versions["kotlin"]))
            print("consumer-gradle=" + min(versions["gradle"], key=lambda v: tuple(map(int, v.split(".")))))
        case ["stage"]:
            stage()
        case ["check-stage"]:
            check_stage()
        case ["record", lane, source]:
            record(lane, Path(source))
        case ["aggregate", directory]:
            results = validate(Path(directory), os.environ["GITHUB_SHA"], json.loads(os.environ["NEEDS_JSON"]))
            Path("build/ci-summary.json").write_text(json.dumps(
                {"sourceSha": os.environ["GITHUB_SHA"], "lanes": results}, indent=2) + "\n")
            print(json.dumps(results, indent=2))
        case _:
            raise InvalidEvidence("Expected matrix, stage, check-stage, record or aggregate")


if __name__ == "__main__":
    main()
