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
