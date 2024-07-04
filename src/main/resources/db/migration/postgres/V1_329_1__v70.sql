--    add 3 unique constraints, one defence, defendant and prosecutor to avoid duplicate names on a case
CREATE UNIQUE INDEX dfc_cas_id_defence_name_unq ON defence (cas_id, defence_name);
ALTER TABLE defence
    ADD UNIQUE USING INDEX dfc_cas_id_defence_name_unq;

CREATE UNIQUE INDEX dfd_cas_id_defendant_name_unq ON defendant (cas_id, defendant_name);
ALTER TABLE defendant
    ADD UNIQUE USING INDEX dfd_cas_id_defendant_name_unq;

CREATE UNIQUE INDEX prn_cas_id_prosecutor_name_unq ON prosecutor (cas_id, prosecutor_name);
ALTER TABLE prosecutor
    ADD UNIQUE USING INDEX prn_cas_id_prosecutor_name_unq;
