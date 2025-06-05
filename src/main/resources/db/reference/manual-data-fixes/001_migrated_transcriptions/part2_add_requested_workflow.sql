--------------------------------------------------------------------------------------------------------------------------
-- Part 2: Create requested workflow entries for all migrated transcriptions                                            --
-- this script should only be run after part1 which ensures there are no  duplicate transcriptions with is_current=true --
--------------------------------------------------------------------------------------------------------------------------

begin;
savepoint dmp5133;

--------------------
-- Initial checks --
--------------------

-- check that no migrated transcriptions have a requested workflow
-- we don't want to introduce one where it already exists
SELECT count(*) transcriptions_with_requested_workflow
FROM darts.transcription tra
JOIN darts.transcription_workflow trw ON trw.tra_id = tra.tra_id AND trw.trs_id = 1
WHERE chronicle_id IS NOT NULL;

-- get the count of transcriptions that should have a requested workflow added
-- record this for cross-referencing after the fix is applied
SELECT count(*) transcriptions_without_requested_workflow
FROM darts.transcription tra
WHERE tra.is_current
AND tra.chronicle_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1
    FROM darts.transcription_workflow trw
    WHERE trw.tra_id = tra.tra_id
    AND trw.trs_id = 1
);

-- this query is used to check the different types of requested workflows that will be created
-- check the all_total matches the count of "transcriptions_without_requested_workflow" from the query above.
WITH potential_new_workflows AS (
	SELECT
        -- is_current tra
        tra.tra_id,
        -- 1 = requested status
        1 trs_id,
        -- the requested by on the is_current transcription
        tra.requested_by tra_requested_by,
        -- requested_by from version 1.0 is used as workflow_actor
        -- if requested_by is null, use the migration user
        COALESCE(tra_v1_0.requested_by, -99) workflow_actor,
        -- created_ts from version 1.0 is used as workflow_ts
        tra_v1_0.created_ts workflow_ts
	FROM darts.transcription tra
	JOIN darts.transcription tra_v1_0 ON tra_v1_0.chronicle_id = tra.chronicle_id AND tra_v1_0.version_label = '1.0'
	WHERE tra.is_current
	AND tra.chronicle_id IS NOT NULL
	-- no requested workflow exists
	AND NOT EXISTS (
		SELECT 1
		FROM darts.transcription_workflow trw
		WHERE trw.tra_id = tra.tra_id
		AND trw.trs_id = 1
	)
), workflow_actor_same AS (
	SELECT count(*) total
	FROM potential_new_workflows nw
	WHERE nw.tra_requested_by = nw.workflow_actor
), workflow_actor_different AS (
	SELECT count(*) total
	FROM potential_new_workflows nw
	WHERE nw.tra_requested_by != nw.workflow_actor
), requested_by_null AS (
	SELECT count(*) total
	FROM potential_new_workflows nw
	WHERE nw.tra_requested_by IS NULL
)
SELECT
    was.total workflow_actor_same_total,
    wad.total workflow_actor_different_total,
    rbn.total requested_by_null_total,
	was.total + wad.total + rbn.total all_total
FROM workflow_actor_same was, workflow_actor_different wad, requested_by_null rbn;

--------------
-- Data fix --
--------------

-- disable triggers in the current session, so that inserts into transcription_workflow do not update the transcription.trs_id
SET session_replication_role = 'replica';

-- this is the data fix
-- insert requested workflows into the transcription_workflow table
INSERT INTO darts.transcription_workflow (
	trw_id,
	tra_id,
	trs_id,
	workflow_actor,
	workflow_ts
)
SELECT
    nextval('darts.trw_seq') trw_id,
    -- is_current tra
    tra.tra_id,
    -- 1 = requested status
    1 trs_id,
    -- requested_by from version 1.0 is used as workflow_actor
    -- if requested_by is null, use the migration user
    COALESCE(tra_v1_0.requested_by, -99) workflow_actor,
    -- created_ts from version 1.0 is used as workflow_ts
    tra_v1_0.created_ts workflow_ts
FROM darts.transcription tra
JOIN darts.transcription tra_v1_0 ON tra_v1_0.chronicle_id = tra.chronicle_id AND tra_v1_0.version_label = '1.0'
WHERE tra.is_current
AND tra.chronicle_id IS NOT NULL
-- no requested workflow exists
AND NOT EXISTS (
    SELECT 1
    FROM darts.transcription_workflow trw
    WHERE trw.tra_id = tra.tra_id
    AND trw.trs_id = 1
);

-- exit replication mode to re-enable triggers
SET session_replication_role = 'origin';

---------------------
-- Post-fix checks --
---------------------

-- check that no migrated transcriptions have trs_id = 1, expect 0
-- anything other than 0 means the trigger was not disabled and the insert should be rolled back
SELECT count(*)
FROM darts.transcription tra
WHERE tra.is_current
AND tra.chronicle_id IS NOT NULL
AND tra.trs_id = 1;

-- check that all migrated transcriptions now have a requested workflow
-- this should equal the "transcriptions_without_requested_workflow" count from the initial checks
SELECT count(*) transcriptions_with_requested_workflow
FROM darts.transcription tra
JOIN darts.transcription_workflow trw ON trw.tra_id = tra.tra_id AND trw.trs_id = 1
WHERE chronicle_id IS NOT NULL;

-- we would now expect there to be 0 records without a requested workflow
SELECT count(*) transcriptions_without_requested_workflow
FROM darts.transcription tra
WHERE tra.is_current
AND tra.chronicle_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1
    FROM darts.transcription_workflow trw
    WHERE trw.tra_id = tra.tra_id
    AND trw.trs_id = 1
);

-- script is currently configured to rollback, if run end-to-end
rollback to dmp5133;
--commit;

-- should the changes in this script need to be reverted after being committed
-- the following query can be used to delete the requested workflows that were added

--DELETE FROM darts.transcription_workflow trw
--WHERE trw.trs_id = 1
--AND trw.tra_id IN (
--	SELECT tra_id
--	FROM darts.transcription
--	WHERE is_current
--	AND chronicle_id IS NOT NULL
--);
