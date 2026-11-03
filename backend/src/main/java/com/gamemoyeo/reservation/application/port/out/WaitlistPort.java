package com.gamemoyeo.reservation.application.port.out;

import com.gamemoyeo.reservation.application.WaitlistUseCase.View;
import java.util.List;

public interface WaitlistPort {
    View find(long meetupId, long memberId, boolean canJoin);
    void join(long meetupId, long memberId);
    void cancel(long meetupId, long memberId);
    void advance(long meetupId);
    void guardAdmission(long meetupId, long memberId);
    void accepted(long meetupId, long memberId);
    List<Long> pendingMeetups();
}
