package com.knzv.spring_ygk_schedule.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
public class MessageService {
    public void sendMessage(long chatId, String text, TelegramClient telegramClient) {
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .build();

        try {
            telegramClient.execute(message);
        }
        catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }
}
