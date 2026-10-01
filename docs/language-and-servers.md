# Language and League server selection

The public site supports Korean (`ko`, displayed as KOR) and English (`en`, displayed as EN).
Language is an interface preference stored as `gamemoyeo:language`. It does not change the selected server,
user-written titles/descriptions, or the timezone. Dates use the selected language and the viewer's local timezone.
Admin catalog editing remains in Korean. League option labels are translated from stable option codes.

League party browsing supports `server=KR`, `server=NA`, and `server=ALL` URL parameters. KR is the initial default.
The latest server choice is also retained in `gamemoyeo:league-server` for future visits.
The URL overrides the saved preference. Other games ignore this League-specific filter.

`GET /api/v1/meetups` accepts an optional positive `regionOptionId` query parameter in addition to `gameId`,
`cursor`, and `size`. A region filter requires `gameId` and an active REGION option belonging to that game;
otherwise the server returns a 400 Problem Details response. Filtering happens before cursor pagination.
Omitting the filter preserves the existing all-server query, including records with a null region.

Party creation continues using the existing `regionOptionId` payload field. The selected League server is carried
through login, and a server is required in the League creation form. Editing does not silently overwrite a saved
server. Game changes clear incompatible options. Riot API access is not required.

Migration V10 adds missing KR/NA League region options and an index on `(game_id, region_option_id, status, id)`.
It does not change existing options, assign a server to legacy parties, or remove other games.
The game seed includes NA for installations where League is registered after migrations have run.
