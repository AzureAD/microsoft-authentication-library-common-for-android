# Pull Request Merge Time Analysis

This repository contains scripts to calculate the average time to merge for pull requests in the Microsoft Authentication Library Common for Android repository.

## Scripts

### 1. `calculate_pr_merge_time.py`

This is the main script that queries the GitHub API directly to fetch pull request data and calculate merge time statistics.

**Features:**
- Fetches merged pull requests from the repository
- Calculates time difference between PR creation and merge
- Provides comprehensive statistics including:
  - Average merge time
  - Median merge time
  - Fastest and slowest merge times
  - Standard deviation
  - Sample of recent merged PRs

**Usage:**
```bash
python3 calculate_pr_merge_time.py
```

**Requirements:**
- Python 3.6+
- `requests` library (`pip install requests`)
- Optional: GitHub personal access token (set as `GITHUB_TOKEN` environment variable for higher API rate limits)

**Setting up GitHub Token:**
```bash
export GITHUB_TOKEN=your_personal_access_token_here
python3 calculate_pr_merge_time.py
```

### 2. `calculate_pr_merge_time_mcp.py`

This script is designed to work with pre-fetched pull request data (e.g., from GitHub MCP server tools or GitHub API responses saved to a file).

**Usage:**
```bash
python3 calculate_pr_merge_time_mcp.py <pr_data.json>
```

Where `pr_data.json` contains pull request data from the GitHub API.

## Example Output

```
================================================================================
Pull Request Merge Time Statistics for AzureAD/microsoft-authentication-library-common-for-android
================================================================================

Total PRs Analyzed: 100

Average Time to Merge: 2 days, 5 hours
                       (53.24 hours)

Median Time to Merge:  1 day, 18 hours
                       (42.15 hours)

Fastest Merge:         1 hour, 28 minutes
                       (1.47 hours)

Slowest Merge:         15 days, 3 hours
                       (363.12 hours)

Standard Deviation:    72.34 hours

================================================================================

Sample of Recent Merged Pull Requests:
--------------------------------------------------------------------------------
  PR #2782: 1 hour, 28 minutes
    Title: Manual merging working/release/23.0.0 to release/23.0.0

  PR #2780: 1 day, 15 hours
    Title: [DEV] [Cherry-pick September Hotfix] Don't set browser pkg name

  ... and 98 more PRs
================================================================================
```

## Configuration

Both scripts are pre-configured to analyze the `AzureAD/microsoft-authentication-library-common-for-android` repository. To analyze a different repository, modify the `owner` and `repo` variables in the `main()` function of the respective script.

## Notes

- The scripts only analyze **merged** pull requests (PRs that were closed without merging are excluded)
- By default, the scripts analyze up to 100 most recently updated pull requests
- API rate limits apply when using the GitHub API without authentication (60 requests per hour)
- With a GitHub personal access token, the rate limit increases to 5,000 requests per hour
