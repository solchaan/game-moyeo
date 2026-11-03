package com.gamemoyeo.reservation.application.port.out;

import com.gamemoyeo.reservation.application.ReservationUseCase.ParticipationView;
import java.util.Optional;

public interface ReservationIdempotencyPort {
    Optional<ParticipationView> replay(long memberId, String operation, String key, String payload);
    void save(long memberId, String operation, String key, String payload, ParticipationView result);
}
