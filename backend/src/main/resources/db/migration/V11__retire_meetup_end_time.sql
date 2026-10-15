-- End time is no longer part of the meetup API or persistence model.
-- Keep historical values for compatibility with the previous app during rollback.
-- A later contract migration can drop the unused column after the rollback window.
ALTER TABLE meetup_session MODIFY COLUMN ends_at DATETIME(6) NULL DEFAULT NULL;
