package com.knzv.spring_ygk_schedule.service;

import com.knzv.spring_ygk_schedule.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException; // Импортируем TelegramApiRequestException
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Service
public class SendToAllService {
    @Autowired
    private MessageService messageService; // Используем для отправки обычных текстовых сообщений
    @Autowired
    private UserRepository userRepository;

    private static final String TAG = "SendToAllService";

    // Метод для обработки недоступных пользователей
    private void handleUserUnavailable(long userId, TelegramApiException e) {
        System.err.println("Пользователь " + userId + " недоступен: " + e.getMessage());
        // Добавляем логику для удаления пользователя из базы данных
        userRepository.deleteByUserId(userId);
        System.out.println("Пользователь " + userId + " удален из базы данных.");
    }

    public void sendMessage(long chatId, String message, TelegramClient telegramClient, long adminId) {
        List<Long> userIds = userRepository.findAllFieldNamesExcept(adminId);

        for (Long id : userIds) {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(id)
                    .text("От админа: " + message)
                    .build();
            try {
                telegramClient.execute(sendMessage);
            } catch (TelegramApiRequestException e) {
                if (e.getErrorCode() == 403 || e.getErrorCode() == 400) {
                    handleUserUnavailable(id, e);
                } else {
                    System.err.println("Ошибка Telegram API при отправке сообщения пользователю " + id + ": " + e.getMessage());
                }
            } catch (TelegramApiException e) {
                System.err.println("Общая ошибка Telegram API при отправке сообщения пользователю " + id + ": " + e.getMessage());
            }
        }
    }

    public void sendPhotoToAll(String fileId, String caption, TelegramClient telegramClient, long adminId) {
        List<Long> userIds = userRepository.findAllFieldNamesExcept(adminId);

        for (Long id : userIds) {
            SendPhoto photo = SendPhoto.builder()
                    .chatId(id)
                    .photo(new InputFile(fileId))
                    .caption(caption)
                    .build();
            try {
                telegramClient.execute(photo);
            } catch (TelegramApiRequestException e) {
                if (e.getErrorCode() == 403 || e.getErrorCode() == 400) {
                    handleUserUnavailable(id, e);
                } else {
                    System.err.println("Ошибка Telegram API при отправке фото пользователю " + id + ": " + e.getMessage());
                }
            } catch (TelegramApiException e) {
                System.err.println("Общая ошибка Telegram API при отправке фото пользователю " + id + ": " + e.getMessage());
            }
        }
    }

    public void sendMediaGroupToAll(List<InputMedia> mediaGroup, TelegramClient telegramClient, long adminId) {
        List<Long> userIds = userRepository.findAllFieldNamesExcept(adminId);

        for (Long id : userIds) {
            SendMediaGroup media = SendMediaGroup.builder()
                    .chatId(id)
                    .medias(mediaGroup)
                    .build();
            try {
                telegramClient.execute(media);
            } catch (TelegramApiRequestException e) {
                if (e.getErrorCode() == 403 || e.getErrorCode() == 400) {
                    handleUserUnavailable(id, e);
                } else {
                    System.err.println("Ошибка Telegram API при отправке медиагруппы пользователю " + id + ": " + e.getMessage());
                }
            } catch (TelegramApiException e) {
                System.err.println("Общая ошибка Telegram API при отправке медиагруппы пользователю " + id + ": " + e.getMessage());
            }
        }
    }
}