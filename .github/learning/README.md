<!--
Copyright (c) Microsoft Corporation. All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
-->

# Human-feedback review learning pilot

This is isolated developer tooling for Common. It does not modify product
modules, replace GitHub Copilot review, migrate review to Foundry, or train a
model. Its only learning output is a draft PR proposing focused changes to
existing review guidance, followed by human approval and merge.

The implementation is Kotlin/JVM, with a standalone build that reuses the
repository's Gradle wrapper but does not load Android projects or private feeds.
The runtime does not depend on the product Common library.

## Default-off operation

Learning runs only on manual dispatch or an explicitly approved PR preview.
There is no scheduled trigger in the initial pilot.
Without `REVIEW_LEARNING_ENABLED=true`, no extraction, CLI installation,
synthesis, checkpoint write, or guidance publication job runs. The offline
verification job can still run.

The dispatch defaults to `dry-run`, which produces a short-lived, validated
proposal artifact but writes no GitHub branches or PRs. Publication additionally
requires dispatch mode `propose` and
`REVIEW_LEARNING_PUBLISH_APPROVED=true`, plus the explicit owner confirmation
`REVIEW_LEARNING_PROTECTION_VERIFIED=true`. These variables are not provisioned
by this change. Enabling learning does not disable or alter native Copilot reviews.

## Live preview before merging the tooling PR

Use the ordinary `pull_request` label event to test real extraction, Copilot
synthesis, and proposal validation from an unmerged tooling PR. This is a
read-only generation test, not a test of publication or a training run.
It never uses `pull_request_target`.
Use only maintainer-trusted PR code: checks inside a PR's workflow are not a
sandbox for arbitrary workflow edits by repository writers.

1. Establish organization-billed CLI permission and configure the generation
   variables listed below: `REVIEW_LEARNING_POLICY`,
   `REVIEW_LEARNING_COPILOT_VERSION`, `REVIEW_LEARNING_MODEL`,
   `REVIEW_LEARNING_MAX_AI_CREDITS`, and `REVIEW_LEARNING_ENABLED=true`.
   Owners must approve the actual identities, model, and credit cap; the
   workflow does not select them automatically. No custom App or secret is needed.
2. Review the same-repository tooling PR's exact head commit. In repository
   **Settings > Secrets and variables > Actions > Variables**, set
   `REVIEW_LEARNING_PREVIEW_SHA` to that full 40-character commit SHA.
   The workflow checks out that exact revision, not the PR's merge ref.
3. An approved human whose immutable ID is in the policy's `ownerIds` applies
   the label `learning-live-preview` to that PR targeting `dev`. A maintainer
   may need to create the label first. Applying it is the explicit request for
   the potentially billed run; a bot or an unregistered reviewer cannot
   authorize it.
4. Open **Actions > Review guidance learning**, select the run for that
   `labeled` event, and expand `prepare`. `inspect` reports the number of
   qualifying evidence records and whether there is signal. Only usable new
   signal invokes a real model; offline compatibility tests can separately
   install/run the CLI against local fixtures. Download **review-learning-proposal** from
   that run's artifacts and inspect `proposal.json`: each rule has a statement,
   supporting evidence IDs, and exact `path`/`before`/`after` edits. The artifact
   expires after one day.

An unmerged PR supplies the tooling being tested, not accepted evidence:
the learner still reads qualifying human feedback from merged source PRs
on `dev`. Fewer than two independent witnesses (without an owner-approved
severe exception), unchanged evidence, or an active guidance draft can
legitimately skip the model. In that case the empty proposal/status proves
the extraction/gating path, **not** a successful live model call. Do not
weaken the evidence policy or invent source comments to force a result.

Pushes, unrelated labels, forks, different actors, and nonapproved SHAs do not
start live synthesis. To test a newer commit, review it, update
`REVIEW_LEARNING_PREVIEW_SHA`, then remove and reapply the preview label.
Re-running an old event does not approve a new commit. Clear the preview SHA
after testing. Normal Actions re-runs of the approved event remain potentially
billable; this is an explicit-event gate, not a one-use credit reservation.

The PR preparation job has only read permissions plus `copilot-requests: write`.
No write token is supplied to synthesis. The CLI repeats preview admission
checks before API/model initialization and rejects `publish` on PR events,
even if publication is otherwise enabled. The writer remains restricted to
manual `propose` dispatch on `dev` with separate publication approval.
Test a full write-path E2E before merge only in an explicitly approved,
isolated organization-owned sandbox with a repository-scoped Actions token.

## Evidence policy

- Only registered, verified `User` identities that are not automation identities
  can provide primary review evidence. Identities use host-local immutable
  numeric IDs, not display names or login-pattern matching.
- A PR author's reply is context only. A different registered owner/reviewer can
  explicitly verify that correction against code; their adjudication becomes
  the primary evidence. Authors cannot verify themselves.
- Every candidate needs an immutable code citation in the source PR's changed
  paths. Inline RIGHT-side comments use their original commit/line. General
  comments and review bodies must include a same-repository commit permalink
  such as `https://github.com/OWNER/REPO/blob/FULL_COMMIT/path.kt#L10-L15`.
- LEFT-side/deleted-line comments require an explicit verifiable code citation
  rather than incorrectly applying the old line number to the new file.
- Only merged PRs targeting `dev` enter the pilot. Guidance/skill changes,
  learning PRs, generated-only changes, versioning-only changes, and inaccessible
  source files are excluded. Closed-but-unmerged PRs are not treated as accepted
  fixes.
- Large REST pages shrink from 100 records to 50, 25, 5, then 1 without raising
  the two-million-character response limit. Divisor page sizes preserve the
  consumed offset when a later page shrinks. `maxPages` bounds completed pages;
  at most four additional size-reduction attempts are allowed per collection.
  An oversized single record still fails closed, and authentication/other
  failures are not retried as size errors.
- Code-witness files retain their 128,000-byte limit. Oversized evidence files
  are excluded with the explicit `source_exceeds_limit` counter; they cannot
  support a proposal. Oversized required guidance, checkpoint, or provenance
  files still fail the run. Access failures are not converted into exclusions.
- REST comments/replies and review bodies are paginated. GraphQL supplies
  thread-resolution context. Resolution is never evidence that a finding was
  correct. Authentication failures, invalid contracts, and pagination exhaustion
  fail the run; they are not reported as "no signal."
- Two distinct PRs and distinct commit/code witnesses are required for a rule.
  Identical copied/backported witnesses do not count twice. An independent
  registered owner may explicitly approve a severe single case.
- The model checks durability and semantic duplication; deterministic validation
  additionally rejects exact existing statements, unsupported citations,
  path escapes, overlapping edits, and oversized/rewrite-style proposals.
  Human review remains essential for semantic correctness and applicability.

### Explicit author-correction adjudication

Post this record in a human review comment or PR discussion, not as a bot
summary. Replace every example value with an actually verified source. The
source comment must be in the same PR and authored by its human author.

````text
```review-learning-adjudication
{
  "version": 1,
  "sourceCommentId": 123456,
  "decision": "verified",
  "rule": "Verify the actual exception hierarchy before claiming a subtype is uncaught.",
  "path": "common4j/src/main/example/RelevantFile.kt",
  "commit": "0123456789012345678901234567890123456789",
  "firstLine": 10,
  "lastLine": 20,
  "severeSingleCase": false
}
```
````

The commit must be anchored to the PR and the source range must exist.
`severeSingleCase=true` requires an independent approved owner ID; ordinary
reviewers cannot grant that exception. A generic "LGTM," thread resolution, or
an automated paraphrase is insufficient.

## Owner-approved configuration

Use repository variables, not secrets or tracked credentials, for these values:

| Variable | Purpose |
|---|---|
| `REVIEW_LEARNING_ENABLED` | Explicit extraction/synthesis enablement; unset is disabled. |
| `REVIEW_LEARNING_POLICY` | Versioned JSON below, containing approved immutable identity IDs and topic paths. |
| `REVIEW_LEARNING_COPILOT_VERSION` | An exact approved `@github/copilot` version, never `latest`. |
| `REVIEW_LEARNING_MODEL` | An explicitly approved model identifier; `auto` is rejected. |
| `REVIEW_LEARNING_MAX_AI_CREDITS` | An owner-approved CLI credit ceiling of at least 30; there is no inferred budget. |
| `REVIEW_LEARNING_PREVIEW_SHA` | Optional, full approved PR head SHA for a pre-merge preview; never a branch name. |
| `REVIEW_LEARNING_PUBLISH_APPROVED` | Separately enables draft/checkpoint publication. |
| `REVIEW_LEARNING_PROTECTION_VERIFIED` | Explicit owner confirmation of the review/non-bypass prerequisites below; unset is unverified. |

No separately provisioned GitHub App, private key, or PAT is required.
GitHub creates a fresh repository-scoped `GITHUB_TOKEN` for each job.
Only the separate `publish` job requests Contents write and Pull requests write;
the synthesis job retains a read-only repository token. Neither job can approve
or merge guidance through this tooling.

Policy template:

```json
{
  "version": 1,
  "host": "github.com",
  "repository": "AzureAD/microsoft-authentication-library-common-for-android",
  "base": "dev",
  "humanIds": [],
  "ownerIds": [],
  "automationIds": [41898282],
  "guidancePaths": [
    ".github/skills/code-review/references/correctness-api-compatibility.md",
    ".github/skills/code-review/references/false-positive-suppression.md"
  ],
  "lookbackDays": 30,
  "maxPages": 10
}
```

Empty human/owner registries intentionally fail validation. Obtain IDs through
approved host API discovery and have repository owners approve the registry.
Record known Copilot/custom reviewer/service identities, including User-typed
service accounts, in `automationIds`, so they cannot become primary evidence.
Register the publisher's verified Bot ID so interrupted publications can be recovered.
For this GitHub.com adapter the publisher is
[`github-actions[bot]`](https://api.github.com/users/github-actions%5Bbot%5D),
whose immutable ID is `41898282`. This verified ID must be in `automationIds`
before publication; a display-name match is insufficient. Do not copy a
GitHub.com numeric identity into a GHE registry. The GHE publisher is not enabled.

Paths must refer to existing `.github/copilot-instructions.md` or existing
focused `.github/skills/code-review/references/*.md` topics. The learner cannot
create skill routes, rewrite product code, or add catch-all insight diaries.
Start with the smallest owner-approved topic allowlist.

### Owner protection confirmation and first publishing test

Before setting `REVIEW_LEARNING_PROTECTION_VERIFIED=true`, a repository owner
must verify the applicable `dev` branch rules require a PR with at least one
approval and either stale-approval dismissal or approval after the last push.
The GitHub Actions identity must not bypass those requirements. Keep required
checks and human merge review in place, and protect the checkpoint/proposal
branch namespaces against unrelated writers.

This is an explicit setup prerequisite, not a claim that the workflow can
inspect every bypass actor. Owners must clear the confirmation if these
conditions change and reconfirm them after correcting the configuration.
No rules or permissions are changed automatically.

To test publishing in Common:

1. Merge the reviewed tooling into `dev` through the normal PR process.
   Keep `REVIEW_LEARNING_PUBLISH_APPROVED=false` until setup is complete.
2. Verify the protection prerequisites above and add `41898282` to the existing
   policy's `automationIds`, preserving other approved identities.
3. Under **Settings > Actions > General > Workflow permissions**, enable
   **Allow GitHub Actions to create and approve pull requests**, if organization
   policy permits. This tooling only creates draft PRs; it never submits
   approvals. Do not enable a globally writable token default: the writer
   permissions are scoped to the `publish` job in YAML.
4. Set `REVIEW_LEARNING_PROTECTION_VERIFIED=true` and then
   `REVIEW_LEARNING_PUBLISH_APPROVED=true`. Keep the approved model/credit cap.
5. In **Actions > Review guidance learning > Run workflow**, select branch
   `dev` and mode `propose`. This generates and revalidates a fresh proposal;
   it does not blindly publish an earlier preview artifact.
6. With a valid nonempty proposal, expect one draft guidance PR and the
   metadata-only checkpoint branch. Review its exact files, citations, and
   independent evidence. Leave the PR unmerged if only testing publication.
   Set publication approval back to `false` after the test.

For CI on each generated PR, a user with write access must select
**Approve workflows to run** in the PR banner. This is GitHub's approval gate
for PR events created by `GITHUB_TOKEN`, not an approval gate on the learning
workflow itself. Human review/approval of the proposed guidance is separate.
Native Copilot review and other required provider checks must also be arranged
under repository policy; CI approval alone does not prove they ran.

## Supported synthesis and isolation

The Actions generation job authenticates with its short-lived `GITHUB_TOKEN`,
with `contents: read` and `copilot-requests: write`, not a personal PAT.
Organization policy **Allow use of Copilot CLI billed to the organization**
and model/budget approval must be established before enabling it. AI credits
are organization-metered; the CLI ceiling is not a promise of a hard
prepaid-dollar cap.

The configured CLI must support the required isolation and credit flags; a
help-only preflight rejects incompatible versions before inference. The tool
provides the prompt on standard input, explicitly exposes zero tools, denies
tools/URLs, disables repository custom-instruction and builtin-MCP loading,
does not grant folder trust, disables account history sync, and runs with an
isolated temporary home and a minimal environment. There is no publisher token
in that process. The temporary home is removed after completion or failure.
Known built-in tools are explicitly excluded, with defense-in-depth denials
using the supported `read,write,shell,url,memory` permission kinds.
`--deny-tool=*` is not valid CLI syntax and fails before inference. In CLI
1.0.108, an empty `--available-tools` flag defaults to the full tool roster;
it is not a zero-tool control.
Token environment variables are supplied only to the API/synthesis/publisher
steps, never to Gradle builds or the npm installer.

The model receives bounded rule/code evidence and opaque citation IDs, not the
identity registry, usernames, raw author replies, or actor IDs. No raw evidence
packet, prompt, response, or session history is uploaded as an artifact.
Only the validated publication bundle is retained for one day. Runtime stdout
contains bounded status/counter JSON; failure diagnostics with raw source or
secrets are withheld.
Subprocess failures report the stage, exit code, and a static diagnostic category
(arguments, credits, authentication/policy, network, model access, or
unclassified), never arbitrary CLI stdout/stderr. These hints do not prove
whether a failed request consumed credits.

Known secret/instruction-injection patterns and private-host links are rejected
before synthesis or publication. This deny-pattern screen is not a complete
PII or semantic-security classifier: humans must review every guidance change.
Model output has no write authority; exact-context edits and citations are
validated again in the independent publisher.

GitHub Models inference is not used. GHE metadata extraction is host-bound, but
GHE synthesis is deliberately refused until a separately verified runtime
adapter establishes the correct token and data boundary. A GHE token must
never fall through to GitHub.com's Copilot authentication.
The Broker metadata API origin is `https://api.msft.ghe.com`, not a GHES
`/api/v3` endpoint; this was verified from the live repository API metadata.

## Checkpoints, publication, and recovery

`copilot/review-learning-state` holds a bounded metadata-only checkpoint:
pending PR IDs, evidence hashes and accepted/declined guidance outcomes,
the active proposal, and an unchanged-evidence fingerprint. Evidence
dispositions expire after 180 days. Raw review comments/code are not stored
there.
Merged guidance is recorded as accepted only after verifying a current-head
approval from an independent registered human, not merely because it merged.

Sub-threshold evidence stays pending. An empty synthesis result records only a
snapshot fingerprint, not a claim that the human feedback was correct,
incorrect, or already covered. Identical evidence plus identical base guidance
does not call the model again; changed feedback or guidance permits reevaluation.
Accepted and declined guidance evidence are distinguished from finding outcomes.

Only one active learning draft is allowed. New evidence is queued while it is
under human review; the tool never updates that draft or invalidates approvals.
Each proposal cycle gets a deterministic new branch under
`copilot/review-learning/`. All ref updates are checked against the observed
head and are fast-forward, never forced. `dev` writes are rejected in code.

Each guidance PR also contains a generated provenance manifest, mapping its
bounded proposal to source PRs. This is publication bookkeeping, excluded from
primary learning evidence. Recovery rechecks actual human evidence, exact
guidance edits, changed-file sets, parent commit, and the approved publisher's
identity before adopting an interrupted PR. A failed draft creation can reuse
an unchanged orphan branch; human edits or a conflicting checkpoint cause a
failure rather than an overwrite. No branches or historic reviews are deleted.

The CLI rejects publication before client initialization unless owner
confirmation and publication approval are both explicitly true, the Actions
context is a manual GitHub.com dispatch on `dev`, and the verified Actions bot
is registered. It then checks ordinary branch metadata to confirm `dev` remains
protected. Errors, a missing branch, or unprotected metadata fail before writes.

The normal Actions token cannot request Administration read. Accordingly,
the detailed review-rule/non-bypass check is owner-verified setup, not an
automatic per-run audit. The protected-branch metadata check is a narrower
runtime check and does not replace that owner prerequisite.

GitHub-token-created PRs' `opened`, `synchronize`, and `reopened` events create
approval-required Actions runs. A writer must approve those CI workflows;
other token-generated events generally do not start workflows.
See [GitHub's token event rules](https://docs.github.com/en/actions/concepts/security/github_token#when-github_token-triggers-workflow-runs).
Common's existing AB-ID workflow rewrites referenced work items to closing
references; generated guidance PRs therefore do not reference the implementation
PBI with `AB#`. Owners must use the approved guidance-only AB-check policy
(for example, the existing `skip AB ID validation` label if permitted), rather
than accidentally closing the implementation PBI on every learned-rule PR.

## Local verification

From the Common repository root, in PowerShell:

```powershell
.\gradlew.bat -p .github\learning --no-daemon --console=plain test jacocoTestCoverageVerification installDist
git diff --check
```

CI runs on JDK 17. The isolated tooling targets Java 8 bytecode and can also
be built with the repository wrapper on an existing Java 8 installation;
this does not change the recommended JDK for product-module validation.
No Android SDK, lab credentials, or private-feed credentials are needed.

When learning is enabled on a trusted same-repository PR, verification also
installs the owner-pinned CLI and runs an offline protocol contract. The same
contract runs in preparation after installation and before real evidence or
credentials are supplied. It uses a localhost canned-response provider, fake
credentials, isolated state, and an upstream-blocking proxy to assert that the
actual model request contains zero tools and that the task arrives on stdin.
No GitHub model inference or AI-credit usage is involved. A future CLI adding
tools fails this contract until the explicit exclusion list is reviewed.
To run it locally with an already installed exact CLI:

```powershell
.\gradlew.bat -p .github\learning test --tests "*CopilotProtocolTest" "-PcopilotExecutable=C:\path\to\copilot.exe" "-PcopilotModel=gpt-6.1-sol"
```

The tooling has an aggregate JaCoCo LINE coverage gate of 80%.
For workflow linting, actionlint 1.7.12 has a stale permission-scope table and
does not recognize the documented `copilot-requests` scope. Keep the scope:
[GitHub's supported CLI authentication](https://docs.github.com/en/copilot/how-tos/copilot-cli/use-copilot-cli-in-actions)
requires it. Until actionlint supports that field, narrowly ignore only
`^unknown permission scope "copilot-requests"\.`; all other workflow errors
remain failures. This exception is not proof of organization policy or
host-level availability, which must still be verified before enabling the job.

Ordinary tests use an in-memory repository and fake model. The opt-in real-CLI
protocol test invokes only a localhost canned-response provider, not a model.
Tests do not create GitHub branches/PRs, alter authentication, or enable
production jobs.
The executable commands are `inspect`, `prepare`, and `publish`, each accepting
a publication-bundle path; workflow environment variables supply policy,
repository, runtime, and token. Do not run credentialed publication as a local
test.

## Rollout boundary

This change implements the Common pilot, not a production enablement.
Verify owner configuration, organization billing, actual native review skill
attribution/effect, protected-branch denial, and a human-reviewed dry-run corpus
before allowing publication. Add a weekly trigger only after that approval.

Reuse the validated core for Broker and MSAL afterward. Broker needs a verified
GHE runner/model/private-data adapter; MSAL should map to its existing
`copilot-instructions.md` review topics instead of assuming it has Common's
skill tree. No AuthApp, classifier, specialist, or Foundry changes are required.
