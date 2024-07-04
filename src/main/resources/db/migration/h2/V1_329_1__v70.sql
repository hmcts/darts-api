--    add 3 unique constraints, one defence, defendant and prosecutor to avoid duplicate names on a case
CREATE UNIQUE INDEX dfc_cas_id_defence_name_unq ON darts.defence (cas_id, defence_name);

CREATE UNIQUE INDEX dfd_cas_id_defendant_name_unq ON darts.defendant (cas_id, defendant_name);

CREATE UNIQUE INDEX prn_cas_id_prosecutor_name_unq ON darts.prosecutor (cas_id, prosecutor_name);
