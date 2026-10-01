INSERT INTO game_option (game_id, option_type, code, display_name, sort_order, active)
SELECT g.id, 'REGION', r.code, r.display_name, r.sort_order, TRUE
FROM game g
CROSS JOIN (
    SELECT 'KR' AS code, '대한민국' AS display_name, 10 AS sort_order
    UNION ALL SELECT 'NA', '북미', 20
) r
WHERE g.slug = 'league-of-legends'
  AND NOT EXISTS (
    SELECT 1 FROM game_option o
    WHERE o.game_id = g.id AND o.option_type = 'REGION' AND o.code = r.code
  );

CREATE INDEX ix_meetup_game_region_status_id ON meetup (game_id, region_option_id, status, id);
