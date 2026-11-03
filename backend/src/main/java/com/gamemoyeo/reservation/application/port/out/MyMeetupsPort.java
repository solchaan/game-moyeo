package com.gamemoyeo.reservation.application.port.out;

import com.gamemoyeo.reservation.application.MyMeetupsUseCase.Filter;
import com.gamemoyeo.reservation.application.MyMeetupsUseCase.Item;
import java.time.Instant;
import java.util.List;

public interface MyMeetupsPort {
    List<Item> find(long memberId, Filter filter, Instant now, Instant cursorTime, Long cursorId, int limit);
}
