UPDATE user_account
SET user_email_address = 'system_DailyListHousekeeping@hmcts.net'
WHERE usr_id = -1
  AND user_name = 'system_DailyListHousekeeping';

UPDATE user_account
SET user_email_address = 'system_ProcessDailyList@hmcts.net'
WHERE usr_id = -2
  AND user_name = 'system_ProcessDailyList';

