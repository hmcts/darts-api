begin;
savepoint dmp5154;

--
-- pre-check list of EOD records to be updated to set status to 14 (ARM_RAW_DATA_FAILED) and transfer_attempts to 1 (expect 1414)
--
SELECT count(*) FROM darts.external_object_directory
WHERE ors_id = 17;

--
--
--
UPDATE darts.external_object_directory
SET ors_id = 14,
last_modified_ts = now(),
transfer_attempts=1
WHERE ors_id = 17;

-- script is currently configured to rollback, if run end-to-end
rollback to dmp5154;
--commit;