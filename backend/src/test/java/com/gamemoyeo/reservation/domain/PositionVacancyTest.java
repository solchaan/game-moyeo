package com.gamemoyeo.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class PositionVacancyTest {
    @Test
    void boundsRoleVacancyByTotalRemainingAndNeverGoesNegative() {
        assertThat(PositionVacancy.calculate(1, 5, 2, true).remainingCapacity()).isEqualTo(2);
        assertThat(PositionVacancy.calculate(3, 1, 2, true).remainingCapacity()).isZero();
        assertThat(PositionVacancy.calculate(3, 1, 2, true).status()).isEqualTo("FILLED");
    }

    @Test
    void unspecifiedTargetsDescribeOccupancyWithoutInventingCapacity() {
        var empty = PositionVacancy.calculate(0, null, 4, true);
        assertThat(empty.capacity()).isNull();
        assertThat(empty.remainingCapacity()).isNull();
        assertThat(empty.status()).isEqualTo("EMPTY");
        assertThat(PositionVacancy.calculate(2, null, 2, true).status()).isEqualTo("OCCUPIED");
    }

    @Test
    void closedMeetupsNeverAdvertiseOpenPositions() {
        assertThat(PositionVacancy.calculate(0, 2, 3, false).status()).isEqualTo("CLOSED");
        assertThat(PositionVacancy.calculate(0, null, 0, false).status()).isEqualTo("CLOSED");
    }
}
