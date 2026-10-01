package com.gamemoyeo.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class RiotAccountTest {
    @Test
    void rejectsIncompleteIdentityAndFormatsRiotId() {
        assertThatThrownBy(() -> new RiotAccount("", "Player", "KR1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RiotAccount("puuid", null, "KR1")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new RiotAccount("puuid", "한글 이름", "KR1").riotId()).isEqualTo("한글 이름#KR1");
    }
}
