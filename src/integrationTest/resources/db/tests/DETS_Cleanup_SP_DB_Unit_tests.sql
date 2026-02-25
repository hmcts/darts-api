--psql -h <HOST> -p 5432 -U <USER> -d darts -f DETS_Cleanup_SP_DB_Unit_tests.sql 1> DETS_Cleanup_SP_DB_Unit_tests.log 2>>&1

----------------------------------------------------------------------
-- ONE-TIME SETUP
----------------------------------------------------------------------

CREATE SCHEMA IF NOT EXISTS test_dets_cleanup;

CREATE TABLE IF NOT EXISTS test_dets_cleanup.external_object_directory
(LIKE darts.external_object_directory INCLUDING ALL);

CREATE TABLE IF NOT EXISTS test_dets_cleanup.object_state_record
(LIKE darts.object_state_record INCLUDING ALL);

----------------------------------------------------------------------
-- TEST HARNESS
----------------------------------------------------------------------

-- Route unqualified table names to test_dets_cleanup tables first
SET search_path TO test_dets_cleanup, darts;

-- Following values taken from Prod
SET max_parallel_workers_per_gather=4;
SET work_mem='4096kB';
SET max_parallel_maintenance_workers=64;

SELECT name, setting, unit, SOURCE, reset_val --, boot_val
FROM pg_settings
WHERE name IN ('search_path', 'max_parallel_workers_per_gather', 'work_mem', 'max_parallel_maintenance_workers')
ORDER BY name;


-- Helper to clear test tables between tests
CREATE OR REPLACE PROCEDURE test_dets_cleanup.reset_test_data() LANGUAGE plpgsql AS $$
BEGIN
    TRUNCATE TABLE test_dets_cleanup.object_state_record;
    TRUNCATE TABLE test_dets_cleanup.external_object_directory;

    RAISE NOTICE '-------------- RESET ---------------';
END;
$$;

----------------------------------------------------------------------
-- TEST 1: No matching records
-- Expect: 0 results, empty array returned
----------------------------------------------------------------------

CALL test_dets_cleanup.reset_test_data();

-- Noise ARM EODs (wrong ors_id), Includes valid records but outside of date range
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (3001, 8980069, 2, 3, NULL, now(), now() - interval '1 day', -99, -99, FALSE, FALSE, FALSE),
    (3002, 8980069, 1, 3, NULL, now(), now() - interval '10 days', -99, -99, FALSE, FALSE, FALSE),
    (3003, 8980069, 1, 3, NULL, now(), now() - interval '7 days', -99, -99, FALSE, FALSE, FALSE),
    (3004, 8980069, 2, 3, NULL, now(), now() - interval '5 days', -99, -99, FALSE, FALSE, FALSE),
    (3005, 8980069, 1, 3, NULL, now(), now() - interval '20 days', -99, -99, FALSE, FALSE, FALSE);

-- Noise DETS EODs
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (4001, 8980069, 2, 4, 'LOC-N1', now(), now() - interval '1 day', -99, -99, FALSE, FALSE, TRUE),
    (4002, 8980069, 2, 4, 'LOC-N2', now(), now() - interval '10 days', -99, -99, FALSE, FALSE, TRUE),
    (4003, 8980069, 2, 4, 'LOC-N3', now(), now() - interval '7 days', -99, -99, FALSE, FALSE, TRUE),
    (4004, 8980069, 2, 4, 'LOC-N4', now(), now() - interval '5 days', -99, -99, FALSE, FALSE, TRUE),
    (4005, 8980069, 2, 4, 'LOC-N5', now(), now() - interval '20 days', -99, -99, FALSE, FALSE, TRUE);

-- Noise OSR rows
INSERT INTO object_state_record (osr_uuid, eod_id, arm_eod_id, dets_location)
VALUES
    (10001, 4001, 3001, 'LOC-N1'),
    (10002, 4002, 3002, 'LOC-N2'),
    (10003, 4003, 3003, 'LOC-N3'),
    (10004, 4004, 3004, 'LOC-N4'),
    (10005, 4005, 3005, 'LOC-N5');

DO $$
    DECLARE
        results darts.id_location_pair[];
    BEGIN
        RAISE NOTICE '------------------------------------';
        RAISE NOTICE '              TEST 1';
        RAISE NOTICE '------------------------------------';

        CALL darts.dets_cleanup_eod_osr(10, now() - interval '7 days', results);

        --ASSERT (results IS NULL OR cardinality(results) = 0), 'TEST 1 FAILED: expected empty results but got non-empty array';

        -- Check results
        IF results IS NULL OR cardinality(results) > 0 THEN
            RAISE NOTICE 'TEST 1 FAILED: expected 0 results but got %', cardinality(results);
            RETURN;
        END IF;

        RAISE NOTICE 'TEST 1 PASSED';
    END $$;

----------------------------------------------------------------------
-- TEST 2: Normal case (5 valid pairs + noise)
----------------------------------------------------------------------

CALL test_dets_cleanup.reset_test_data();

-- 5 valid ARM EODs
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (3101, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE),
    (3102, 8980069, 2, 3, NULL, now(), now() - interval '7 days', -99, -99, FALSE, FALSE, FALSE),
    (3103, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE),
    (3104, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE),
    (3105, 8980069, 2, 3, NULL, now(), now() - interval '20 days', -99, -99, FALSE, FALSE, FALSE);

-- 5 valid DETS EODs
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (4101, 8980069, 2, 4, 'LOC-A1', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4102, 8980069, 2, 4, 'LOC-A2', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4103, 8980069, 2, 4, 'LOC-A3', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4104, 8980069, 2, 4, 'LOC-A4', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4105, 8980069, 2, 4, 'LOC-A5', now(), now(), -99, -99, FALSE, FALSE, TRUE);

-- 5 valid OSR rows
INSERT INTO object_state_record (osr_uuid, eod_id, arm_eod_id, dets_location)
VALUES
    (11001, 4101, 3101, 'LOC-A1'),
    (11002, 4102, 3102, 'LOC-A2'),
    (11003, 4103, 3103, 'LOC-A3'),
    (11004, 4104, 3104, 'LOC-A4'),
    (11005, 4105, 3105, 'LOC-A5');

-- Noise ARM (older than 7 days but invalid ors_id + within 7 days and valid ors_id + valid records apart from difference location)
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (3191, 8980069, 1, 3, NULL, now(), now() - interval '10 days', -99, -99, FALSE, FALSE, FALSE),
    (3192, 8980069, 2, 3, NULL, now(), now() - interval '1 days', -99, -99, FALSE, FALSE, FALSE),
    (3193, 8980069, 2, 3, NULL, now(), now() - interval '10 days', -99, -99, FALSE, FALSE, FALSE);

-- Noise DETS
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (4191, 8980069, 2, 4, 'LOC-Z1', now(), now() - interval '10 days', -99, -99, FALSE, FALSE, TRUE),
    (4192, 8980069, 2, 4, 'LOC-Z2', now(), now() - interval '10 days', -99, -99, FALSE, FALSE, TRUE),
    (4193, 8980069, 2, 4, 'LOC-ZZ Different', now(), now() - interval '10 days', -99, -99, FALSE, FALSE, TRUE);

INSERT INTO object_state_record (osr_uuid, eod_id, arm_eod_id, dets_location)
VALUES (11901, 4191, 3191, 'LOC-Z1'),
       (11902, 4192, 3192, 'LOC-Z2'),
       (11903, 4193, 3193, 'LOC-ZZ');

DO $$
    DECLARE
        results darts.id_location_pair[];
        expected darts.id_location_pair[];
    BEGIN
        RAISE NOTICE '------------------------------------';
        RAISE NOTICE '              TEST 2';
        RAISE NOTICE '------------------------------------';

        expected := ARRAY[
            ROW(11001, 'LOC-A1')::darts.id_location_pair,
            ROW(11002, 'LOC-A2')::darts.id_location_pair,
            ROW(11003, 'LOC-A3')::darts.id_location_pair,
            ROW(11004, 'LOC-A4')::darts.id_location_pair,
            ROW(11005, 'LOC-A5')::darts.id_location_pair
            ];

        CALL darts.dets_cleanup_eod_osr(10, now() - interval '7 days', results);

        --ASSERT cardinality(results) = 5, 'TEST 2 FAILED: expected 5 results but got ' || cardinality(results);
        --ASSERT results = expected, 'TEST 2 FAILED: results array does not match expected values';

        -- Check count
        IF cardinality(results) <> 5 THEN
            RAISE NOTICE 'TEST 2 FAILED: expected 5 results but got %', cardinality(results);
            RETURN;
        END IF;

        -- Check content
        IF results IS DISTINCT FROM expected THEN
            RAISE NOTICE 'TEST 2 FAILED: results array does not match expected values';
            RETURN;
        END IF;

        RAISE NOTICE 'TEST 2 PASSED';

    END $$;

----------------------------------------------------------------------------------------
-- TEST 3: Limit behavior (limit = 3), more available
-- Test includes a check where the last_modified_ts is exactly the same as the parameter
----------------------------------------------------------------------------------------

CALL test_dets_cleanup.reset_test_data();

-- 5 valid ARM EODs (one exact timestamp match)
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (3201, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE),
    (3202, 8980069, 2, 3, NULL, now(), (now()::date - 7)::timestamp, -99, -99, FALSE, FALSE, FALSE), -- exact match
    (3203, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE),
    (3204, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE),
    (3205, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE);

-- 5 valid DETS EODs
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (4201, 8980069, 2, 4, 'LOC-B1', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4202, 8980069, 2, 4, 'LOC-B2 - Exact TS', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4203, 8980069, 2, 4, 'LOC-B3', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4204, 8980069, 2, 4, 'LOC-B4', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4205, 8980069, 2, 4, 'LOC-B5', now(), now(), -99, -99, FALSE, FALSE, TRUE);

-- 5 valid OSR rows
INSERT INTO object_state_record (osr_uuid, eod_id, arm_eod_id, dets_location)
VALUES
    (12001, 4201, 3201, 'LOC-B1'),
    (12002, 4202, 3202, 'LOC-B2 - Exact TS'),
    (12003, 4203, 3203, 'LOC-B3'),
    (12004, 4204, 3204, 'LOC-B4'),
    (12005, 4205, 3205, 'LOC-B5');

-- Noise ARM (older than 7 days but invalid ors_id)
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (3291, 8980069, 1, 3, NULL, now(), now() - interval '10 days', -99, -99, FALSE, FALSE, FALSE);

-- Noise DETS
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (4291, 8980069, 1, 4, 'LOC-Z2', now(), now() - interval '10 days', -99, -99, FALSE, FALSE, TRUE);

INSERT INTO object_state_record (osr_uuid, eod_id, arm_eod_id, dets_location)
VALUES (12901, 4291, 3291, 'LOC-Z2');

DO $$
    DECLARE
        results darts.id_location_pair[];
        expected darts.id_location_pair[];
    BEGIN
        RAISE NOTICE '------------------------------------';
        RAISE NOTICE '              TEST 3';
        RAISE NOTICE '------------------------------------';

        expected := ARRAY[
            ROW(12001, 'LOC-B1')::darts.id_location_pair,
            ROW(12002, 'LOC-B2 - Exact TS')::darts.id_location_pair,
            ROW(12003, 'LOC-B3')::darts.id_location_pair
            ];

        CALL darts.dets_cleanup_eod_osr(3, (now()::date - 7)::timestamp, results);

        --ASSERT cardinality(results) = 3, 'TEST 3 FAILED: expected 3 results but got ' || cardinality(results);
        --ASSERT results = expected, 'TEST 3 FAILED: results array does not match expected values';

        -- Check count
        IF cardinality(results) <> 3 THEN
            RAISE NOTICE 'TEST 3 FAILED: expected 3 results but got %', cardinality(results);
            RETURN;
        END IF;

        -- Check content
        IF results IS DISTINCT FROM expected THEN
            RAISE NOTICE 'TEST 3 FAILED: results array does not match expected values';
            RETURN;
        END IF;

        RAISE NOTICE 'TEST 3 PASSED';
    END $$;

----------------------------------------------------------------------------------------
-- TEST 4: Two-call behavior
-- Call 1: limit = 3 - expect first 3 valid pairs
-- Call 2: limit = 3 - expect 2 valid pairs + 3 incomplete cleanup, from call 1
-- Test includes a check where the last_modified_ts is exactly the same as the parameter
----------------------------------------------------------------------------------------

CALL test_dets_cleanup.reset_test_data();

-- 5 valid ARM EODs (one exact timestamp match)
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (3301, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE),
    (3302, 8980069, 2, 3, NULL, now(), (now()::date - 7)::timestamp, -99, -99, FALSE, FALSE, FALSE), -- exact match check
    (3303, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE),
    (3304, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE),
    (3305, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE);

-- 5 valid DETS EODs
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (4301, 8980069, 2, 4, 'LOC-C1', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4302, 8980069, 2, 4, 'LOC-C2', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4303, 8980069, 2, 4, 'LOC-C3', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4304, 8980069, 2, 4, 'LOC-C4', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4305, 8980069, 2, 4, 'LOC-C5', now(), now(), -99, -99, FALSE, FALSE, TRUE);

-- 5 valid OSR rows
INSERT INTO object_state_record (osr_uuid, eod_id, arm_eod_id, dets_location)
VALUES
    (13001, 4301, 3301, 'LOC-C1'),
    (13002, 4302, 3302, 'LOC-C2'),
    (13003, 4303, 3303, 'LOC-C3'),
    (13004, 4304, 3304, 'LOC-C4'),
    (13005, 4305, 3305, 'LOC-C5');

-- Noise ARM (older than 7 days but invalid ors_id + not older than 7 days but valid ors_id)
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (3391, 8980069, 1, 3, NULL, now(), now() - interval '10 days', -99, -99, FALSE, FALSE, FALSE),
    (3392, 8980069, 2, 3, NULL, now(), now() - interval '2 days', -99, -99, FALSE, FALSE, FALSE);

-- DETS
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (4391, 8980069, 2, 4, 'LOC-Z3', now(), now() - interval '10 days', -99, -99, FALSE, FALSE, TRUE),
    (4392, 8980069, 2, 4, 'LOC-Z4', now(), now() - interval '10 days', -99, -99, FALSE, FALSE, TRUE);

INSERT INTO object_state_record (osr_uuid, eod_id, arm_eod_id, dets_location)
VALUES (13901, 4391, 3391, 'LOC-Z3'),
       (13902, 4392, 3392, 'LOC-Z4');

-- 2 calls: limit = 3
DO $$
    DECLARE
        results  darts.id_location_pair[];
        expected darts.id_location_pair[];
    BEGIN
        RAISE NOTICE '------------------------------------';
        RAISE NOTICE '              TEST 4';
        RAISE NOTICE '------------------------------------';

        --Call 1: limit 3 - Should behave normally
        expected := ARRAY[
            ROW(13001, 'LOC-C1')::darts.id_location_pair,
            ROW(13002, 'LOC-C2')::darts.id_location_pair,
            ROW(13003, 'LOC-C3')::darts.id_location_pair
            ];

        CALL darts.dets_cleanup_eod_osr(3, (now()::date - 7)::timestamp, results);

        --ASSERT cardinality(results) = 3, 'TEST 4 CALL 1 FAILED: expected 3 results but got ' || cardinality(results);
        --ASSERT results = expected, 'TEST 4 CALL 1 FAILED: results array does not match expected values';

        -- Check count
        IF cardinality(results) <> 3 THEN
            RAISE NOTICE 'TEST 4 CALL 1 FAILED: expected 3 results but got %', cardinality(results);
            RETURN;
        END IF;

        -- Check content
        IF results IS DISTINCT FROM expected THEN
            RAISE NOTICE 'TEST 4 CALL 1 FAILED: results array does not match expected values';
            RETURN;
        END IF;

        RAISE NOTICE 'TEST 4 CALL 1 PASSED';

        --Call 2: limit = 3 - Expect the other 2 valid pair and the 3 from call 1, because their flag hasn't been set)
        expected := ARRAY[
            ROW(13004, 'LOC-C4')::darts.id_location_pair,
            ROW(13005, 'LOC-C5')::darts.id_location_pair,
            ROW(13001, 'LOC-C1')::darts.id_location_pair,
            ROW(13002, 'LOC-C2')::darts.id_location_pair,
            ROW(13003, 'LOC-C3')::darts.id_location_pair
            ];

        CALL darts.dets_cleanup_eod_osr(3, now() - interval '7 days', results);

        --ASSERT cardinality(results) = 5, 'TEST 4 CALL 2 FAILED: expected 5 results but got ' || cardinality(results);
        --ASSERT results = expected, 'TEST 4 CALL 2 FAILED: results array does not match expected values';

        -- Check count
        IF cardinality(results) <> 5 THEN
            RAISE NOTICE 'TEST 4 CALL 2 FAILED: expected 5 results but got %', cardinality(results);
            RETURN;
        END IF;

        -- Check content
        IF results IS DISTINCT FROM expected THEN
            RAISE NOTICE 'TEST 4 CALL 2 FAILED: results array does not match expected values';
            RETURN;
        END IF;

        RAISE NOTICE 'TEST 4 CALL 2 PASSED';
    END $$;

----------------------------------------------------------------------------------------
-- TEST 5: Two-call behavior
-- Call 1: limit = 3 - expect first 3 valid pairs
--  Update deleted flag
-- Call 2: limit = 3 - expect the remaining 2 valid pairs (0 incomplete cleanup)
-- Test includes a check where the last_modified_ts is exactly the same as the parameter
----------------------------------------------------------------------------------------

CALL test_dets_cleanup.reset_test_data();

-- 5 valid ARM EODs (one exact timestamp match)
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (3301, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE),
    (3302, 8980069, 2, 3, NULL, now(), (now()::date - 7)::timestamp, -99, -99, FALSE, FALSE, FALSE), -- exact match check
    (3303, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE),
    (3304, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE),
    (3305, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE);

-- 5 valid DETS EODs
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (4301, 8980069, 2, 4, 'LOC-C1', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4302, 8980069, 2, 4, 'LOC-C2', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4303, 8980069, 2, 4, 'LOC-C3', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4304, 8980069, 2, 4, 'LOC-C4', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4305, 8980069, 2, 4, 'LOC-C5', now(), now(), -99, -99, FALSE, FALSE, TRUE);

-- 5 valid OSR rows
INSERT INTO object_state_record (osr_uuid, eod_id, arm_eod_id, dets_location)
VALUES
    (13001, 4301, 3301, 'LOC-C1'),
    (13002, 4302, 3302, 'LOC-C2'),
    (13003, 4303, 3303, 'LOC-C3'),
    (13004, 4304, 3304, 'LOC-C4'),
    (13005, 4305, 3305, 'LOC-C5');

-- Noise ARM (older than 7 days but invalid ors_id + not older than 7 days but valid ors_id)
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (3391, 8980069, 1, 3, NULL, now(), now() - interval '10 days', -99, -99, FALSE, FALSE, FALSE),
    (3392, 8980069, 2, 3, NULL, now(), now() - interval '2 days', -99, -99, FALSE, FALSE, FALSE);

-- DETS
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (4391, 8980069, 2, 4, 'LOC-Z3', now(), now() - interval '10 days', -99, -99, FALSE, FALSE, TRUE),
    (4392, 8980069, 2, 4, 'LOC-Z4', now(), now() - interval '10 days', -99, -99, FALSE, FALSE, TRUE);

INSERT INTO object_state_record (osr_uuid, eod_id, arm_eod_id, dets_location)
VALUES (13901, 4391, 3391, 'LOC-Z3'),
       (13902, 4392, 3392, 'LOC-Z4');

-- 2 calls: limit = 3
DO $$
    DECLARE
        results  darts.id_location_pair[];
        expected darts.id_location_pair[];
    BEGIN
        RAISE NOTICE '------------------------------------';
        RAISE NOTICE '              TEST 5';
        RAISE NOTICE '------------------------------------';

        --Call 1: limit 3 - Should behave normally
        expected := ARRAY[
            ROW(13001, 'LOC-C1')::darts.id_location_pair,
            ROW(13002, 'LOC-C2')::darts.id_location_pair,
            ROW(13003, 'LOC-C3')::darts.id_location_pair
            ];

        CALL darts.dets_cleanup_eod_osr(3, (now()::date - 7)::timestamp, results);

        --ASSERT cardinality(results) = 3, 'TEST 5 CALL 1 FAILED: expected 3 results but got ' || cardinality(results);
        --ASSERT results = expected, 'TEST 5 CALL 1 FAILED: results array does not match expected values';

        -- Check count
        IF cardinality(results) <> 3 THEN
            RAISE NOTICE 'TEST 5 CALL 1 FAILED: expected 3 results but got %', cardinality(results);
            RETURN;
        END IF;

        -- Check content
        IF results IS DISTINCT FROM expected THEN
            RAISE NOTICE 'TEST 5 CALL 1 FAILED: results array does not match expected values';
            RETURN;
        END IF;

        RAISE NOTICE 'TEST 5 CALL 1 PASSED';

        --Update the flag_file_dets_cleanup_status flag to simulate final cleanup has completed
        UPDATE object_state_record
        SET flag_file_dets_cleanup_status = TRUE
        WHERE osr_uuid IN (13001, 13002, 13003);

        --Call 2: limit = 3 - Expect the other 2 valid pairs ONLY)
        expected := ARRAY[
            ROW(13004, 'LOC-C4')::darts.id_location_pair,
            ROW(13005, 'LOC-C5')::darts.id_location_pair
            ];

        CALL darts.dets_cleanup_eod_osr(3, now() - interval '7 days', results);

        --ASSERT cardinality(results) = 2, 'TEST 5 CALL 2 FAILED: expected 2 results but got ' || cardinality(results);
        --ASSERT results = expected, 'TEST 5 CALL 2 FAILED: results array does not match expected values';

        -- Check count
        IF cardinality(results) <> 2 THEN
            RAISE NOTICE 'TEST 5 CALL 2 FAILED: expected 2 results but got %', cardinality(results);
            RETURN;
        END IF;

        -- Check content
        IF results IS DISTINCT FROM expected THEN
            RAISE NOTICE 'TEST 5 CALL 2 FAILED: results array does not match expected values';
            RETURN;
        END IF;

        RAISE NOTICE 'TEST 5 CALL 2 PASSED';
    END $$;

---------------------------------------------------------------------------
-- TEST 6: ARM record linked to multiple OSR/DETS records, expect exception
---------------------------------------------------------------------------

CALL test_dets_cleanup.reset_test_data();

-- Valid ARM EOD
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (3501, 8980069, 2, 3, NULL, now(), now() - interval '8 days', -99, -99, FALSE, FALSE, FALSE);

-- Valid DETS EOD
INSERT INTO external_object_directory ( eod_id, med_id, ors_id, elt_id, external_location, created_ts, last_modified_ts, created_by, last_modified_by,
                                        update_retention, is_response_cleaned, is_dets)
VALUES
    (4501, 8980069, 2, 4, 'LOC-D1', now(), now(), -99, -99, FALSE, FALSE, TRUE),
    (4502, 8980069, 2, 4, 'LOC-D2', now(), now(), -99, -99, FALSE, FALSE, TRUE);

-- OSR referencing both DETS EODs but ARM EOD is associated with both
INSERT INTO object_state_record (osr_uuid, eod_id, arm_eod_id, dets_location)
VALUES
    (15001, 4501, 3501, 'LOC-D1'),
    (15002, 4502, 3501, 'LOC-D2');

DO $$
    DECLARE
        results darts.id_location_pair[];
    BEGIN
        RAISE NOTICE '------------------------------------';
        RAISE NOTICE '              TEST 6';
        RAISE NOTICE '------------------------------------';

        CALL darts.dets_cleanup_eod_osr(10, now() - interval '7 days', results);
        RAISE NOTICE 'TEST 6 FAILED: expected exception but procedure completed successfully.';
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'TEST 6 PASSED (exception raised as expected): %', SQLERRM;
    END $$;

----------------------------------------------------------------------
-- END OF TESTS
----------------------------------------------------------------------

DO $$
    BEGIN
        RAISE NOTICE 'ALL TESTS COMPLETED';
        RAISE NOTICE 'Cleaning up...';
    END $$;

----------------------------------------------------------------------
-- CLEAN UP
----------------------------------------------------------------------
RESET ALL;
--Drop schema avoiding using CASCADE
DROP PROCEDURE IF EXISTS test_dets_cleanup.reset_test_data;
DROP TABLE IF EXISTS test_dets_cleanup.external_object_directory;
DROP TABLE IF EXISTS test_dets_cleanup.object_state_record;
DROP SCHEMA IF EXISTS test_dets_cleanup;

SELECT name, setting, unit, SOURCE
FROM pg_settings
WHERE name IN ('search_path', 'max_parallel_workers_per_gather', 'work_mem', 'max_parallel_maintenance_workers')
ORDER BY name;