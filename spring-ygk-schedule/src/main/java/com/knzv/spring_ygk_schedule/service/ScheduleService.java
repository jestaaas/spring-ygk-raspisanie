package com.knzv.spring_ygk_schedule.service;

import com.knzv.spring_ygk_schedule.repository.ScheduleRepository;
import com.knzv.spring_ygk_schedule.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;

@Service
public class ScheduleService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private CurrentWeekService getCurrentWeekService;
    @Autowired
    private ScheduleWebParserService updateScheduleHandler;
    @Autowired
    private WeekdaysConvertor weekdaysConvertor;

    public String getSchedule(Update update) {

        if (userRepository.findByUserId(update.getMessage().getChat().getId()).isEmpty()) {
            return "Выберите группу";
        }

        StringBuilder header = new StringBuilder();
        StringBuilder result = new StringBuilder();

        String currentDay = getCurrentWeekService.getCurrentDay();

        Long userId = update.getMessage().getChat().getId();
        long userGroup = userRepository.findByUserId(userId).get().getGroup().getId().intValue();
        String groupName = userRepository.findByUserId(userId).get().getGroup().getName();
        List<Long> weektypeIds = new ArrayList<>();

        if (getCurrentWeekService.getCurrentWeek().equalsIgnoreCase( "числитель")) {
            weektypeIds = Arrays.asList(1L, 2L);
        }
        else if (getCurrentWeekService.getCurrentWeek().equalsIgnoreCase("знаменатель")) {
            weektypeIds = Arrays.asList(1L, 3L);
        }

        long weekDayId = weekdaysConvertor.convertDay(currentDay);

        List<Object[]> schedule = scheduleRepository.findScheduleByGroupIdAndWeekdayIdAndWeektypeIds(userGroup, weekDayId, weektypeIds);

        header
                .append("Расписание на ").append(currentDay.substring(0, 1).toUpperCase() + currentDay.substring(1))
                .append(" (")
                .append(getCurrentWeekService.getCurrentWeek())
                .append(")\n\n");
        result.append(header);

        Map<Integer, String> changedPairs = updateScheduleHandler.getChanges(groupName);
        Map<Integer, String> pairsFromDB = new HashMap<>();

        for (Object[] row : schedule) {
            result
                        .append(row[1])
                        .append(" ").append(row[2])
                        .append(" (").append(row[3]).append(")")
                        .append("\n");

            pairsFromDB.put(Integer.parseInt(row[0].toString()), result.toString());
            result = new StringBuilder();
        }

        Map<Integer, String> sortedFinalSchedule = new TreeMap<>(pairsFromDB);
        sortedFinalSchedule.putAll(changedPairs);


        for (Map.Entry<Integer, String> entry : sortedFinalSchedule.entrySet()) {
            int intValue = entry.getKey();
            String emojiNumber = convertToEmojiNumber(intValue);

            result.append(emojiNumber).append(" - ")
                    .append(entry.getValue());
        }

        return result.toString();
    }
    public String getFullSchedule(Update update, String weekTypeName) {
        Long userId = update.getMessage().getChat().getId();
        String groupName = userRepository.findByUserId(userId).get().getGroup().getName();

        List<Object[]> schedule = scheduleRepository.findScheduleByGroupNameAndWeektypeName(groupName, weekTypeName);
        StringBuilder result = new StringBuilder();

        result.append("Расписание для ").append(groupName).append(" (").append(weekTypeName).append(")\n");
        String weekDay = "";
        for (var el : schedule) {
            if (!el[0].equals(weekDay)) {
                weekDay = el[0].toString();
                result.append("\n").append(weekDay).append("\n");
            }
            int intValue = Integer.parseInt(el[1].toString());
            String emojiNumber = convertToEmojiNumber(intValue);

            result.append(emojiNumber).append(" ").append(el[2]).append(" ").append(el[3]).append(" (").append(el[4]).append(")\n");
        }
        return result.toString();
    }

    private static String convertToEmojiNumber(int number) {
        String[] emojiNumbers = {"0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"};
        StringBuilder emojiString = new StringBuilder();

        if (number == 0) {
            emojiString.append(emojiNumbers[0]);
        } else {
            while (number > 0) {
                int digit = number % 10;
                emojiString.insert(0, emojiNumbers[digit]);
                number /= 10;
            }
        }

        return emojiString.toString();
    }
}
