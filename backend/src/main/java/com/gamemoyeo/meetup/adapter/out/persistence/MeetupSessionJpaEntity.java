package com.gamemoyeo.meetup.adapter.out.persistence;

import com.gamemoyeo.meetup.application.MeetupBoardUseCase.MeetupCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "meetup_session")
class MeetupSessionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meetup_id", nullable = false, unique = true)
    MeetupJpaEntity meetup;
    @Column(name = "starts_at", nullable = false)
    Instant startsAt;
    @Column(name = "ends_at", nullable = false)
    Instant endsAt;
    @Column(nullable = false)
    int capacity;
    @Column(name = "reserved_count", nullable = false)
    int reservedCount;
    @Column(nullable = false, length = 20)
    String status = "OPEN";
    @Version
    long version;

    protected MeetupSessionJpaEntity() {
    }

    MeetupSessionJpaEntity(MeetupJpaEntity meetup, MeetupCommand command) {
        this.meetup = meetup;
        update(command);
    }

    void update(MeetupCommand command) {
        startsAt = command.startsAt();
        endsAt = command.endsAt();
        capacity = command.capacity();
    }
}
