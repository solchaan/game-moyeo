package com.gamemoyeo.meetup.adapter.out.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface MeetupRepository extends JpaRepository<MeetupJpaEntity, Long> {

    @EntityGraph(attributePaths = {"session", "roleRequirements"})
    List<MeetupJpaEntity> findByStatusAndIdLessThanOrderByIdDesc(String status, long cursor, Pageable pageable);

    @EntityGraph(attributePaths = {"session", "roleRequirements"})
    List<MeetupJpaEntity> findByStatusAndGameIdAndIdLessThanOrderByIdDesc(
        String status,
        long gameId,
        long cursor,
        Pageable pageable
    );
}
