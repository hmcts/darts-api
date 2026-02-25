INSERT INTO darts.automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                                  created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (35, 'CleanUpDetsData', 'Cleans up Dets files that have successfully been stored in ARM', '0 24 0-6,19-23 ? * *', true, 100_000,
        current_timestamp, 0, current_timestamp, 0, false);

INSERT INTO user_account
VALUES (-40, NULL, '', 'systemCleanUpDetsData@hmcts.net',
        'systemCleanUpDetsDataAutomatedTask', '2025-02-16 00:00:00+00', '2025-02-16 00:00:00+00', NULL, 0, 0, NULL, true,
        true, d 'systemCleanUpDetsDataAutomatedTask');

SET SEARCH_PATH TO darts;

DROP PROCEDURE IF EXISTS dets_cleanup_eod_osr;
DROP TYPE IF EXISTS id_location_pair;

CREATE TYPE id_location_pair AS (
                                    osr_uuid      BIGINT,
                                    dets_location TEXT
                                );

CREATE INDEX IF NOT EXISTS osr_file_dets_cleanup_idx ON object_state_record (date_file_dets_cleanup, flag_file_dets_cleanup_status);

CREATE OR REPLACE PROCEDURE dets_cleanup_eod_osr(
    IN  pi_limit            INTEGER,
    IN  pi_last_modified_ts TIMESTAMPTZ,
    OUT po_results_array    id_location_pair[]
)
    LANGUAGE 'plpgsql'
AS $$
DECLARE
    c_msg_prefix       CONSTANT TEXT := 'dets_cleanup_eod_osr:';
    v_dets_eod_ids              BIGINT[];
    v_arm_eod_ids               BIGINT[];
    v_incomplete_cleanup        id_location_pair[];

    v_dets_eod_count            INTEGER;
    v_arm_eod_count             INTEGER;
    v_results_count             INTEGER;
    v_incomplete_cleanup_count  INTEGER;

    v_dup_count                 INTEGER;
    v_deleted_count             INTEGER;
    v_updated_count             INTEGER;
BEGIN
    RAISE NOTICE '% Started at % [pi_limit = %, pi_last_modified_ts = %]', c_msg_prefix, clock_timestamp(), pi_limit, pi_last_modified_ts;

    BEGIN
        --Get DETS records to be deleted, limited by pi_limit
        --   DETS record: OSR and EOD records must match on eod_id and locations, where eod.elt_id = 4 (DETS)
        --   ARM record : OSR and EOD records must match where OSR.arm_eod_id = EOD.eod_id, where eod.elt_id = 3 (ARM)
        --                ARM record must be STORED (i.e. ors_id = 2) and it's last_modified_ts is before or equal to pi_last_modified_ts
        SELECT array_agg(eod_id)
             , array_agg(arm_eod_id)
             , array_agg((osr_uuid, dets_location)::id_location_pair)
        INTO v_dets_eod_ids
            , v_arm_eod_ids
            , po_results_array
        FROM (
                 SELECT osr.osr_uuid, osr.dets_location, eod_dets.eod_id, eod_arm.eod_id AS arm_eod_id
                 FROM object_state_record osr
                          JOIN external_object_directory eod_dets
                               ON eod_dets.eod_id = osr.eod_id
                                   AND eod_dets.external_location = osr.dets_location
                          JOIN external_object_directory eod_arm
                               ON eod_arm.eod_id = osr.arm_eod_id
                 WHERE eod_dets.elt_id = 4 --DETS
                   AND eod_arm.elt_id = 3  --ARM
                   AND eod_arm.ors_id = 2  --STORED
                   AND eod_arm.last_modified_ts <= pi_last_modified_ts
                 ORDER BY osr.osr_uuid
                 LIMIT pi_limit
             ) t;

        v_dets_eod_count := COALESCE(cardinality(v_dets_eod_ids), 0);
        v_arm_eod_count  := COALESCE(cardinality(v_arm_eod_ids), 0);
        v_results_count  := COALESCE(cardinality(po_results_array), 0);

        RAISE NOTICE '% Number of records in DETS EOD id array = %', c_msg_prefix, v_dets_eod_count;
        RAISE NOTICE '% Number of records in ARM  EOD id array = %', c_msg_prefix, v_arm_eod_count;
        RAISE NOTICE '% Number of records in results array = %', c_msg_prefix, v_results_count;

        -- Check to ensure duplicate records were not returned. Indicates that there is more than one ARM or DETS record, which should not happen
        SELECT COUNT(*)
        INTO v_dup_count
        FROM (
                 SELECT elem
                 FROM unnest(v_arm_eod_ids || v_dets_eod_ids) AS elem
                 GROUP BY elem
                 HAVING COUNT(*) > 1
             ) AS t;

        RAISE NOTICE '% Duplicate count = %', c_msg_prefix, v_dup_count;

        IF v_dup_count > 0 THEN
            RAISE EXCEPTION 'Validation failed: Duplicate records [%] found.', v_dup_count;
        END IF;

        --Before updating OSR, retrieve records from OSR that have a date_file_dets_cleanup value but flag_file_dets_cleanup_status has not been set.
        --  append the results to the OUT parameter
        SELECT array_agg((osr_uuid, dets_location)::id_location_pair ORDER BY osr_uuid) AS id_location_pairs
        INTO v_incomplete_cleanup
        FROM object_state_record
        WHERE date_file_dets_cleanup IS NOT NULL
          AND (flag_file_dets_cleanup_status IS NULL
            OR flag_file_dets_cleanup_status = FALSE);

        v_incomplete_cleanup_count := COALESCE(cardinality(v_incomplete_cleanup), 0);

        RAISE NOTICE '% Number of records in incomplete cleanup array = %', c_msg_prefix, v_incomplete_cleanup_count;

        po_results_array := COALESCE( CASE WHEN v_results_count > 0
            AND v_incomplete_cleanup_count > 0
                                               THEN po_results_array || v_incomplete_cleanup
                                           WHEN v_results_count > 0
                                               THEN po_results_array
                                           WHEN v_incomplete_cleanup_count > 0
                                               THEN v_incomplete_cleanup
                                          END,
                                      '{}'::id_location_pair[]
                            );

        RAISE NOTICE '% Number of records in combined results array = %', c_msg_prefix, COALESCE(cardinality(po_results_array), 0);

        --Only perform DELETE and UPDATE if there are values to process
        IF v_dets_eod_count > 0 THEN

            --Delete records from EOD
            DELETE FROM external_object_directory
            WHERE eod_id = ANY(v_dets_eod_ids);

            GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
            RAISE NOTICE '% Number of EOD records deleted = %', c_msg_prefix, v_deleted_count;

            --Update OSR records
            UPDATE object_state_record
            SET eod_id = NULL
              , date_file_dets_cleanup = CURRENT_TIMESTAMP
            WHERE eod_id = ANY(v_dets_eod_ids);

            GET DIAGNOSTICS v_updated_count = ROW_COUNT;
            RAISE NOTICE '% Number of OSR records updated = %', c_msg_prefix, v_updated_count;

        ELSE
            RAISE NOTICE '% There is nothing to process. No EOD Deletes or OSR Updates were executed.', c_msg_prefix;
        END IF;

    EXCEPTION
        WHEN OTHERS THEN
            RAISE NOTICE '% Error in dets_cleanup_eod_osr: % - %', c_msg_prefix, SQLSTATE, SQLERRM;
            RAISE;
    END;

    --Only COMMIT if there were values to process
    IF v_dets_eod_count > 0 THEN
        RAISE NOTICE '%   Commit started at %', c_msg_prefix, clock_timestamp();
        COMMIT;
        RAISE NOTICE '%   Commit finished at %', c_msg_prefix, clock_timestamp();
    END IF;

    RAISE NOTICE '% Finished at %', c_msg_prefix, clock_timestamp();
END;
$$;