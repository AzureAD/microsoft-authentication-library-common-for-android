# Latest Release Branch

The latest release branch is: **`working/release/23.1.0`**

## Release Branch Information

- **Branch Name**: `working/release/23.1.0`
- **Corresponding Tag**: `v23.1.0`
- **Branch Pattern**: `working/release/<VERSION>`

## How to Access

To checkout the latest release branch:
```bash
git fetch origin working/release/23.1.0
git checkout working/release/23.1.0
```

## Release History (Recent)

The repository maintains release branches following the pattern `working/release/<VERSION>`:

- `working/release/23.1.0` (latest)
- `working/release/23.0.0`
- `working/release/22.1.0`
- `working/release/22.0.1`
- `working/release/22.0.0`
- `working/release/21.4.0`
- `working/release/21.3.0`
- `working/release/21.2.0`
- `working/release/21.1.0`
- `working/release/21.0.0`

## Verification

To verify the latest release branch yourself:
```bash
# List all release branches sorted by version
git ls-remote --heads origin | grep "working/release/" | awk -F'/' '{print $NF}' | sort -V | tail -1
```

This will output: `23.1.0`

## Tags

The release is also tagged as `v23.1.0`. To see all tags:
```bash
git tag --sort=-version:refname | head -20
```
