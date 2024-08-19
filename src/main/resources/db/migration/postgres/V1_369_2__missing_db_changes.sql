DROP INDEX user_account_user_email_address_unq;
CREATE UNIQUE INDEX user_account_user_email_address_unq ON user_account (upper(user_email_address)) where is_active;
