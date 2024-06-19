-- Instructions
--
-- This script is designed to delete cases by case number.
-- It does this by removing all direct links to cas_id 
-- and all direct links to the associated hearings, using hea_id.
-- 
-- It DOES NOT remove the events, transcriptions and media associated with the case.
-- This is because it's possible that these could link to other cases/hearings.

DO $$
<<first_block>>
DECLARE
	case_numbers text[] := ARRAY['CASE1', 'CASE2'];

	case_num text;
	case_id int;
	hearing_ids int[];
	hearing_id int;
	
	deleted_count int;
BEGIN
	RAISE NOTICE 'Deleting cases: %', case_numbers;

   	FOREACH case_num IN ARRAY case_numbers LOOP
		
		SELECT cc.cas_id
		INTO case_id
		FROM darts.court_case cc
		WHERE cc.case_number = case_num;
		
		IF case_id IS NULL THEN
			RAISE NOTICE 'Case number not found: %', case_num;
			CONTINUE;
		END IF;
		
		hearing_ids := ARRAY(
			SELECT hea_id
			FROM darts.hearing
			WHERE cas_id = case_id
		);
	
		RAISE NOTICE 'CASE_START::Deleting case number: %, with case ID: %, with hearings: %', case_num, case_id, hearing_ids;
		
		DELETE FROM darts.case_judge_ae j
		WHERE j.cas_id = case_id;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;		
		RAISE NOTICE '  Deleted % row(s) from case_judge_ae', deleted_count;
		
		DELETE FROM darts.case_transcription_ae tr
		WHERE tr.cas_id = case_id;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;		
		RAISE NOTICE '  Deleted % row(s) from case_transcription_ae', deleted_count;
		
		DELETE FROM darts.defence d
		WHERE d.cas_id = case_id;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;		
		RAISE NOTICE '  Deleted % row(s) from defence', deleted_count;
		
		DELETE FROM darts.defendant d
		WHERE d.cas_id = case_id;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;		
		RAISE NOTICE '  Deleted % row(s) from defendant', deleted_count;
		
		DELETE FROM darts.prosecutor pr
		WHERE pr.cas_id = case_id;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;		
		RAISE NOTICE '  Deleted % row(s) from prosecutor', deleted_count;
		
		DELETE FROM darts.event_linked_case elc
		WHERE elc.cas_id = case_id;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;		
		RAISE NOTICE '  Deleted % row(s) from event_linked_case', deleted_count;
		
		DELETE FROM darts.media_linked_case mlc
		WHERE mlc.cas_id = case_id;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;		
		RAISE NOTICE '  Deleted % row(s) from media_linked_case', deleted_count;
		
		DELETE FROM darts.case_document cd
		WHERE cd.cas_id = case_id;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;		
		RAISE NOTICE '  Deleted % row(s) from case_document', deleted_count;
		
		DELETE FROM darts.case_overflow co
		WHERE co.cas_id = case_id;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;		
		RAISE NOTICE '  Deleted % row(s) from case_overflow', deleted_count;
		
		DELETE FROM darts.case_management_retention cmr
		WHERE cmr.cas_id = case_id;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;		
		RAISE NOTICE '  Deleted % row(s) from case_management_retention', deleted_count;
		
		DELETE FROM darts.case_retention cr
		WHERE cr.cas_id = case_id;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;		
		RAISE NOTICE '  Deleted % row(s) from case_retention', deleted_count;
		
		DELETE FROM darts.audit
		WHERE cas_id = case_id;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;		
		RAISE NOTICE '  Deleted % row(s) from audit', deleted_count;
		
		DELETE FROM darts.notification
		WHERE cas_id = case_id;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;		
		RAISE NOTICE '  Deleted % row(s) from notification', deleted_count;
		
		FOREACH hearing_id IN ARRAY hearing_ids LOOP
		
			RAISE NOTICE '  HEARING_START::Deleting hearing with ID: %', hearing_id;
		
			DELETE FROM darts.hearing_annotation_ae
			WHERE hea_id = hearing_id;
			GET DIAGNOSTICS deleted_count = ROW_COUNT;		
			RAISE NOTICE '    Deleted % row(s) from hearing_annotation_ae', deleted_count;
			
			DELETE FROM darts.hearing_event_ae
			WHERE hea_id = hearing_id;
			GET DIAGNOSTICS deleted_count = ROW_COUNT;		
			RAISE NOTICE '    Deleted % row(s) from hearing_event_ae', deleted_count;
			
			DELETE FROM darts.hearing_judge_ae
			WHERE hea_id = hearing_id;
			GET DIAGNOSTICS deleted_count = ROW_COUNT;		
			RAISE NOTICE '    Deleted % row(s) from hearing_judge_ae', deleted_count;
			
			DELETE FROM darts.hearing_media_ae
			WHERE hea_id = hearing_id;
			GET DIAGNOSTICS deleted_count = ROW_COUNT;		
			RAISE NOTICE '    Deleted % row(s) from hearing_media_ae', deleted_count;

			DELETE FROM darts.hearing_transcription_ae
			WHERE hea_id = hearing_id;
			GET DIAGNOSTICS deleted_count = ROW_COUNT;		
			RAISE NOTICE '    Deleted % row(s) from hearing_transcription_ae', deleted_count;
			
			DELETE FROM darts.transient_object_directory tod
			WHERE tod.trm_id IN (
				SELECT tm.trm_id
				FROM darts.transformed_media tm
				WHERE tm.mer_id IN (
					SELECT mer_id
					FROM darts.media_request
					WHERE hea_id = hearing_id
				)
			);
			GET DIAGNOSTICS deleted_count = ROW_COUNT;		
			RAISE NOTICE '    Deleted % row(s) from transient_object_directory', deleted_count;
			
			DELETE FROM darts.transformed_media tm
			WHERE tm.mer_id IN (
				SELECT mer_id
				FROM darts.media_request
				WHERE hea_id = hearing_id
			);
			GET DIAGNOSTICS deleted_count = ROW_COUNT;		
			RAISE NOTICE '    Deleted % row(s) from transformed_media', deleted_count;
			
			DELETE FROM darts.media_request
			WHERE hea_id = hearing_id;
			GET DIAGNOSTICS deleted_count = ROW_COUNT;		
			RAISE NOTICE '    Deleted % row(s) from media_request', deleted_count;
			
			DELETE FROM darts.hearing
			WHERE hea_id = hearing_id;
			RAISE NOTICE '  HEARING_END::Deleted hearing with ID: %', hearing_id;
			
		END LOOP;
		
		DELETE FROM darts.court_case
		WHERE cas_id = case_id;
		RAISE NOTICE 'CASE_END::Deleted Case with ID: %', case_id;
		
   	END LOOP;
END first_block $$;
