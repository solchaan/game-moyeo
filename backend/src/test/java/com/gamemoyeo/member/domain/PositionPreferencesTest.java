package com.gamemoyeo.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.gamemoyeo.member.domain.PositionPreferences.Position.TOP;
import static com.gamemoyeo.member.domain.PositionPreferences.Position.MID;
import static com.gamemoyeo.member.domain.PositionPreferences.Position.SUPPORT;
import static com.gamemoyeo.member.domain.PositionPreferences.Position.ANY;
import org.junit.jupiter.api.Test;

class PositionPreferencesTest {
    @Test
    void allowsUnsetAnyAndDifferentPositions() {
        assertThat(PositionPreferences.empty().primaryPosition()).isNull();
        assertThat(new PositionPreferences(ANY, null).primaryPosition()).isEqualTo(ANY);
        assertThat(new PositionPreferences(MID, SUPPORT).secondaryPosition()).isEqualTo(SUPPORT);
    }
    @Test
    void rejectsDuplicateAndSecondaryWithoutPrimary() {
        assertThatThrownBy(() -> new PositionPreferences(TOP, TOP)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PositionPreferences(null, TOP)).isInstanceOf(IllegalArgumentException.class);
    }
}
