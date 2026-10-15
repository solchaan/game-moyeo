package com.gamemoyeo.member.application.port.in;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.member.domain.PositionPreferences;
import com.gamemoyeo.member.domain.PositionPreferences.Position;
import org.springframework.http.HttpStatus;

public record PositionSelection(Position primaryPosition, Position secondaryPosition) {
    public PositionPreferences toPreferences() {
        try {
            return new PositionPreferences(primaryPosition, secondaryPosition);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_POSITIONS", exception.getMessage(),
                java.util.Map.of("secondaryPosition", exception.getMessage()));
        }
    }
    public static PositionSelection from(PositionPreferences value) {
        return new PositionSelection(value.primaryPosition(), value.secondaryPosition());
    }
}
