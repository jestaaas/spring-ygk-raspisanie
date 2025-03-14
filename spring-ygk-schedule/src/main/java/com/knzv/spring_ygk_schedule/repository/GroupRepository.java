package com.knzv.spring_ygk_schedule.repository;

import com.knzv.spring_ygk_schedule.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findGroupByName(String name);
    List<Group> findAllByName(String name);
    @Query(value = "SELECT * FROM group_ g WHERE g.name LIKE CONCAT(:prefix, '%') ORDER BY g.name", nativeQuery = true)
    List<Group> findByPrefix(@Param("prefix") String prefix);

    @Query(value = "SELECT DISTINCT SUBSTRING(name, 1, 2) FROM group_ ORDER BY SUBSTRING(name, 1, 2)", nativeQuery = true)
    List<String> findDistinctPrefixes();
}
