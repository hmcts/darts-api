UPDATE user_account
SET user_email_address = 'darts.user@hmcts.net', is_active = true

WHERE usr_id = 101
AND user_email_address = 'darts.global.user@hmcts.net';