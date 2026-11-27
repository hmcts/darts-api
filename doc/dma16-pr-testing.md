# DMA-16 PR blob-discovery comparison

## Tasks and source settings

| Task | Source | Prefix | Listing limit |
| --- | --- | --- | --- |
| AudioSpikeCvpExternal | CVP account, crime-recordings, Azure identity | AUDIO_SPIKE_CVP_PREFIX (default 2026/) | Task batch size (default 1000) |
| AudioSpikeCvp | Existing DARTS account, darts-inbound-container | None | All blobs |
| AudioSpikeCar | Existing DARTS account, darts-unstructured | AUDIO_SPIKE_CAR_PREFIX (default 0) | All matching blobs |
| AudioSpikeCombined | Existing DARTS account, inbound then unstructured | AUDIO_SPIKE_PREFIX (default 0) | Batch size per source |
| AudioSpikeCombinedFaulty | Existing DARTS account, darts-container then unstructured | None for CVP; 0 for CAR | All matching blobs if reached |

All reads list metadata only. No downloads, uploads, copies, database checkpoints or
blob-name logging are performed. "All CAR files" means all names starting with 0,
not all names in the container. Full scans can take longer than the task's nine-minute
execution limit; cancelled/failed scans do not establish a complete container count.
A prefix is a case-sensitive blob-name prefix, not a filename extension filter.

## Deployment and migration

V1_526 remains unchanged. New migration V1_527 renames the old AudioSpikeCvp task to
AudioSpikeCvpExternal and renames its existing system user (-55), preserving identity
access configuration. It adds the new internal AudioSpikeCvp (-58) and
AudioSpikeCombinedFaulty (-59), both disabled. The renamed external task is disabled
too. CAR and combined registrations retain their schedules and existing settings.
Recheck migration version and reserved negative IDs against the target branch before
merging. The source identity is the pod's Azure identity, not the database system user.

Deploy the branch through the PR pipeline using enable_keep_helm and
enable_darts_portal. These are normal automated tasks, not KEDA workers. After the
migration, confirm all five names appear in task administration. Schedules are every
ten minutes; leave every spike task disabled after testing.

Internal tasks reuse AZURE_STORAGE_CONNECTION_STRING unless the existing
AUDIO_SPIKE_CONNECTION_STRING override is supplied. Container overrides remain
AUDIO_SPIKE_CVP_CONTAINER and AUDIO_SPIKE_CAR_CONTAINER. The deliberately nonexistent
CVP container for the faulty variant is fixed to darts-container and must stay absent
for the failure experiment to be meaningful.

External CVP retains AUDIO_SPIKE_CVP_ACCOUNT_URL (sandbox URL by default),
AUDIO_SPIKE_CVP_SOURCE_CONTAINER (crime-recordings) and AUDIO_SPIKE_CVP_PREFIX (2026/).
It uses DefaultAzureCredential, not a source account key or connection-string fallback.
Confirm the approved pod identity, container read permissions, federation and network
access. A 403 AuthorizationPermissionMismatch is a permission issue; a listing timeout
needs network/identity investigation and does not establish successful authorisation.
No Azure role, federation or firewall changes are made by this spike.

## Run manually

1. Confirm each task's enabled state and avoid overlapping scheduled runs during comparisons.
2. Run internal CVP and CAR individually. Record counts and elapsedMs; their scans are
   not truncated by the task batch size. Counts are blob counts, not ingested files.
3. Run combined. Its configured 0 filter and batch cap are intentionally unchanged,
   so its counts are not directly comparable to the broader individual scans.
4. Run combined-faulty. Expect CVP/darts-container FAILED, CAR NOT_ATTEMPTED and the
   overall run FAILED. It deliberately stops at the first exception, like combined.
   A skipped source has count=unknown; that is not an empty-listing result.
5. Run CAR separately after the faulty combined run to show it can succeed independently.
6. Run external CVP separately after confirming source access. Different accounts and
   data volumes prevent direct timing comparisons with internal sources.
7. Repeat normal scans with stable data, equal filters/limits where a fair performance
   comparison is intended, and comparable warm-up conditions. These are not controlled
   benchmark measurements. Listing-page counts are not currently recorded.
8. For schedule tests, enable only the desired variant(s), observe at least two ten-minute
   intervals, then disable them. Separate schedules do not guarantee simultaneous execution.

## Application Insights

```kusto
traces
| where timestamp > ago(24h)
| where cloud_RoleInstance contains "darts-api-pr-PR-NUMBER"
| where message contains "AudioDiscoverySpike"
| project timestamp, cloud_RoleInstance, message
| order by timestamp asc
```

Replace PR-NUMBER. Every run has a run UUID and mode. Every source logs STARTED and
then SUCCESS or FAILED with container, source label, count and elapsedMs. Failed
counts are partial counts only. A failed combined scan logs NOT_ATTEMPTED for each
remaining source with the failedSource and reason, followed by an overall FAILED
duration. Success has an overall SUCCESS duration. Error logs include exception type
and stack trace. Configuration errors before scanning remain framework failures.
The 30-second Azure listing-call timeout is separate from the task execution limit.

Source labels are CVP, CAR and CVP_EXTERNAL. Use mode=COMBINED versus
mode=COMBINED_FAULTY to distinguish the combined experiments. Full listings report
limitReached=false; bounded scans reporting true may be truncated.
