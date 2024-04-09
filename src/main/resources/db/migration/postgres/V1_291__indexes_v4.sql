DROP INDEX cth_cn_idx;
DROP INDEX ctr_cn_idx;
DROP INDEX dfc_dn_idx;
DROP INDEX dfd_dn_idx;
DROP INDEX jud_jn_idx;
DROP INDEX prn_pn_idx;

CREATE INDEX cth_cn_idx         ON COURTHOUSE(UPPER(courthouse_name)) ;
CREATE INDEX ctr_cn_idx         ON COURTROOM(UPPER(courtroom_name))   ;
CREATE INDEX dfc_dn_idx         ON DEFENCE(UPPER(defence_name))       ;
CREATE INDEX dfd_dn_idx         ON DEFENDANT(UPPER(defendant_name))   ;
CREATE INDEX jud_jn_idx         ON JUDGE(UPPER(judge_name))           ;
CREATE INDEX prn_pn_idx         ON PROSECUTOR(UPPER(prosecutor_name)) ;
