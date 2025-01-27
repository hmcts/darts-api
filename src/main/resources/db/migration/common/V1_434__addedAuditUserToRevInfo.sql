alter table revinfo
    add column audit_user bigint,
    add foreign key (audit_user) references user_account (usr_id);