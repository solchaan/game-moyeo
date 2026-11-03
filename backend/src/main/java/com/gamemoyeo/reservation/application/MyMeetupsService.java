package com.gamemoyeo.reservation.application;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.reservation.application.port.out.MyMeetupsPort;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MyMeetupsService implements MyMeetupsUseCase {
    private final MyMeetupsPort port;
    private final Clock clock;
    @Autowired
    public MyMeetupsService(MyMeetupsPort port) { this(port, Clock.systemUTC()); }
    MyMeetupsService(MyMeetupsPort port, Clock clock) { this.port = port;
        this.clock = clock; }

    @Override
    public Page find(long memberId, Filter filter, String cursor, int size) {
        Instant time = null;
        Long id = null;
        if (cursor != null) {
            try {
                String[] parts = cursor.split("~", -1);
                if (parts.length != 2) { throw new IllegalArgumentException(); }
                time = Instant.parse(parts[0]);
                id = Long.valueOf(parts[1]);
                if (id < 1) { throw new IllegalArgumentException(); }
            } catch (RuntimeException error) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "Invalid meetup cursor.");
            }
        }
        int limit = Math.min(Math.max(size, 1), 100);
        var values = port.find(memberId, filter, clock.instant(), time, id, limit + 1);
        boolean more = values.size() > limit;
        var items = java.util.List.copyOf(values.subList(0, Math.min(values.size(), limit)));
        String next = more ? items.get(items.size() - 1).startsAt() + "~" + items.get(items.size() - 1).id() : null;
        return new Page(items, next, more);
    }

}
