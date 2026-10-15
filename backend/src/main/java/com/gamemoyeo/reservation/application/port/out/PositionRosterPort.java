package com.gamemoyeo.reservation.application.port.out;

import com.gamemoyeo.member.domain.PositionPreferences;
import java.util.List;

public interface PositionRosterPort {
    List<MemberPosition> confirmedPositions(long meetupId);

    record MemberPosition(long memberId, PositionPreferences positions) { }
}
