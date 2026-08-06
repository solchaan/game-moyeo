package com.gamemoyeo.meetup.application.port.out;

import com.gamemoyeo.meetup.application.MeetupBoardUseCase.MeetupCommand;
import com.gamemoyeo.meetup.application.MeetupBoardUseCase.MeetupView;
import java.util.List;

public interface MeetupBoardPort {

    MeetupView create(long ownerId, MeetupCommand command);

    MeetupView update(long meetupId, long ownerId, MeetupCommand command);

    void delete(long meetupId, long ownerId);

    MeetupView find(long meetupId);

    List<MeetupView> findAll(Long gameId, Long cursor, int limit);
}
