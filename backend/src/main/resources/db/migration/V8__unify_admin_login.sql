INSERT INTO member_credential (
    member_id,
    username,
    password_hash,
    failed_attempts,
    locked_until,
    password_changed_at,
    last_login_at,
    version
)
SELECT admin.member_id,
       admin.username,
       admin.password_hash,
       admin.failed_attempts,
       admin.locked_until,
       admin.password_changed_at,
       admin.last_login_at,
       admin.version
FROM admin_credential admin
WHERE NOT EXISTS (
    SELECT 1
    FROM member_credential member_login
    WHERE member_login.member_id = admin.member_id
       OR member_login.username = admin.username
);
