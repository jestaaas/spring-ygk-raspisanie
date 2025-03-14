package com.knzv.spring_ygk_schedule.repository;

import com.knzv.spring_ygk_schedule.entity.WeekType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeekTypeRepository extends JpaRepository<WeekType, Long> {
}
