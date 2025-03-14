package com.knzv.spring_ygk_schedule.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.*;


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
                        // тут был код
                        if (row.select("td").get(2).text().contains(",")) {
                            String[] numberArray = row.select("td").get(2).text().split(",");
                            StringBuilder result = new StringBuilder();
                            result
                                    .append(row.select("td").get(4).text()).append(" (")
                                    .append(row.select("td").get(5).text())
                                    .append(") (❗замена)")
                                    .append("\n");

                            for (String pairNum : numberArray) {
                                changedPairs.put(Integer.parseInt(pairNum), result.toString());
                            }
                        }
                        else if (row.select("td").get(2).text().contains("-")) {
                            String[] numberArray = row.select("td").get(2).text().split("-");
                            List<String> pairNumArray = new ArrayList<>();
                            for (int i = Integer.parseInt(numberArray[0]); i <= Integer.parseInt(numberArray[1]); i++) {
                                pairNumArray.add(i + "");
                            }
                            for (int i = Integer.parseInt(numberArray[0]); i <= Integer.parseInt(numberArray[1]); i++) {
                                StringBuilder result = new StringBuilder();
                                result
                                        .append(row.select("td").get(4).text()).append(" (")
                                        .append(row.select("td").get(5).text())
                                        .append(") (❗замена)")
                                        .append("\n");

                                for (String pairNum : pairNumArray) {
                                    changedPairs.put(Integer.parseInt(pairNum), result.toString());
                                }
                            }
                        }
                        else {
                            StringBuilder result = new StringBuilder();

                            result
                                    .append(row.select("td").get(4).text()).append(" (")
                                    .append(row.select("td").get(5).text())
                                    .append(") (❗замена)")
                                    .append("\n");

                            changedPairs.put(Integer.parseInt(row.select("td").get(2).text()), result.toString());
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
