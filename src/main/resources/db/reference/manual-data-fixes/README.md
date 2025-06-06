# Manual data fixes

This directory stores a reference of manual data fixes that have been applied to the production database.

## 001 - migrated transcriptions

This manual data fix applies to migrated transcription data and consists for 2 parts, as described below.

Scripts available under directory [001_migrated_transcriptions](../manual-data-fixes/001_migrated_transcriptions)

**Fix applied date:** 5/6/2025

### Part 1

Fixing duplicate transcriptions with is_current=true, setting is_current=false on the version 1.0 transcription record.

Script: [part1_fix_duplicate_is_current.sql](../manual-data-fixes/001_migrated_transcriptions/part1_fix_duplicate_is_current.sql)

### Part 2

Adding a requested workflow entry for all migrated transcriptions.

Script: [part2_add_requested_workflow.sql](../manual-data-fixes/001_migrated_transcriptions/part2_add_requested_workflow.sql)

## 002 - Reset failed ARM objects status
This manual data fix applies to failed ARM objects so that they can be pushed again in next execution. This is done by updating the status to 14(ARM_RAW_DATA_FAILED_ and transfer_attempts to 1.

**Fix applied date:** 5/6/2025

Scripts available under following location [reset_status_for_failed_arm_objects.sql](../manual-data-fixes/002_failed_arm_jobs_reset_status/reset_status_for_failed_arm_objects.sql)





