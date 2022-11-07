#!/usr/bin/env python3

from __future__ import annotations

import bz2
from dataclasses import dataclass
from datetime import datetime
from enum import auto, Enum, unique
from io import TextIOWrapper
from os.path import basename
from sys import argv
from xml.etree import ElementTree
from xml.etree.ElementTree import Element
from zipfile import ZipFile


@unique
class Status(Enum):
    TRUE_POSITIVE = auto()
    TRUE_NEGATIVE = auto()
    FALSE_POSITIVE = auto()
    FALSE_NEGATIVE = auto()
    DONT_KNOW = auto()


@unique
class Verdict(Enum):
    TRUE = 'true'
    FALSE = 'false'
    OUT_OF_MEMORY = 'OUT OF MEMORY'
    TIMEOUT = 'TIMEOUT'
    UNKNOWN = 'unknown'


@dataclass
class Run:
    name: str

    expected_verdict: Verdict
    verdict: Verdict

    hostname: str
    timestamp: datetime
    walltime: float

    output: str

    @property
    def status(self: Run) -> Status:
        if self.verdict in {Verdict.OUT_OF_MEMORY, Verdict.TIMEOUT, Verdict.UNKNOWN}:
            return Status.DONT_KNOW
        elif self.verdict == Verdict.TRUE:
            if self.expected_verdict == Verdict.TRUE:
                return Status.TRUE_POSITIVE
            else:
                return Status.FALSE_POSITIVE
        elif self.verdict == Verdict.FALSE:
            if self.expected_verdict == Verdict.FALSE:
                return Status.TRUE_NEGATIVE
            else:
                return Status.FALSE_NEGATIVE
        raise ValueError(f"verdict {self.verdict} unhandled")


def main(source: str, target: str) -> None:
    result = ElementTree.parse(bz2.open(source)).getroot()
    name = result.get("name")
    name_prefix = name.rsplit(".", maxsplit=1)[0]

    logfiles = source.split(".results.")[0] + ".logfiles"

    runs = []
    with ZipFile(f"{logfiles}.zip") as log_zip:
        for child in result:
            if child.tag == "run":
                runs.append(read_run(child, f"{basename(logfiles)}/{name_prefix}", log_zip))

    testsuites = ElementTree.Element("testsuites")
    for run_id, run in enumerate(runs):
        write_run(testsuites, run, run_id, name)
    ElementTree.ElementTree(testsuites).write(target)


def read_run(run: Element, name_prefix: str, log_zip: ZipFile) -> Run:
    name = run.get("name")
    expected_verdict = run.get("expectedVerdict")
    hostname = starttime = verdict = walltime = None
    for column in run:
        if column.get("title") == "host":
            hostname = column.get("value")
        elif column.get("title") == "starttime":
            starttime = column.get("value")
        elif column.get("title") == "status":
            verdict = column.get("value")
        elif column.get("title") == "walltime":
            walltime = column.get("value")

    with TextIOWrapper(log_zip.open(f"{name_prefix}.{basename(name)}.log")) as log:
        output = log.read()

    if not hostname or not starttime or not verdict or not walltime:
        raise ValueError("column(s) missing")

    return Run(name, Verdict(expected_verdict), Verdict(verdict), hostname, datetime.fromisoformat(starttime),
               float(walltime[:-1]), output)


def write_run(testsuites: Element, run: Run, run_id: int, name: str) -> None:
    testsuite = ElementTree.SubElement(testsuites, "testsuite")
    ElementTree.SubElement(testsuite, "properties")
    ElementTree.SubElement(testsuite, "system-err")

    testsuite.set("id", str(run_id))
    testsuite.set("package", name)

    testsuite.set("errors", "0")
    testsuite.set("tests", "1")

    testsuite.set("name", run.name)
    testsuite.set("hostname", run.hostname)
    testsuite.set("timestamp", run.timestamp.isoformat())
    testsuite.set("time", str(run.walltime))

    sysout = ElementTree.SubElement(testsuite, "system-out")
    sysout.text = run.output[:1000]  # libXML does not accept large texts

    testcase = ElementTree.SubElement(testsuite, "testcase")
    testcase.set("classname", basename(run.name))
    testcase.set("name", run.name)
    testcase.set("time", str(run.walltime))

    if run.status == Status.FALSE_NEGATIVE or run.status == Status.FALSE_POSITIVE:
        testsuite.set("failures", "1")
        failure = ElementTree.SubElement(testcase, "failure")
        failure.set("type", str(run.status))
        failure.set("message", f"Incorrect verdict: {run.verdict.value} (expected: {run.expected_verdict.value})")
    elif run.status == Status.DONT_KNOW:
        testsuite.set("failures", "0")
        skipped = ElementTree.SubElement(testcase, "skipped")
        skipped.set("message", f"No verdict: {run.verdict.value} (expected: {run.expected_verdict.value})")
    else:
        testsuite.set("failures", "0")


if __name__ == '__main__':
    main(argv[1], argv[2])
