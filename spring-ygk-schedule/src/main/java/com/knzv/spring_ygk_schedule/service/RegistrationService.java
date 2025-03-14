package com.knzv.spring_ygk_schedule.service;

import com.knzv.spring_ygk_schedule.entity.Group;
import com.knzv.spring_ygk_schedule.entity.User;
import com.knzv.spring_ygk_schedule.repository.GroupRepository;
import com.knzv.spring_ygk_schedule.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Service
public class RegistrationService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;

    public void registerOrChangeGroup(long userId, String username, String groupName) {
        User user;
        Group group = groupRepository.findGroupByName(groupName).orElseThrow();

        if (userRepository.findByUserId(userId).isEmpty()) {
            user = User
                    .builder()
                    .userId(userId)
                    .username(username)
                    .group(group)
                    .build();

        }
        else {
            user = userRepository.findByUserId(userId).orElseThrow();

            user.setGroup(group);
        }
        userRepository.save(user);
    }

    public SendMessage attachKeyboard(long chatId, String groupName) {
        return SendMessage
                .builder()
                .chatId(chatId)
                .text("Ваша группа " + groupName)
                .replyMarkup(
                        ReplyKeyboardMarkup
                                .builder()
                                .keyboardRow(new KeyboardRow("Расписание"))
                                .keyboardRow(new KeyboardRow("Числитель", "Знаменатель"))
                                .keyboardRow(new KeyboardRow("Группа"))
                                .isPersistent(true)
                                .resizeKeyboard(true)
                                .build()
                )
                .build();
    }
}
