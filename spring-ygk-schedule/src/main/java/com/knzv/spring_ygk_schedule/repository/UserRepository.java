package com.knzv.spring_ygk_schedule.repository;

import com.knzv.spring_ygk_schedule.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserId(Long userId);
    @Query(value = "SELECT user_id FROM user_ WHERE user_id <> :excludedValue", nativeQuery = true)
    List<Long> findAllFieldNamesExcept(@Param("excludedValue") Long excludedValue);
    @Override
    long count();
    @Modifying
    @Transactional // Важно для операций изменения данных
    @Query("DELETE FROM User u WHERE u.userId = :userId")
    void deleteByUserId(@Param("userId") long userId);

}
