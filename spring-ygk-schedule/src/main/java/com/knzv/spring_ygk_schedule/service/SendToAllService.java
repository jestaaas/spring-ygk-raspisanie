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
    private MessageService messageService;
    @Autowired
    private UserRepository userRepository;

    public void sendToAll(String message, TelegramClient telegramClient, long adminId) {
        List<Long> userIds = userRepository.findAllFieldNamesExcept(adminId);

        for (Long id : userIds) {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(id)
                    .text("От админа: " + message)
                    .build();
            try {
                telegramClient.execute(sendMessage);
            } catch (TelegramApiRequestException e) {
                // Обрабатываем специфические ошибки Telegram API
                // 403 Forbidden: бот заблокирован пользователем
                // 400 Bad Request: чат не найден или ID невалиден
                if (e.getErrorCode() == 403) {
                    System.err.println("Пользователь " + id + " заблокировал бота. Сообщение не отправлено. Ошибка: " + e.getMessage());
                } else if (e.getErrorCode() == 400) {
                    System.err.println("Ошибка при отправке сообщения пользователю " + id + ": чат не найден или ID невалиден. Ошибка: " + e.getMessage());
                } else {
                    System.err.println("Ошибка Telegram API при отправке сообщения пользователю " + id + ": " + e.getErrorCode() + " - " + e.getMessage());
                }
            } catch (TelegramApiException e) {
                // Обрабатываем другие общие ошибки Telegram API
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
                if (e.getErrorCode() == 403) {
                    System.err.println("Пользователь " + id + " заблокировал бота. Фото не отправлено. Ошибка: " + e.getMessage());
                } else if (e.getErrorCode() == 400) {
                    System.err.println("Ошибка при отправке фото пользователю " + id + ": чат не найден или ID невалиден. Ошибка: " + e.getMessage());
                } else {
                    System.err.println("Ошибка Telegram API при отправке фото пользователю " + id + ": " + e.getErrorCode() + " - " + e.getMessage());
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
                if (e.getErrorCode() == 403) {
                    System.err.println("Пользователь " + id + " заблокировал бота. Медиагруппа не отправлена. Ошибка: " + e.getMessage());
                } else if (e.getErrorCode() == 400) {
                    System.err.println("Ошибка при отправке медиагруппы пользователю " + id + ": чат не найден или ID невалиден. Ошибка: " + e.getMessage());
                } else {
                    System.err.println("Ошибка Telegram API при отправке медиагруппы пользователю " + id + ": " + e.getErrorCode() + " - " + e.getMessage());
                }
            } catch (TelegramApiException e) {
                System.err.println("Общая ошибка Telegram API при отправке медиагруппы пользователю " + id + ": " + e.getMessage());
            }
        }
    }
}