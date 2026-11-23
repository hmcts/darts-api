# DMA-16 PR blob-discovery comparison

Deploy this branch using the normal PR pipeline. This adds three tasks, all disabled
by migration V1_526: AudioSpikeCvp, AudioSpikeCar and AudioSpikeCombined.
System users -55, -56 and -57 are reserved by this migration; recheck migration
version and user IDs against the target branch before merging.

## Deployment settings

Set these on the PR API deployment using its secret/configuration mechanism:

| Variable | Value                                                         |
| --- |---------------------------------------------------------------|
| AUDIO_SPIKE_CONNECTION_STRING | Secret reference for the dedicated Azure test storage account |
| AUDIO_SPIKE_CVP_CONTAINER | darts-inbound-container                                                     |
| AUDIO_SPIKE_CAR_CONTAINER | darts-unstructured                                                     |
| AUDIO_SPIKE_PREFIX | 2026/                                                         |

Never commit the connection string. The PR pod must have network access to storage.
This implementation uses a connection string, not managed identity. No fallback to
existing ingestion storage is used. No deployment/secret configuration has been
applied automatically by these source changes.

Upload known samples under 2026/ in both containers. Files are only listed, not
downloaded, deleted, ingested or saved to the database. Blob names are not logged.

## Comparison

1. Confirm all three tasks appear inactive in the PR portal.
2. Use Run task for AudioSpikeCombined. Record counts, per-container times and total.
3. Run AudioSpikeCvp and AudioSpikeCar separately. Record counts and timings.
4. Repeat with the same sample. Counts should agree; batch size limits each source
   to 1000 by default. limitReached=true means the sample may be truncated; increase
   the task batch size to cover the sample. Listings always restart at the prefix.
5. Compare schedules by enabling only the combined task, then disabling it and
   enabling the two separate tasks. Cron is every ten minutes, with separate locks.
   Separate tasks are independently schedulable but simultaneous execution is not
   guaranteed by this spike.
6. Change only the CVP spike container setting to a nonexistent name and redeploy.
   Combined fails before scanning CAR. CAR-only still succeeds. Restore the setting.
7. Leave all three tasks inactive after testing.

Application Insights query (replace PR-NUMBER):

```kusto
traces
| where timestamp > ago(24h)
| where cloud_RoleInstance contains "darts-api-pr-PR-NUMBER-api"
| where message contains "AudioDiscoverySpike"
| project timestamp, cloud_RoleInstance, message
| order by timestamp asc
```

The service uses a run UUID to correlate container and total timings. Failed listing
propagates to the existing framework; there is no successful total for a partial run.
Combined runs list CVP then CAR sequentially. Timing includes client/listing overhead;
this is not a controlled benchmark. Repeat and account for network/cache variability.

This spike does not implement new-file tracking, the 30-minute delay, audio processing,
managed identity or persisted scan checkpoints. It does not change the existing
FindFilesFromCVPAndCAR task. Live PR deployment and evidence remain outstanding.
