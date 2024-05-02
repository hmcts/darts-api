CREATE INDEX usr_upea_idx       ON USER_ACCOUNT(UPPER(user_email_address));
CREATE INDEX usr_ag_idx         ON USER_ACCOUNT(account_guid)             ;