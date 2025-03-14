package com.knzv.spring_ygk_schedule.service;

import org.springframework.stereotype.Service;

@Service
public class WeekdaysConvertor {
    public long convertDay(String day) {
        switch (day) {
            case "понедельник":
                return 1;
            case "вторник":
                return 2;
            case "среда":
                return 3;
            case "четверг":
                return 4;
            case "пятница":
                return 5;
            case "суббота":
                return 6;
            default:
                return 0;
        }
    }
}
