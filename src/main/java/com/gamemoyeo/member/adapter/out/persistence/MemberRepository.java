package com.gamemoyeo.member.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<MemberJpaEntity, Long> {
}
