package com.knzv.spring_ygk_schedule.repository;

import com.knzv.spring_ygk_schedule.entity.WeekDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeekDayRepository extends JpaRepository<WeekDay, Long> {
    Optional<WeekDay> findWeekDayByName(String name);
}
