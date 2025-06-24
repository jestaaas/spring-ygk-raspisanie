package com.knzv.spring_ygk_schedule.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class ScheduleWebParserService {
    public Map<Integer, String > getChanges(String group) {
        Map<Integer, String> changedPairs = new HashMap<>();
        String[] urls = {"https://menu.sttec.yar.ru/timetable/rasp_second.html", "https://menu.sttec.yar.ru/timetable/rasp_first.html"};
        try {
            for (String url : urls) {
                Document document = Jsoup.connect(url).get();

                var table = document.body().select("div").select("table").get(0);
                if (!table.toString().contains(group)) {
                    continue;
                }

                var rows = table.select("tbody").select("tr");
                for (var row : rows) {

                    if (row.select("td").get(1).text().equalsIgnoreCase(group)) {
                        // Обрезаем пробелы сразу при получении текста из HTML
                        String pairNumberText = row.select("td").get(2).text().trim();
                        String subjectText = row.select("td").get(4).text().trim();
                        String roomText = row.select("td").get(5).text().trim();

                        if (pairNumberText.contains(",")) {
                            String[] numberArray = pairNumberText.split(",");
                            StringBuilder result = new StringBuilder();
                            result
                                    .append(subjectText).append(" (")
                                    .append(roomText)
                                    .append(") (❗замена)"); // Убрали \n, его добавит ScheduleService

                            for (String pairNum : numberArray) {
                                changedPairs.put(Integer.parseInt(pairNum.trim()), result.toString()); // Обрезаем пробелы для чисел
                            }
                        }
                        else if (pairNumberText.contains("-")) {
                            String[] numberArray = pairNumberText.split("-");
                            int start = Integer.parseInt(numberArray[0].trim()); // Обрезаем пробелы для чисел
                            int end = Integer.parseInt(numberArray[1].trim());   // Обрезаем пробелы для чисел

                            StringBuilder result = new StringBuilder();
                            result
                                    .append(subjectText).append(" (")
                                    .append(roomText)
                                    .append(") (❗замена)"); // Убрали \n, его добавит ScheduleService

                            for (int i = start; i <= end; i++) {
                                changedPairs.put(i, result.toString());
                            }
                        }
                        else {
                            StringBuilder result = new StringBuilder();

                            result
                                    .append(subjectText).append(" (")
                                    .append(roomText)
                                    .append(") (❗замена)"); // Убрали \n, его добавит ScheduleService

                            changedPairs.put(Integer.parseInt(pairNumberText), result.toString());
                        }

                    }
                }
            }

        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return changedPairs;
    }
}