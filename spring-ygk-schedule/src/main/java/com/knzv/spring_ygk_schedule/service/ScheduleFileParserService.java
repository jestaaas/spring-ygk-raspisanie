package com.knzv.spring_ygk_schedule.service;

import com.knzv.spring_ygk_schedule.entity.Group;
import com.knzv.spring_ygk_schedule.entity.Schedule;
import com.knzv.spring_ygk_schedule.entity.WeekDay;
import com.knzv.spring_ygk_schedule.entity.WeekType;
import com.knzv.spring_ygk_schedule.repository.GroupRepository;
import com.knzv.spring_ygk_schedule.repository.ScheduleRepository;
import com.knzv.spring_ygk_schedule.repository.WeekDayRepository;
import com.knzv.spring_ygk_schedule.repository.WeekTypeRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class ScheduleFileParserService {
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private WeekdaysConvertor weekdaysConvertor;
    @Autowired
    private WeekDayRepository weekDayRepository;
    @Autowired
    private WeekTypeRepository weekTypeRepository;
    private void parseGroups(String fileName) throws IOException {
        File input = new File(fileName);
        Document doc = Jsoup.parse(input, "UTF-8");
        String[] strParts
                = {"ИС1", "ИБ1", "СА1", "СТ1", "РК1", "ДИ1", "СД2", "АР1", "ЗИ1", "ЮС1",
                "ЗМ1", "УД1", "ТТ1", "ТУ1", "ЮР1", "МА1", "ЭК1", "МС1", "МО2", "ГД1", "ЮР2", "ЮС2"};

        var table = doc.body().select("table").get(0).select("tbody");

        for (var row : table.select("tr")) {
            if (row.text().isEmpty()) {
                continue;
            }

            for (String part : strParts) {
                if (row.select("td").get(0).text().contains(part)) {

                    if (row.select("td").get(0).text().contains("/") && !row.select("td").get(0).text().contains("ИС1-43")) {
                        int indexSlash = row.select("td").get(0).text().indexOf("/");

                        Group group = Group
                                .builder()
                                .name(row.select("td").get(0).text().substring(0, indexSlash))
                                .build();

                        groupRepository.save(group);
                    }
                    else {
                        Group group = Group
                                .builder()
                                .name(row.select("td").get(0).text().substring(0, row.select("td").get(0).text().length()))
                                .build();

                        groupRepository.save(group);
                    }
                }
            }
        }
    }

    public void parseSchedule(String fileName) throws IOException {

        File input = new File("d:\\bot\\" + fileName);
        Document doc = Jsoup.parse(input, "UTF-8");

        String[] strParts
                = {"ИС1", "ИБ1", "СА1", "СТ1", "РК1", "ДИ1", "СД2", "АР1", "ЗИ1", "ЮС1",
                "ЗМ1", "УД1", "ТТ1", "ТУ1", "ЮР1", "МА1", "ЭК1", "МС1", "МО2", "ГД1", "ЮР2", "ЮС2"};
        String[] daysOfWeek = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};

        var table = doc.body().select("table").get(0).select("tbody");

        String currentGroup = "";
        String currentWeek = "";
        int indexSlash = 6;
        int pairNum = -1;
        String pair = "";
        String teacher = "";
        String room = "";
        Group group = new Group();
        WeekDay weekDay = new WeekDay();

        WeekType mixType = weekTypeRepository.findById(Long.parseLong("1")).orElseThrow();
        WeekType evenType = weekTypeRepository.findById(Long.parseLong("2")).orElseThrow();
        WeekType oddType = weekTypeRepository.findById(Long.parseLong("3")).orElseThrow();

        for (var row : table.select("tr")) {
            if (row.text().isEmpty()) {
                continue;
            }

            for (String part : strParts) {
                if (row.select("td").get(0).text().trim().contains(part)) {
                    currentGroup = row.select("td").get(0).text().trim().substring(0, indexSlash);

                    group = groupRepository.findGroupByName(currentGroup).orElseThrow();

                    System.out.println(currentGroup);
                    break;
                }
            }
            for (String day : daysOfWeek) {
                if (row.select("td").get(0).text().equalsIgnoreCase(day)) {
                    currentWeek = row.select("td").get(0).text();

                    weekDay = weekDayRepository.findWeekDayByName(currentWeek).orElseThrow();

                    System.out.println(currentWeek);
                    break;
                }
            }
            if (row.select("td").size() == 4) {
                String row0 = row.select("td").get(0).text();
                String row1 = row.select("td").get(1).text();
                String row2 = row.select("td").get(2).text();
                String row3 = row.select("td").get(3).text();
                if (
                        row1.equalsIgnoreCase("") &&
                                row2.equalsIgnoreCase("") &&
                                row3.equalsIgnoreCase("")
                ) {
                    pairNum = Integer.parseInt(row0);
                    System.out.println(
                            row0 + " верхняя пара только для знаменателя"
                    );
                }

                else {
                    System.out.println(
                            row0 + " " + row1 + " " + row2 + " " + row3 +"\n"
                    );
                    Schedule schedule;
                    if (!row.select("td").get(0).hasAttr("rowspan")) {
                         schedule = Schedule.builder()
                                .pairNumber(Integer.parseInt(row0))
                                .subject(row1)
                                .teacher(row2)
                                .room(row3)
                                .group(group)
                                .weekDay(weekDay)
                                .weekType(mixType)
                                .build();
                    }
                    else if (
                            row1.equalsIgnoreCase("") &&
                            row2.equalsIgnoreCase("") &&
                            row.select("td").get(3).hasAttr("rowspan")
                    ) {
                        pairNum = Integer.parseInt(row0);
                        continue;
                    }
                    else {
                         schedule = Schedule.builder()
                                .pairNumber(Integer.parseInt(row0))
                                .subject(row1)
                                .teacher(row2)
                                .room(row3)
                                .group(group)
                                .weekDay(weekDay)
                                .weekType(evenType)
                                .build();
                    }


                    pairNum = Integer.parseInt(row0);
                    pair = row1;
                    teacher = row2;
                    room = row3;

                    scheduleRepository.save(schedule);
                }
            }
            else if (row.select("td").size() == 3) {
                String row1 = row.select("td").get(0).text();
                String row2 = row.select("td").get(1).text();
                String row3 = row.select("td").get(2).text();
                if (row1.equalsIgnoreCase("") && row2.equalsIgnoreCase("") && row3.equalsIgnoreCase("")) {
                    System.out.println("Эта пара только для числителя");
                }

                else {
                    Schedule schedule = Schedule.builder()
                            .pairNumber(pairNum)
                            .subject(row1)
                            .teacher(row2)
                            .room(row3)
                            .group(group)
                            .weekDay(weekDay)
                            .weekType(oddType)
                            .build();

                    System.out.println(pairNum + " " + row1 + " " + row2 + " " + row3 + " знаменатель\n");
                    scheduleRepository.save(schedule);

                }
            }
            else if (row.select("td").size() == 2) {
                String row1 = row.select("td").get(0).text();
                String row2 = row.select("td").get(1).text();

                if (row1.equalsIgnoreCase("") && row2.equalsIgnoreCase("")) {
                    System.out.println("Эта пара только для числителя");
                    continue;
                }

                Schedule schedule = Schedule.builder()
                        .pairNumber(pairNum)
                        .subject(row1)
                        .teacher(row2)
                        .room(room)
                        .group(group)
                        .weekDay(weekDay)
                        .weekType(oddType)
                        .build();

                System.out.println(pairNum + " " + row1 + " " + row2 + " " + room + " знаменатель\n");
                scheduleRepository.save(schedule);

            }
            else if (row.select("td").size() == 1) {

                var el = row.select("td").getFirst();
                if (el.hasClass("column1")) {

                    Schedule schedule = Schedule.builder()
                            .pairNumber(pairNum)
                            .subject(el.text())
                            .teacher(teacher)
                            .room(room)
                            .group(group)
                            .weekDay(weekDay)
                            .weekType(oddType)
                            .build();

                    System.out.println(pairNum + " " + el.text() + " " + teacher + " " + room + " знаменатель\n");
                    scheduleRepository.save(schedule);

                }
                else if (row.select("td").getFirst().hasClass("column8")) {

                    Schedule schedule = Schedule.builder()
                            .pairNumber(pairNum)
                            .subject(pair)
                            .teacher(teacher)
                            .room(el.text())
                            .group(group)
                            .weekDay(weekDay)
                            .weekType(oddType)
                            .build();

                    System.out.println(
                            pairNum + " " + pair + " " + teacher + " " + el.text() + " знаменатель\n"
                    );
                    scheduleRepository.save(schedule);
                }
            }
        }
    }

}
