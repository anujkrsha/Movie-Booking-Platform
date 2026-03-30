-- Resets the users table between integration tests.
-- Referenced via @Sql(scripts = "classpath:db/reset.sql", ...)
TRUNCATE TABLE users RESTART IDENTITY CASCADE;
