package com.gamemoyeo.reservation.application;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.game.application.GameCatalogUseCase;
import com.gamemoyeo.member.application.port.in.GameProfileUseCase;
import com.gamemoyeo.reservation.application.port.out.RecommendationPort;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecommendationService implements RecommendationUseCase {
    private final RecommendationPort port;
    private final GameProfileUseCase profiles;
    private final GameCatalogUseCase games;
    public RecommendationService(RecommendationPort port, GameProfileUseCase profiles, GameCatalogUseCase games) {
        this.port = port;
        this.profiles = profiles;
        this.games = games;
    }
    @Override
    public Page find(long memberId, Query query) {
        var game = games.findGame(query.gameId());
        if (query.from() != null && query.to() != null && !query.from().isBefore(query.to())) { throw invalid(); }
        Integer tier = option(game, query.tierId(), GameCatalogUseCase.OptionType.TIER);
        option(game, query.regionId(), GameCatalogUseCase.OptionType.REGION);
        option(game, query.modeId(), GameCatalogUseCase.OptionType.MODE);
        if (query.playStyle() != null && !List.of("CASUAL","COMPETITIVE","PRACTICE","BEGINNER","SOCIAL").contains(query.playStyle())) { throw invalid(); }
        Integer score = null;
        Instant time = null;
        Long id = null;
        if (query.cursor() != null) {
            try {
                var parts = query.cursor().split("~", -1);
                if (parts.length != 3) { throw invalid(); }
                score = Integer.valueOf(parts[0]);
                time = Instant.parse(parts[1]);
                id = Long.valueOf(parts[2]);
                if (id < 1 || !List.of(0,60,100).contains(score)) { throw invalid(); }
            } catch (RuntimeException error) { throw invalid(); }
        }
        int size = Math.max(1,Math.min(100,query.size()));
        var rows = port.find(memberId, query, ("league-of-legends".equals(game.game().slug()) ? profiles.find(memberId,query.gameId()) : com.gamemoyeo.member.domain.PositionPreferences.empty()), tier, Instant.now(), score,time,id,size+1);
        boolean more = rows.size()>size;
        var items = List.copyOf(rows.subList(0,Math.min(rows.size(),size)));
        String cursor = more ? items.getLast().score()+"~"+items.getLast().startsAt()+"~"+items.getLast().id() : null;
        return new Page(items,cursor,more);
    }
    private Integer option(GameCatalogUseCase.GameDetailView game, Long id, GameCatalogUseCase.OptionType type) {
        if (id == null) { return null; }
        return game.options().stream().filter(option -> option.id()==id && option.type()==type && option.active())
            .map(GameCatalogUseCase.OptionView::sortOrder).findFirst().orElseThrow(this::invalid);
    }
    private ApiException invalid() { return new ApiException(HttpStatus.BAD_REQUEST,"INVALID_RECOMMENDATION_FILTER","Invalid matching filter or cursor."); }
}
