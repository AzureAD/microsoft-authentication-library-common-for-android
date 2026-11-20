<!-- Copyright (c) Microsoft Corporation. All rights reserved. Licensed under the MIT License. -->

# Flight source inventory

Build-only Kotlin/JDK 17 tooling shared by flight-governance consumers. It reads
committed Java source using the JDK syntax parser, not a single-line/uppercase regex.
It does not evaluate arbitrary Java, infer owners, change defaults, query ECS,
or enforce PR policy.

## Build and test

Use the repository's JDK/feed setup in [AGENTS.md](../../AGENTS.md).
From `common-java-root`:

```powershell
.\gradlew.bat :flight-governance:test :flight-governance:installDist --no-daemon
```

The tool is not a dependency of any runtime library and has no publishing configuration.
Common4j PR validation runs the tool's tests and packages its distribution.

## Create a candidate legacy snapshot

From the repository root, with an existing output directory outside all input repositories:

```powershell
.\tooling\flight-governance\build\install\flight-governance\bin\flight-governance.bat `
  --source common C:\src\common HEAD common4j\src\main\com\microsoft\identity\common\java\flighting\CommonFlight.java `
  --output C:\temp\flight-inventory.json
```

Repeat `--source <label> <repository-directory> <revision> <relative-java-file>`
for additional sources. A repository label identifies one commit. Revisions resolve
to immutable commit IDs; uncommitted input-file changes are not scanned.
Only enums implementing `IFlightConfig` with string-literal keys and two constructor
arguments are supported. Java syntax is checked; this is not a replacement for compilation.

Explicit test declarations can be excluded with
`--exclude "label:qualified.Enum#CONSTANT"`. Supply the exact, reviewed test IDs
from the calling repository; there are no prefix-based automatic exclusions.
Unknown/duplicate exclusions fail, and excluded records remain in the snapshot.
Keep private declarations/configuration out of public fixtures and examples.

## Output and approval

The JSON contains sources, included/excluded declarations, and counts for declarations,
unique ECS keys, exclusions, and unresolved defaults. Defaults retain their source
expression, normalized expression, type, value when known, and unresolved reason.
Pretty-printed expressions use LF line endings; original source expressions stay unchanged.
Annotation types/arguments are recorded as written; they are not evaluated ownership
or policy assertions. Shared keys remain separate declarations.

Literal values and literal `org.json.JSONObject` constructors are resolved without
executing input code. Constructors with anonymous class bodies, including double-brace
initializers, remain `UNRESOLVED` even when their arguments are literals.
Other expressions remain `UNRESOLVED`, never guessed false/zero.
Malformed Java, unsupported declarations, and Git failures produce nonzero exits.

Output is canonical UTF-8 JSON with no volatile timestamp. Creation refuses an existing
destination or a destination inside an input repository. This prevents accidental
overwrite; it is not a digital signature or filesystem immutability guarantee.
Review the candidate and store the approved snapshot in controlled, versioned storage
before treating it as a trusted legacy baseline. This tool cannot approve it.

## Report-only PR checks

After building the distribution, invoke the checker with the real PR source revision
and target branch, not the generated PR merge commit:

```powershell
.\tooling\flight-governance\build\install\flight-governance\bin\flight-governance.bat check-pr `
  --repository common C:\src\common --base origin/dev --head <source-commit> `
  --source common4j/src/main/com/microsoft/identity/common/java/flighting/CommonFlight.java `
  --policy config/flight-governance.json --output C:\temp\flight-pr-report.json
```

`check-pr` paths are Git paths using `/` separators. Repeat `--source` for additional
registries. Full history and a single target merge base are required. The checkout
is read-only; reports must be new files outside it. JSON and a `.json.md` summary
identify the source, target, merge base, evaluated declarations, findings and approved
legacy gaps. Policy findings return zero during this pilot; failed or incomplete
execution returns nonzero and, when the destination is valid, an incomplete JSON report.
Complete JSON is published only after its Markdown summary is written; a failed scan
may have no Markdown summary. Existing destination entries, including dangling
symlinks, are never overwritten.

New/changed keys, defaults or metadata need an explicit current person/team owner
and lifecycle type. Git authorship never substitutes for ownership; reviewers confirm
that the recorded owner is current. Every type (Release, Kill Switch and Config)
requires an existing same-repository Markdown governance document regardless of default.
The path uses `FlightMeta.premortem`, whose name is retained for compatibility.
Changing type does not evade the document requirement. Untouched
legacy declarations and formatting/comments alone are grandfathered. Removing or
renaming a linked document is reported even without an enum edit.

The policy engine consumes normalized records; the Java adapter handles syntax.
It has no hardcoded registry paths or Broker precedence. Future Kotlin extraction
can feed the same policy model, but is not implemented here.

### Trusted initial legacy migration

The checker reads `--policy` only from the resolved **target commit**. Missing policy
means no exceptions, not an empty successful policy evaluation. The initial committed
policy freezes the pre-policy baseline and contains no approvals. The baseline must
belong to the target's history; the PR source may have branched before it. Migration
still requires the declaration to exist unchanged at both the baseline and PR merge base.

A separate owner-reviewed target-branch change can add an exact approval:

```json
{
  "schemaVersion": 1,
  "legacyRevision": "<full-pre-policy-commit-id>",
  "migrationApprovals": [
    {
      "id": "common:example.Flights#FEATURE",
      "owner": "owning-team",
      "type": "RELEASE",
      "premortem": "",
      "reviewedBy": "reviewing-team",
      "review": "<approval-reference>"
    }
  ]
}
```

Only an initial metadata addition to an unchanged baseline declaration qualifies:
the exact approved owner/type/document values must match, keys/defaults must stay
unchanged, and the PR cannot contain runtime changes. The adapter compares Java
syntax after removing governance metadata/imports; other changed files must be
Markdown under `docs/`. Prefer explicit metadata imports for migration PRs.
PR-authored approvals/baseline edits cannot grant an exception. Unknown owners,
reclassification and changed/new behavior do not qualify; historical gaps stay visible.

The shared ADO step fetches the PR source/target and publishes advisory findings;
incomplete scans fail explicitly. This is checkpoint A only: it changes neither
branch policies nor the eventual blocking policy mode. Reviewers must still identify
material behavior changes elsewhere that declaration comparisons cannot infer.

This repository-governance check is separate from the planned monthly, user-invoked
audit skill for old flights and configuration discrepancies. That later skill is not
a prerequisite for metadata backfill or PR enforcement and is not a release gate.
