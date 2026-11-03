package com.gamemoyeo.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.reservation.application.MyMeetupsUseCase.Item;
import com.gamemoyeo.reservation.application.MyMeetupsUseCase.Filter;
import com.gamemoyeo.reservation.application.port.out.MyMeetupsPort;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class MyMeetupsServiceTest {
    private final MyMeetupsPort port = mock(MyMeetupsPort.class);
    private final Instant now = Instant.parse("2026-11-03T00:00:00Z");
    private final MyMeetupsService service = new MyMeetupsService(port, Clock.fixed(now, ZoneOffset.UTC));
    @Test
    void returnsOpaqueCompositeCursorAndCapsPageSize() {
        var first = new Item(7, 1, "Party", now, "OPEN", "JOINED", 2, 5, true, true, false, true);
        var second = new Item(8, 1, "Party", now, "OPEN", "JOINED", 2, 5, true, true, false, true);
        when(port.find(3, Filter.UPCOMING, now, null, null, 2)).thenReturn(List.of(first, second));
        var page = service.find(3, Filter.UPCOMING, null, 1);
        assertThat(page.items()).containsExactly(first);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isEqualTo(now + "~7");
        when(port.find(3, Filter.UPCOMING, now, now, 7L, 101)).thenReturn(List.of(second));
        assertThat(service.find(3, Filter.UPCOMING, page.nextCursor(), 500).hasNext()).isFalse();
    }
    @Test
    void rejectsMalformedCursorWithoutQueryingDatabase() {
        for (String cursor : List.of("bad", "2026-11-03T00:00:00Z~-1", "2026-11-03T00:00:00Z~1~2")) {
            assertThatThrownBy(() -> service.find(3, Filter.PAST, cursor, 20)).isInstanceOf(ApiException.class);
        }
        verifyNoInteractions(port);
    }
}
