package com.knzv.spring_ygk_schedule.service;

import org.springframework.stereotype.Service;

@Service
public class WeekdaysConvertor {
    public long convertDay(String day) {
        return switch (day) {
            case "понедельник" -> 1;
            case "вторник" -> 2;
            case "среда" -> 3;
            case "четверг" -> 4;
            case "пятница" -> 5;
            case "суббота" -> 6;
            default -> 0;
        };
    }
}
