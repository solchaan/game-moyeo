package com.gamemoyeo.member.application;

import java.util.List;

public interface CommunityUseCase {
    CompanionPage companions(long memberId, Long cursor, int size);
    BlockPage blocks(long memberId, Long cursor, int size);
    void block(long memberId, long target, boolean blocked);
    void endorse(long memberId, long target, long meetup, boolean endorsed);
    void report(long memberId, long target, long meetup, String reason, String details);
    ReportPage reports(Long cursor, int size);
    void resolve(long admin, long report, String status, String resolution);
    void guardAdmission(long meetup, long member);
    @io.swagger.v3.oas.annotations.media.Schema(name = "Companion")
    record Companion(long id, long meetupId, String meetupTitle, long memberId, String nickname, boolean blocked, boolean endorsed) { }
    @io.swagger.v3.oas.annotations.media.Schema(name = "CompanionPage")
    record CompanionPage(List<Companion> items, @io.swagger.v3.oas.annotations.media.Schema(nullable = true) Long nextCursor, boolean hasNext) { }
    @io.swagger.v3.oas.annotations.media.Schema(name = "BlockedMember")
    record Blocked(long id, String nickname) { }
    @io.swagger.v3.oas.annotations.media.Schema(name = "BlockPage")
    record BlockPage(List<Blocked> items, @io.swagger.v3.oas.annotations.media.Schema(nullable = true) Long nextCursor, boolean hasNext) { }
    @io.swagger.v3.oas.annotations.media.Schema(name = "MemberReport")
    record Report(long id, long reporterId, long targetMemberId, long meetupId, String reason, String details, String status, @io.swagger.v3.oas.annotations.media.Schema(nullable = true) String resolution) { }
    @io.swagger.v3.oas.annotations.media.Schema(name = "ReportPage")
    record ReportPage(List<Report> items, @io.swagger.v3.oas.annotations.media.Schema(nullable = true) Long nextCursor, boolean hasNext) { }
}
