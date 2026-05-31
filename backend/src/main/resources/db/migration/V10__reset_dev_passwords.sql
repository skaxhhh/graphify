-- 개발 시드 비밀번호 복구 (password123)
UPDATE users
SET password_hash = '{noop}password123'
WHERE email IN ('demo@graphify.dev', 'newuser@graphify.dev');
