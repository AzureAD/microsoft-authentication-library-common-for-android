#!/usr/bin/env python3
# Copyright (c) Microsoft Corporation. All rights reserved.
# Licensed under the MIT License.
"""Compare JaCoCo coverage between a base (target/dev) report and a PR report.

Used as a blocking PR check: it fails (exit 1) when overall coverage drops by more
than the allowed tolerance, and prints a per-class breakdown of WHERE coverage
regressed so the PR author knows exactly which classes need tests.

Usage:
  compare_coverage.py --base <dev.xml>...  --pr <pr.xml>...
      [--metric LINE|BRANCH] [--tolerance PP] [--unit-tolerance PP]
      [--min-missed N] [--top N] [--out-md FILE] [--out-json FILE]
      [--no-fail-on-drop]

Stdlib only - runs on any pipeline image with Python 3.
"""

import argparse
import glob
import json
import sys
import xml.etree.ElementTree as ET


def _pct(covered, missed):
    total = covered + missed
    return round(100.0 * covered / total, 2) if total else 0.0


def _class_units(xml_path, metric):
    """Return {class_name: {'Covered': c, 'Missed': m}} for a JaCoCo report."""
    root = ET.parse(xml_path).getroot()
    units = {}
    for cls in root.iter("class"):
        name = (cls.get("name") or "").replace("/", ".")
        for counter in cls.findall("counter"):
            if counter.get("type", "").upper() == metric:
                entry = units.setdefault(name, {"Covered": 0, "Missed": 0})
                entry["Covered"] += int(counter.get("covered", 0))
                entry["Missed"] += int(counter.get("missed", 0))
                break
    return units


def _aggregate(paths, metric):
    agg = {}
    for path in sorted(set(paths)):
        try:
            for name, cm in _class_units(path, metric).items():
                entry = agg.setdefault(name, {"Covered": 0, "Missed": 0})
                entry["Covered"] += cm["Covered"]
                entry["Missed"] += cm["Missed"]
        except (ET.ParseError, OSError) as exc:
            sys.stderr.write(f"WARNING: skipping {path}: {exc}\n")
    return agg


# JaCoCo <counter type="..."> values. Guards against silent 0% when a caller passes
# a typo'd or wrong-cased metric (which would otherwise match no counters).
VALID_METRICS = {"INSTRUCTION", "LINE", "BRANCH", "COMPLEXITY", "METHOD", "CLASS"}


def _write_skip(args, metric, reason):
    """Write a 'skipped/not gating' Markdown + JSON so downstream publish steps that
    always run (succeededOrFailed) still find the expected output files."""
    if args.out_md:
        with open(args.out_md, "w", encoding="utf-8") as handle:
            handle.write(f"# Code Coverage Comparison ({metric}) - SKIPPED\n\n{reason}\n")
    if args.out_json:
        with open(args.out_json, "w", encoding="utf-8") as handle:
            json.dump({"metric": metric, "skipped": True, "reason": reason,
                       "failed": False}, handle, indent=2)


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base", nargs="+", required=True,
                        help="Baseline (target/dev) JaCoCo XML file(s) or glob(s).")
    parser.add_argument("--pr", nargs="+", required=True,
                        help="PR branch JaCoCo XML file(s) or glob(s).")
    parser.add_argument("--metric", default="LINE", help="LINE (default) or BRANCH.")
    parser.add_argument("--tolerance", type=float, default=0.0,
                        help="Allowed overall drop (percentage points) before failing.")
    parser.add_argument("--unit-tolerance", type=float, default=0.0, dest="unit_tolerance",
                        help="Per-class drop (pp) below which a class is listed as regressed.")
    parser.add_argument("--min-missed", type=int, default=1, dest="min_missed",
                        help="Ignore new classes with fewer missed units than this.")
    parser.add_argument("--top", type=int, default=25,
                        help="Show only the top N rows per table (0 = all).")
    parser.add_argument("--out-md", default="", dest="out_md")
    parser.add_argument("--out-json", default="", dest="out_json")
    parser.add_argument("--no-fail-on-drop", action="store_false", dest="fail_on_drop",
                        help="Report the diff but never exit non-zero (non-gating).")
    parser.set_defaults(fail_on_drop=True)
    args = parser.parse_args()

    metric = args.metric.upper()
    if metric not in VALID_METRICS:
        sys.stderr.write(f"ERROR: --metric must be one of {sorted(VALID_METRICS)}; "
                         f"got {args.metric!r}\n")
        return 2
    if args.tolerance < 0 or args.unit_tolerance < 0:
        sys.stderr.write("ERROR: --tolerance and --unit-tolerance must be "
                         "non-negative percentage points.\n")
        return 2
    base_paths, pr_paths = [], []
    for pattern in args.base:
        base_paths.extend(glob.glob(pattern, recursive=True))
    for pattern in args.pr:
        pr_paths.extend(glob.glob(pattern, recursive=True))

    if not pr_paths:
        sys.stderr.write(f"ERROR: no PR coverage files matched: {', '.join(args.pr)}\n")
        _write_skip(args, metric, "No PR coverage report was found, so no comparison "
                    "could be made (not gating).")
        return 2
    if not base_paths:
        # No baseline (e.g. new module / first run): don't block the PR.
        reason = ("No base (dev) coverage report was found, so there is nothing to "
                  "compare against; skipping the coverage gate (not gating).")
        sys.stderr.write("WARNING: no base coverage files matched: "
                         f"{', '.join(args.base)}; skipping comparison (not gating).\n")
        _write_skip(args, metric, reason)
        return 0

    base = _aggregate(base_paths, metric)
    pr = _aggregate(pr_paths, metric)

    base_cov = sum(v["Covered"] for v in base.values())
    base_miss = sum(v["Missed"] for v in base.values())
    pr_cov = sum(v["Covered"] for v in pr.values())
    pr_miss = sum(v["Missed"] for v in pr.values())
    base_pct = _pct(base_cov, base_miss)
    pr_pct = _pct(pr_cov, pr_miss)
    delta = round(pr_pct - base_pct, 2)

    regressed, new_gaps, removed = [], [], 0
    for unit in sorted(set(base) | set(pr)):
        b = base.get(unit)
        p = pr.get(unit)
        if b is None:
            # New class in the PR: report it as a gap if it lacks coverage.
            p_pct = _pct(p["Covered"], p["Missed"])
            if p["Missed"] >= args.min_missed:
                new_gaps.append({"Unit": unit, "Percentage": p_pct,
                                 "Missed": p["Missed"],
                                 "Total": p["Covered"] + p["Missed"]})
            continue
        if p is None:
            # Class removed in the PR: not a coverage regression, so don't list it.
            removed += 1
            continue
        b_pct = _pct(b["Covered"], b["Missed"])
        p_pct = _pct(p["Covered"], p["Missed"])
        d = round(p_pct - b_pct, 2)
        if d < -args.unit_tolerance:
            regressed.append({"Unit": unit, "BasePct": b_pct, "PrPct": p_pct,
                              "Delta": d, "Missed": p["Missed"]})

    regressed.sort(key=lambda r: (r["Delta"], -r["Missed"]))
    new_gaps.sort(key=lambda r: (-r["Missed"], r["Percentage"]))

    failed = args.fail_on_drop and (pr_pct < base_pct - args.tolerance - 1e-9)
    verdict = "FAIL" if failed else "PASS"
    sign = "+" if delta >= 0 else ""

    lines = [
        f"# Code Coverage Comparison ({metric}) - {verdict}",
        "",
        "| | Base | PR | Delta |",
        "| --- | --- | --- | --- |",
        f"| **{metric} coverage** | {base_pct}% | {pr_pct}% | {sign}{delta} pp |",
        "",
    ]
    if args.tolerance:
        lines += [f"_Allowed drop (tolerance): {args.tolerance} pp._", ""]
    if failed:
        lines += [f"**Coverage dropped by {abs(delta)} pp** (base {base_pct}% -> PR "
                  f"{pr_pct}%), exceeding the allowed {args.tolerance} pp. "
                  "Add tests for the classes below to restore coverage.", ""]

    if regressed:
        top_reg = regressed[:args.top] if args.top > 0 else regressed
        lines += [f"## Classes with reduced coverage ({len(regressed)})", "",
                  "| Class | Base | PR | Delta | Missed (PR) |",
                  "| --- | --- | --- | --- | --- |"]
        for r in top_reg:
            lines.append(f"| {r['Unit']} | {r['BasePct']}% | {r['PrPct']}% | "
                         f"{r['Delta']} pp | {r['Missed']} |")
        lines.append("")

    if new_gaps:
        top_new = new_gaps[:args.top] if args.top > 0 else new_gaps
        lines += [f"## New/changed classes lacking coverage ({len(new_gaps)})", "",
                  "| Class | Coverage | Missed | Covered/Total |",
                  "| --- | --- | --- | --- |"]
        for r in top_new:
            covered = r["Total"] - r["Missed"]
            lines.append(f"| {r['Unit']} | {r['Percentage']}% | {r['Missed']} | "
                         f"{covered}/{r['Total']} |")
        lines.append("")

    if not regressed and not new_gaps:
        lines += ["_No per-class coverage regressions detected._", ""]

    markdown = "\n".join(lines) + "\n"
    if args.out_md:
        with open(args.out_md, "w", encoding="utf-8") as handle:
            handle.write(markdown)
    if args.out_json:
        with open(args.out_json, "w", encoding="utf-8") as handle:
            json.dump({"metric": metric, "basePercentage": base_pct,
                       "prPercentage": pr_pct, "deltaPp": delta,
                       "tolerancePp": args.tolerance, "failed": failed,
                       "removedClasses": removed,
                       "regressed": regressed, "newGaps": new_gaps}, handle, indent=2)

    print(markdown)
    if failed:
        print(f"ERROR: PR {metric} coverage {pr_pct}% is below base {base_pct}% "
              f"(drop {abs(delta)} pp > tolerance {args.tolerance} pp). Failing.",
              file=sys.stderr)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
