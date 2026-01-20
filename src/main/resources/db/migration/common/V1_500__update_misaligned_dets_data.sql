update object_state_record
set flag_file_stored_in_arm = FALSE,
    date_file_stored_in_arm = NULL
where flag_file_stored_in_arm = TRUE
and arm_eod_id in
(
    select eod_id from external_object_directory eod
    where eod.ors_id <> 2
    and eod.eod_id = arm_eod_id
);