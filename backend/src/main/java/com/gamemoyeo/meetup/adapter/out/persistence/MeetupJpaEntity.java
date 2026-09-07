package com.gamemoyeo.meetup.adapter.out.persistence;

import com.gamemoyeo.meetup.application.MeetupBoardUseCase.MeetupCommand;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "meetup")
class MeetupJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "game_id", nullable = false)
    long gameId;
    @Column(name = "owner_id", nullable = false)
    long ownerId;
    @Column(name = "mode_option_id")
    Long modeOptionId;
    @Column(name = "platform_option_id")
    Long platformOptionId;
    @Column(name = "region_option_id")
    Long regionOptionId;
    @Column(name = "minimum_tier_option_id")
    Long minimumTierOptionId;
    @Column(name = "maximum_tier_option_id")
    Long maximumTierOptionId;
    @Column(nullable = false, length = 160)
    String title;
    @Column(columnDefinition = "text")
    String description;
    @Column(name = "play_style", nullable = false, length = 30)
    String playStyle;
    @Column(name = "voice_chat_policy", nullable = false, length = 30)
    String voiceChatPolicy;
    @Column(name = "approval_type", nullable = false, length = 30)
    String approvalType;
    @Column(name = "recruitment_deadline")
    Instant recruitmentDeadline;
    @Column(nullable = false, length = 20)
    String status = "OPEN";
    @Version
    long version;

    @OneToOne(mappedBy = "meetup", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true,
        fetch = FetchType.LAZY)
    MeetupSessionJpaEntity session;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "meetup_role_requirement", joinColumns = @JoinColumn(name = "meetup_id"))
    @MapKeyColumn(name = "role_option_id")
    @Column(name = "capacity")
    Map<Long, Integer> roleRequirements = new HashMap<>();

    protected MeetupJpaEntity() {
    }

    MeetupJpaEntity(long ownerId, MeetupCommand command) {
        this.ownerId = ownerId;
        update(command);
        this.session = new MeetupSessionJpaEntity(this, command);
        this.status = session.status;
    }

    void update(MeetupCommand command) {
        gameId = command.gameId();
        modeOptionId = command.modeOptionId();
        platformOptionId = command.platformOptionId();
        regionOptionId = command.regionOptionId();
        minimumTierOptionId = command.minimumTierOptionId();
        maximumTierOptionId = command.maximumTierOptionId();
        title = command.title();
        description = command.description();
        playStyle = command.playStyle();
        voiceChatPolicy = command.voiceChatPolicy();
        approvalType = command.approvalType();
        recruitmentDeadline = command.recruitmentDeadline();
        roleRequirements.clear();
        roleRequirements.putAll(command.roleRequirements());
        if (session != null) {
            session.update(command);
            status = session.status;
        }
    }
}
