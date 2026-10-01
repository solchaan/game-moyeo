package com.gamemoyeo.member.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<MemberJpaEntity, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from MemberJpaEntity m where m.id = :id")
    java.util.Optional<MemberJpaEntity> findForAccountLink(@org.springframework.data.repository.query.Param("id") long id);

    boolean existsByNickname(String nickname);
}
