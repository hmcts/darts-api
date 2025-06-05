# Manual data fixes

This directory stores a reference of manual data fixes that have been applied to the production database.

## 001 - migrated transcriptions

This manual data fix applies to migrated transcription data and consists for 2 parts, as described below.

Scripts available under directory [001_migrated_transcriptions](../manual-data-fixes/001_migrated_transcriptions)

**Fix applied date:** Not yet applied 

### Part 1

Fixing duplicate transcriptions with is_current=true, setting is_current=false on the version 1.0 transcription record.

Script: [part1_fix_duplicate_is_current.sql](../manual-data-fixes/001_migrated_transcriptions/part1_fix_duplicate_is_current.sql)

### Part 2

Adding a requested workflow entry for all migrated transcriptions.

Script: [part2_add_requested_workflow.sql](../manual-data-fixes/001_migrated_transcriptions/part2_add_requested_workflow.sql)
