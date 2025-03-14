package com.knzv.spring_ygk_schedule.repository;

import com.knzv.spring_ygk_schedule.entity.Schedule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query(value = "SELECT pair_number, subject, teacher, room FROM schedule WHERE group_id = :groupId AND weekday_id = :weekdayId AND weektype_id IN (:weektypeIds)", nativeQuery = true)
    List<Object[]> findScheduleByGroupIdAndWeekdayIdAndWeektypeIds(
            @Param("groupId") Long groupId,
            @Param("weekdayId") Long weekdayId,
            @Param("weektypeIds") List<Long> weektypeIds);
    @Query(value = "SELECT week_day.name, pair_number, subject, teacher, room FROM schedule INNER JOIN group_ ON group_.id=schedule.group_id INNER JOIN week_type ON week_type.id = schedule.weektype_id INNER JOIN week_day ON week_day.id = schedule.weekday_id WHERE group_.name= :groupName AND week_type.name in('чз', :weektypeName)", nativeQuery = true)
    List<Object[]> findScheduleByGroupNameAndWeektypeName(
            @Param("groupName") String groupName,
            @Param("weektypeName") String weektypeName
    );
}
