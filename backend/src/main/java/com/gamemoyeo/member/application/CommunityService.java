package com.gamemoyeo.member.application;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.member.application.port.out.CommunityPort;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommunityService implements CommunityUseCase {
    private final CommunityPort port;
    public CommunityService(CommunityPort port) { this.port = port; }
    @Override
    @Transactional(readOnly=true)
    public CompanionPage companions(long member, Long cursor, int size) {
        int limit = limit(size);
        var rows = port.companions(member,cursor,limit+1);
        var items = List.copyOf(rows.subList(0,Math.min(limit,rows.size())));
        return new CompanionPage(items,rows.size()>limit?items.getLast().id():null,rows.size()>limit);
    }
    @Override
    @Transactional(readOnly=true)
    public BlockPage blocks(long member, Long cursor, int size) {
        int limit = limit(size);
        var rows = port.blocks(member,cursor,limit+1);
        var items = List.copyOf(rows.subList(0,Math.min(limit,rows.size())));
        return new BlockPage(items,rows.size()>limit?items.getLast().id():null,rows.size()>limit);
    }
    @Override
    public void block(long member, long target, boolean blocked) {
        if (member==target) { throw invalid(); }
        port.block(member,target,blocked);
    }
    @Override
    public void endorse(long member, long target, long meetup, boolean endorsed) {
        requireShared(member,target,meetup);
        port.endorse(member,target,meetup,endorsed);
    }
    @Override
    public void report(long member, long target, long meetup, String reason, String details) {
        requireShared(member,target,meetup);
        if (reason==null || !List.of("ABUSE","SPAM","NO_SHOW","OTHER").contains(reason)
            || details==null || details.isBlank() || details.length()>2000) { throw invalid(); }
        port.report(member,target,meetup,reason,details.strip());
    }
    @Override
    @Transactional(readOnly=true)
    public ReportPage reports(Long cursor, int size) {
        int limit = limit(size);
        var rows = port.reports(cursor,limit+1);
        var items = List.copyOf(rows.subList(0,Math.min(limit,rows.size())));
        return new ReportPage(items,rows.size()>limit?items.getLast().id():null,rows.size()>limit);
    }
    @Override
    public void resolve(long admin, long report, String status, String resolution) {
        if (status==null || !List.of("RESOLVED","DISMISSED").contains(status)
            || resolution==null || resolution.isBlank() || resolution.length()>2000) { throw invalid(); }
        port.resolve(admin,report,status,resolution.strip());
    }
    @Override
    public void guardAdmission(long meetup, long member) { port.guardAdmission(meetup,member); }
    private void requireShared(long member,long target,long meetup) {
        if (member==target || !port.sharedPast(member,target,meetup)) {
            throw new ApiException(HttpStatus.FORBIDDEN,"SHARED_MEETUP_REQUIRED","Only participants of a shared past meetup can leave feedback.");
        }
    }
    private int limit(int size) { return Math.max(1,Math.min(size,100)); }
    private ApiException invalid() { return new ApiException(HttpStatus.BAD_REQUEST,"INVALID_COMMUNITY_ACTION","Check the target, reason and details."); }
}
