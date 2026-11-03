package com.gamemoyeo.member.application.port.out;

import com.gamemoyeo.member.application.CommunityUseCase.Companion;
import com.gamemoyeo.member.application.CommunityUseCase.Blocked;
import com.gamemoyeo.member.application.CommunityUseCase.Report;
import java.util.List;

public interface CommunityPort {
    List<Companion> companions(long member, Long cursor, int limit);
    List<Blocked> blocks(long member, Long cursor, int limit);
    void block(long member, long target, boolean blocked);
    boolean sharedPast(long member, long target, long meetup);
    void endorse(long member, long target, long meetup, boolean endorsed);
    void report(long member, long target, long meetup, String reason, String details);
    List<Report> reports(Long cursor, int limit);
    void resolve(long admin, long report, String status, String resolution);
    void guardAdmission(long meetup, long member);
}
