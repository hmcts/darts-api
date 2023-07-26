-- additional unique multi-column indexes and constraints

--,UNIQUE (cth_id,courtroom_name)
CREATE UNIQUE INDEX ctr_chr_crn_unq ON courtroom( cth_id, courtroom_name);
ALTER TABLE courtroom ADD UNIQUE USING INDEX ctr_chr_crn_unq;

--,UNIQUE(cas_id,ctr_id,c_hearing_date)
CREATE UNIQUE INDEX hea_cas_ctr_hd_unq ON hearing( cas_id, ctr_id,hearing_date);
ALTER TABLE hearing ADD UNIQUE USING INDEX hea_cas_ctr_hd_unq;

--,UNIQUE(cth_id, case_number)
CREATE UNIQUE INDEX cas_case_number_cth_id_unq ON court_case(case_number,cth_id);
ALTER TABLE court_case ADD UNIQUE USING INDEX cas_case_number_cth_id_unq;
