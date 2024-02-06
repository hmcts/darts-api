UPDATE darts.security_role
SET role_name    = 'TRANSLATION_QA',
    display_name = 'Translation QA'
WHERE role_name = 'LANGUAGE_SHOP_USER';
