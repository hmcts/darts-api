# Playwright branch selection (DMA-546)

The existing API PR pipeline runs Playwright after successful functional tests.
Deployment still requires `enable_keep_helm` and `enable_darts_portal`. Tests target
the portal and API belonging to that API PR; a test-selection label never changes
the deployment target.

| Labels | Playwright selection |
| --- | --- |
| No recognised Playwright label | Skip Playwright, as before |
| `pw_e2e` | Existing spike branch `dma-264-spike-pr-e2e-testing`, unchanged |
| `pw_e2e_branch` | Extract the DMA ticket from the API PR source branch and select exactly one matching Playwright branch |

For example, an API source branch `dma-123-xyz` selects Playwright branch
`dma-123-abc` when `pw_e2e_branch` is present. Jenkins exposes the source branch
through `CHANGE_BRANCH`; `BRANCH_NAME` is the PR job name, such as `PR-3503`, and
must not be used to extract the ticket. No extra manual-build parameter is needed.

`pw_e2e_branch` takes precedence when both labels are present. Ticket prefixes
are case insensitive and support variable-length numbers; `dma-9`, `DMA-123-abc`
and `dma-12345-abc` are valid. Remote branches must be named `dma-<ticket>` or
`dma-<ticket>-<description>`. Ticket 264 does not match 2640. A missing/invalid
source branch or zero/multiple matching remote branches fails the opt-in stage
clearly, with no fallback.

For the current trial, API PR **3503** has source branch
`dma-264-spike-pr-e2e-testing`. Applying `pw_e2e_branch` selects the unique DMA-264
Playwright branch and still targets **API/portal PR 3503**.
The remote branch is resolved to a commit before checkout. Both existing shards
run that same commit, even if the test branch advances during the build. The
selected branch, commit and deployment URLs are logged in **Resolve Playwright
revision**. Existing secret injection, sharding, merged reports, failure handling,
fork exclusions, PR-only hook and other pipeline stages are unchanged.

## Validation

Run the standalone checks with an available Groovy runtime:

```sh
groovy bin/ci/tests/playwright-e2e-test.groovy
```

These checks stub Jenkins/GitHub and exercise label selection, ticket boundaries,
missing/ambiguous branches, deployment guards and both shard checkouts. They do
not deploy or access secrets. A live labelled Jenkins run is still required to
verify the new path with the shared library and Jenkins CPS runtime.

For the live trial, push this API change, ensure the intended Playwright branch is
pushed, add `pw_e2e_branch` to PR 3503, and rebuild its Jenkins job. Check the
selected commit, both shard results and the merged Playwright E2E report. Merely editing a
label is not assumed to trigger a build. Remove `pw_e2e_branch` and retain `pw_e2e` to restore the legacy selection,
or remove both labels to skip Playwright.
