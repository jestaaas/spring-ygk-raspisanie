package com.knzv.spring_ygk_schedule.repository;

import com.knzv.spring_ygk_schedule.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserId(Long userId);
    @Query(value = "SELECT user_id FROM user_ WHERE user_id <> :excludedValue", nativeQuery = true)
    List<Long> findAllFieldNamesExcept(@Param("excludedValue") Long excludedValue);
    @Override
    long count();
}
