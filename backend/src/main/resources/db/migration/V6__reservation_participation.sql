ALTER TABLE meetup_session
    ADD COLUMN closed_reason VARCHAR(30) NULL AFTER status;

INSERT INTO reservation (session_id, member_id, party_size, status)
SELECT session.id, meetup.owner_id, 1, 'CONFIRMED'
FROM meetup_session session
JOIN meetup ON meetup.id = session.meetup_id
LEFT JOIN reservation existing
    ON existing.session_id = session.id
    AND existing.member_id = meetup.owner_id
WHERE existing.id IS NULL
  AND meetup.status <> 'DELETED';

UPDATE meetup_session session
SET session.reserved_count = LEAST(
    session.capacity,
    (SELECT COALESCE(SUM(reservation.party_size), 0)
     FROM reservation
     WHERE reservation.session_id = session.id
       AND reservation.status = 'CONFIRMED')
);

UPDATE meetup_session session
JOIN meetup ON meetup.id = session.meetup_id
SET session.status = 'CLOSED',
    session.closed_reason = 'FULL',
    meetup.status = 'CLOSED'
WHERE meetup.status <> 'DELETED'
  AND session.reserved_count >= session.capacity;

UPDATE meetup_session session
JOIN meetup ON meetup.id = session.meetup_id
SET session.status = 'CLOSED',
    session.closed_reason = 'START_IMMINENT',
    meetup.status = 'CLOSED'
WHERE meetup.status = 'OPEN'
  AND session.status = 'OPEN'
  AND (
      session.starts_at <= DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 10 MINUTE)
      OR (meetup.recruitment_deadline IS NOT NULL
          AND meetup.recruitment_deadline <= UTC_TIMESTAMP(6))
  );
