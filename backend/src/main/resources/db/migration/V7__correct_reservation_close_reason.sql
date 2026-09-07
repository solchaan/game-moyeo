UPDATE meetup_session session
JOIN meetup ON meetup.id = session.meetup_id
SET session.closed_reason = 'DEADLINE_REACHED'
WHERE meetup.status = 'CLOSED'
  AND session.status = 'CLOSED'
  AND session.closed_reason = 'START_IMMINENT'
  AND session.starts_at > DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 10 MINUTE)
  AND meetup.recruitment_deadline IS NOT NULL
  AND meetup.recruitment_deadline <= UTC_TIMESTAMP(6);
