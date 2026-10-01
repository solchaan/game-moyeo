package com.gamemoyeo.member.adapter.out.persistence;

import com.gamemoyeo.member.domain.RiotAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "riot_account")
public class RiotAccountJpaEntity {
    @Id
    @Column(name = "member_id")
    private Long memberId;
    @Column(nullable = false, unique = true, length = 191)
    private String puuid;
    @Column(name = "game_name", nullable = false, length = 100)
    private String gameName;
    @Column(name = "tag_line", nullable = false, length = 32)
    private String tagLine;
    protected RiotAccountJpaEntity() { }
    public RiotAccountJpaEntity(long memberId, RiotAccount account) {
        this.memberId = memberId;
        this.puuid = account.puuid();
        this.gameName = account.gameName();
        this.tagLine = account.tagLine();
    }
    public RiotAccount toDomain() {
        return new RiotAccount(puuid, gameName, tagLine);
    }
}
