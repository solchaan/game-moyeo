-- Capture existing hosts' current preferences once. Later profile edits do not change reservations.
UPDATE reservation r
JOIN meetup_session s ON s.id = r.session_id
JOIN meetup m ON m.id = s.meetup_id AND m.owner_id = r.member_id
JOIN game g ON g.id = m.game_id AND g.slug = 'league-of-legends'
JOIN game_option o ON o.id = m.map_option_id AND o.game_id = m.game_id
    AND o.option_type = 'MAP' AND o.code = 'SUMMONERS_RIFT' AND o.active = TRUE
JOIN member_game_profile p ON p.member_id = m.owner_id AND p.game_id = m.game_id
SET r.primary_position = p.primary_position, r.secondary_position = p.secondary_position
WHERE r.status = 'CONFIRMED' AND m.status <> 'DELETED'
    AND r.primary_position IS NULL AND r.secondary_position IS NULL;
