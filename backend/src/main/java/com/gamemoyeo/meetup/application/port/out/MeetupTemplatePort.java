package com.gamemoyeo.meetup.application.port.out;

import com.gamemoyeo.meetup.application.MeetupBoardUseCase.MeetupCommand;
import com.gamemoyeo.meetup.application.MeetupTemplateUseCase.Item;
import com.gamemoyeo.meetup.application.MeetupTemplateUseCase.Created;
import java.util.List;
import java.util.Optional;

public interface MeetupTemplatePort {
    long save(long memberId, long source, MeetupCommand command);
    List<Item> find(long memberId, Long cursor, int limit);
    void delete(long memberId, long templateId);
    MeetupCommand lock(long memberId, long templateId);
    Optional<Created> replay(long memberId, long templateId, String key, String payload);
    void remember(long memberId, long templateId, String key, String payload, Created result);
}
