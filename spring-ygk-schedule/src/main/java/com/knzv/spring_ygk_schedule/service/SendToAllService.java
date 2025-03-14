package com.knzv.spring_ygk_schedule.service;

import com.knzv.spring_ygk_schedule.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Service
public class SendToAllService {
    @Autowired
    private MessageService messageService;
    @Autowired
    private UserRepository userRepository;

    public void sendToAll(String message, TelegramClient telegramClient, long adminId) {
        List<Long> userIds = userRepository.findAllFieldNamesExcept(adminId);

        for (Long id : userIds) {
            messageService.sendMessage(id,"От админа: " + message, telegramClient);
        }
    }
}
