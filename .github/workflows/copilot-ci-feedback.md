---
description: "Investigates CI failures on Copilot coding agent PRs and posts @copilot comments with specific errors so the agent can self-correct."
on:
  check_suite:
    types: [completed]
  workflow_dispatch:
    inputs:
      pr_number:
        description: "PR number to investigate (required for manual testing)"
        required: true
  skip-bots: [github-actions]

if: >
  (github.event_name == 'workflow_dispatch' && github.event.inputs.pr_number != '') ||
  (github.event.check_suite.conclusion == 'failure' &&
   startsWith(github.event.check_suite.head_branch, 'copilot/'))

engine: copilot

permissions:
  contents: read
  checks: read
  actions: read
  issues: read
  pull-requests: read

safe-outputs:
  add-comment:
    pull-requests: true
    discussions: false

mcp-servers:
  ado:
    command: "npx"
    args: ["-y", "@azure-devops/mcp@2.4.0", "IdentityDivision", "-d", "pipelines"]
    allowed:
      - "get_build_status"
      - "get_build_log"
      - "get_build_log_by_id"
      - "get_builds"

steps:
  - name: Verify Copilot agent PR
    id: verify
    env:
      EVENT_NAME: ${{ github.event_name }}
      INPUT_PR_NUMBER: ${{ github.event.inputs.pr_number }}
      CHECK_SUITE_BRANCH: ${{ github.event.check_suite.head_branch }}
      GH_TOKEN: ${{ github.token }}
    run: |
      if [ "$EVENT_NAME" = "workflow_dispatch" ]; then
        PR_NUMBER="$INPUT_PR_NUMBER"
      else
        PR_NUMBER=$(gh pr list --head "$CHECK_SUITE_BRANCH" --state open --json number --jq '.[0].number' 2>/dev/null || echo "")
      fi
      if [ -z "$PR_NUMBER" ]; then
        echo "No PR found. Exiting."
        exit 1
      fi
      AUTHOR=$(gh pr view "$PR_NUMBER" --json author --jq '.author.login' 2>/dev/null || echo "")
      if [ "$AUTHOR" != "app/copilot-swe-agent" ] && [ "$AUTHOR" != "copilot-swe-agent[bot]" ]; then
        echo "PR #$PR_NUMBER author is '$AUTHOR', not copilot-swe-agent. Exiting."
        exit 1
      fi
      echo "PR #$PR_NUMBER is by Copilot agent. Proceeding."
      echo "pr_number=$PR_NUMBER" >> $GITHUB_OUTPUT

tools:
  github:
    toolsets: [pull_requests]
---

# Copilot CI Feedback

When CI checks fail on a PR created by the Copilot coding agent (`copilot-swe-agent[bot]`),
investigate the failures and post a `@copilot` comment with specific, actionable error
messages so the coding agent can fix them.

## When to Act

**For `check_suite` trigger**: Only act when ALL of these are true:
1. The `check_suite` completed with `failure` conclusion
2. The PR branch starts with `copilot/`
3. The PR author is `copilot-swe-agent[bot]` — verify this in Step 1 after finding the PR
4. No feedback comment was already posted in the last 15 minutes (avoid spam)

If any condition is not met, exit without posting.

**For `workflow_dispatch` trigger**: Use the `pr_number` input to specify which PR to investigate.
If no `pr_number` is provided, exit without posting.

## Investigation Process

### Step 1: Find the PR and Verify Author

**For `check_suite` events**:
Use the GitHub tools to find the open PR associated with the failing check suite's
head branch. Then verify the PR author is `copilot-swe-agent[bot]`. If the author is
anyone else, exit immediately — this workflow only acts on Copilot agent PRs.

**For `workflow_dispatch` events**:
Use the `pr_number` input (${{ github.event.inputs.pr_number }}) to look up the PR directly.

### Step 2: Identify Failing Checks

List all check runs on the PR. For each failed check:
- Note the check name and conclusion
- Note the `details_url` — this contains the ADO build URL for pipeline checks

### Step 3: Get Error Details

**For GitHub Actions checks** (changelog, validate-pr-ab-id, etc.):
- Read the check run annotations for error messages

**For Azure DevOps Pipeline checks** (Common - Build & Test, Assemble consumers, etc.):
- Parse the `details_url` to extract the ADO `buildId`
  (e.g., from `https://identitydivision.visualstudio.com/.../buildId=1602697` extract `1602697`)
- Use the ADO MCP server to query the build timeline and find failing tasks
- Fetch the log content for failing tasks
- Extract the actual error lines (look for patterns like `e: file:///...`, `error:`, `FAILURE:`)

### Step 4: Categorize and Summarize

Group errors into categories:
- **Compilation errors**: Type mismatches, unresolved references, missing imports
- **Missing changelog**: The `changelog.txt` wasn't updated
- **Lint/SpotBugs**: Static analysis findings
- **Test failures**: Unit or integration test failures
- **Consumer build failures**: Downstream repos (MSAL, Broker) can't compile against changes

For each error, include:
- The exact file and line number
- The full error message
- A brief suggestion of what to fix (based on the error type)

### Step 5: Post Comment

Post a single comment on the PR. **You MUST specify the PR number explicitly** using the
`item_number` parameter when calling the comment tool — the `check_suite` trigger does not
automatically provide PR context.

Use the PR number resolved in Step 1. Format:

```
@copilot CI checks failed on this PR. Please fix the following issues and push an update.

## Compilation Errors
- `AuthTabManager.kt:69` — Type mismatch: expected `ActivityResultLauncher<Intent>`, got `ActivityResultLauncher<Uri>`. Check the AndroidX Browser 1.9.0 API — `AuthTabIntent.registerActivityResultLauncher()` returns `ActivityResultLauncher<Intent>`, not `<Uri>`.

## Missing Changelog
- Add an entry to `changelog.txt` describing your changes. Format: `- [MINOR] Description (#PR_NUMBER)`

## Other
- [any other errors with specific file:line and messages]
```

## Important Rules

- **Be specific**: Include exact file paths, line numbers, and error messages. The coding agent
  cannot click links or browse ADO — it needs the error text inline.
- **Don't fix the code yourself**: Your job is to report errors, not create PRs. The coding
  agent will fix them.
- **Don't post if recently posted**: Check the last 5 comments on the PR. If one contains
  `@copilot` and `CI checks failed` from within the last 15 minutes, skip posting.
- **Don't post on non-Copilot PRs**: Only act on PRs from `copilot-swe-agent[bot]`.
- **Include ADO build links**: For reference, include the ADO build URL so humans can
  also investigate if needed.
