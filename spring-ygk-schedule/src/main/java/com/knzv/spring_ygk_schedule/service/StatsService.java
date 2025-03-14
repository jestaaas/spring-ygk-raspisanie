package com.knzv.spring_ygk_schedule.service;

import com.knzv.spring_ygk_schedule.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatsService {
    @Autowired
    private UserRepository userRepository;

    public long getCountUsers() {
        return userRepository.count();
    }
}
