package com.gamemoyeo.meetup.application;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.meetup.application.MeetupBoardUseCase.MeetupCommand;
import com.gamemoyeo.meetup.application.port.out.MeetupTemplatePort;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MeetupTemplateService implements MeetupTemplateUseCase {
    private final MeetupTemplatePort port;
    private final MeetupBoardUseCase board;
    public MeetupTemplateService(MeetupTemplatePort port, MeetupBoardUseCase board) {
        this.port = port;
        this.board = board;
    }
    @Override
    public long save(long memberId, long source) {
        var m = board.find(source);
        if (m.ownerId()!=memberId) { throw new ApiException(HttpStatus.FORBIDDEN,"MEETUP_OWNER_REQUIRED","Only your own meetup can be saved as a template."); }
        return port.save(memberId,source,new MeetupCommand(m.gameId(),m.modeOptionId(),m.platformOptionId(),m.regionOptionId(),
            m.minimumTierOptionId(),m.maximumTierOptionId(),m.title(),m.description(),m.playStyle(),m.voiceChatPolicy(),m.approvalType(),
            null,m.startsAt(),m.capacity(),m.roleRequirements(),m.mapOptionId()));
    }
    @Override
    @Transactional(readOnly=true)
    public Page find(long memberId, Long cursor, int size) {
        int limit = Math.max(1,Math.min(size,100));
        var rows = port.find(memberId,cursor,limit+1);
        boolean more = rows.size()>limit;
        var items = List.copyOf(rows.subList(0,Math.min(rows.size(),limit)));
        return new Page(items,more?items.getLast().id():null,more);
    }
    @Override
    public void delete(long memberId, long templateId) { port.delete(memberId,templateId); }
    @Override
    public Created create(long memberId, long templateId, Instant startsAt, int weeks, String timeZone, String key) {
        if (weeks<1 || weeks>8 || startsAt==null || key==null || key.isBlank() || key.length()>100) { throw invalid(); }
        ZoneId zone;
        try { zone = ZoneId.of(timeZone); }
        catch (RuntimeException error) { throw invalid(); }
        var m = port.lock(memberId,templateId);
        String payload = startsAt+":"+weeks+":"+timeZone;
        var previous = port.replay(memberId,templateId,key,payload);
        if (previous.isPresent()) { return previous.get(); }
        var ids = new ArrayList<Long>();
        var local = startsAt.atZone(zone);
        for (int week=0;week<weeks;week++) {
            var start = local.plusWeeks(week).toInstant();
            ids.add(board.create(memberId,new MeetupCommand(m.gameId(),m.modeOptionId(),m.platformOptionId(),m.regionOptionId(),
                m.minimumTierOptionId(),m.maximumTierOptionId(),m.title(),m.description(),m.playStyle(),m.voiceChatPolicy(),m.approvalType(),
                null,start,m.capacity(),m.roleRequirements(),m.mapOptionId())).id());
        }
        var created = new Created(List.copyOf(ids));
        port.remember(memberId,templateId,key,payload,created);
        return created;
    }
    private ApiException invalid() { return new ApiException(HttpStatus.BAD_REQUEST,"INVALID_REPEAT_SCHEDULE","Choose a valid start, time zone and 1 to 8 occurrences."); }
}
