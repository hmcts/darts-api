update darts.user_account ua1 set is_active = false where exists (
    select 1
    from darts.user_account ua2
    where ua2.user_email_address = ua1.user_email_address
    and ua2.is_active = true
    and ua1.is_active = true
    and (ua1.created_ts < ua2.created_ts or (ua1.created_ts = ua2.created_ts and ua1.usr_id < ua2.usr_id)));

CREATE UNIQUE INDEX user_account_user_email_address_unq ON darts.user_account (user_email_address) where is_active;
