package com.knzv.spring_ygk_schedule.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CurrentWeekService {
    public String getCurrentWeek() {
        String result = "";
        try {
            Document document = Jsoup.connect("https://menu.sttec.yar.ru/timetable/rasp_second.html").get();

            result = extractWeekType(
                    document.body()
                            .select("div")
                            .select("div")
                            .get(3)
                            .text()
            );
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    public String getCurrentDay() {
        String result = "";
        try {
            Document document = Jsoup.connect("https://menu.sttec.yar.ru/timetable/rasp_second.html").get();

            result = extractDayName(
                    document.body()
                            .select("div")
                            .select("div")
                            .get(2)
                            .text()
            );
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    private String extractWeekType(String string) {
        String result = "";
        Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(string);

        if (matcher.find()) {
            result = matcher.group(1);
        }
        return result;
    }

    private String extractDayName(String string) {
        int index = string.indexOf("/");

        if (index != -1) {
            return string.substring(index + 1).trim();
        }
        return "";
    }
}
