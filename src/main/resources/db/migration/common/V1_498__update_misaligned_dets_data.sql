update object_state_record osr
set osr.flag_file_stored_in_arm = FALSE,
    osr.date_file_stored_in_arm = NULL
where osr.flag_file_stored_in_arm = TRUE
and osr.arm_eod_id in
(
    select eod_id from external_object_directory
    where ors_id <> 2
    and eod_id = osr.arm_eod_id
);