# Playwright branch selection (DMA-546)

The existing API PR pipeline runs Playwright after successful functional tests.
Deployment still requires `enable_keep_helm` and `enable_darts_portal`. Tests target
the portal and API belonging to that API PR; a test-selection label never changes
the deployment target.

| Labels | Playwright selection |
| --- | --- |
| No recognised Playwright label | Skip Playwright, as before |
| `pw_e2e` or bare `pw_e2e_branch` | Existing spike branch `dma-264-spike-pr-e2e-testing`, unchanged |
| `pw_e2e:264` | Exactly one remote Playwright branch named `dma-264` or beginning `dma-264-` |
| `pw_e2e_branch:264` | Same ticket selection, using the DMA-546 label spelling |

Numbered labels work on their own. If a bare label is also present, the numbered
label takes precedence. Supply only one numbered label. Matching is case
insensitive (`DMA-264-...` also works), and `264` does not match ticket `2640`.
No matching branch, multiple matching branches, or a malformed selector fails
the opt-in stage with a clear error. There is no fallback to another test branch.

For example, API PR **3503** with `pw_e2e:264` runs the unique DMA-264 Playwright
branch against **API/portal PR 3503**, regardless of the API branch's ticket name.
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
pushed, add `pw_e2e:264` to PR 3503, and rebuild its Jenkins job. Check the selected
commit, both shard results and the merged Playwright E2E report. Merely editing a
label is not assumed to trigger a build. Remove the numbered label to restore
bare-label selection, or remove all Playwright labels to skip it.
