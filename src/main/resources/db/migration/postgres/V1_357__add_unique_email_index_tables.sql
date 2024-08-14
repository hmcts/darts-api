update darts.user_account ua1 set is_active = false where ua1.usr_id not in
(select ua2.usr_id
from darts.user_account ua2
where ua2.user_email_address = ua1.user_email_address
and ua2.is_active = true
order by last_modified_ts desc limit 1);

CREATE UNIQUE INDEX user_account_user_email_address_unq ON darts.user_account (user_email_address) where is_active;


