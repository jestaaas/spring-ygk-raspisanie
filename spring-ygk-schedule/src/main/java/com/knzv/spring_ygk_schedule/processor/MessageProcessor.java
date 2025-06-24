package com.knzv.spring_ygk_schedule.processor;

import com.knzv.spring_ygk_schedule.enums.BotState;
import com.knzv.spring_ygk_schedule.repository.UserRepository;
import com.knzv.spring_ygk_schedule.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class MessageProcessor {

    @Autowired
    private MessageService sendMessageService;
    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private ScheduleFileParserService scheduleFileParserService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SelectGroupService selectGroupService;
    @Autowired
    private SendToAllService sendToAllService;
    @Autowired
    private StatsService statsService;

    private final Map<String, List<InputMedia>> mediaGroups = new ConcurrentHashMap<>();
    private final Map<String, Long> mediaGroupTimestamps = new ConcurrentHashMap<>();

    private static final long MEDIA_GROUP_TIMEOUT_MS = 1500; // Таймаут в миллисекундах для определения конца медиагруппы

    public void processMessage(Update update, BotState currentState, TelegramClient telegramClient, Map<Long, BotState> botStates, long adminId) {
        long chatId = update.getMessage().getChatId();

        boolean hasText = update.getMessage().hasText();
        boolean hasPhoto = update.getMessage().hasPhoto();
        // ИСПРАВЛЕНИЕ ЗДЕСЬ:
        boolean hasMediaGroupId = update.getMessage().getMediaGroupId() != null; // Правильная проверка наличия media_group_id
        String mediaGroupId = update.getMessage().getMediaGroupId();

        String text = hasText ? update.getMessage().getText() : null;

        switch (currentState) {
            case NORMAL:
                handleNormalStateMessage(chatId, text, update, telegramClient, botStates, adminId);
                break;
            case SEND_MESSAGE:
                if (hasText) {
                    if (text.equalsIgnoreCase("/cancel_send")) {
                        sendMessageService.sendMessage(chatId, "Рассылка отменена.", telegramClient);
                        setBotState(chatId, BotState.NORMAL, botStates);
                        return;
                    }
                    sendToAllService.sendToAll(text, telegramClient, adminId);
                    sendMessageService.sendMessage(chatId, "Сообщение успешно отправлено всем пользователям!", telegramClient);
                    setBotState(chatId, BotState.NORMAL, botStates);
                } else {
                    sendMessageService.sendMessage(chatId, "Пожалуйста, введите текстовое сообщение. Или /cancel_send для отмены.", telegramClient);
                }
                break;
            case SEND_FILENAME:
                if (hasText) {
                    if (text.equalsIgnoreCase("/cancel_send")) {
                        sendMessageService.sendMessage(chatId, "Ввод имени файла отменен.", telegramClient);
                        setBotState(chatId, BotState.NORMAL, botStates);
                        return;
                    }
                    try {
                        scheduleFileParserService.parseSchedule(text);
                        sendMessageService.sendMessage(chatId, "Группы в базе!", telegramClient);
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                        sendMessageService.sendMessage(chatId, "Ошибка при парсинге файла: " + e.getMessage(), telegramClient);
                    }
                    setBotState(chatId, BotState.NORMAL, botStates);
                } else {
                    sendMessageService.sendMessage(chatId, "Пожалуйста, введите название файла. Или /cancel_send для отмены.", telegramClient);
                }
                break;
            case SEND_PHOTO:
                if (hasText && text.equalsIgnoreCase("/cancel_send")) {
                    sendMessageService.sendMessage(chatId, "Отправка фотографий отменена.", telegramClient);
                    setBotState(chatId, BotState.NORMAL, botStates);
                    return;
                }

                if (hasPhoto) {
                    List<PhotoSize> photos = update.getMessage().getPhoto();
                    String fileId = photos.stream()
                            .max(Comparator.comparing(PhotoSize::getFileSize))
                            .map(PhotoSize::getFileId)
                            .orElse(null);

                    String caption = update.getMessage().getCaption();

                    if (fileId != null) {
                        if (hasMediaGroupId) { // Теперь здесь просто проверяем hasMediaGroupId
                            mediaGroups.computeIfAbsent(mediaGroupId, k -> new ArrayList<>())
                                    .add(InputMediaPhoto.builder().media(fileId).caption(caption).build());
                            mediaGroupTimestamps.put(mediaGroupId, System.currentTimeMillis());

                            sendMessageService.sendMessage(chatId, "Получено фото (часть группы). Ожидаю остальные...", telegramClient);

                            // Запускаем поток для обработки медиагруппы после таймаута
                            // В реальном приложении для этого лучше использовать ScheduledExecutorService
                            new Thread(() -> {
                                try {
                                    Thread.sleep(MEDIA_GROUP_TIMEOUT_MS);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                Long lastReceived = mediaGroupTimestamps.get(mediaGroupId);
                                if (lastReceived != null && (System.currentTimeMillis() - lastReceived) >= MEDIA_GROUP_TIMEOUT_MS) {
                                    List<InputMedia> groupToSend = mediaGroups.remove(mediaGroupId);
                                    mediaGroupTimestamps.remove(mediaGroupId);

                                    if (groupToSend != null && !groupToSend.isEmpty()) {
                                        sendToAllService.sendMediaGroupToAll(groupToSend, telegramClient, adminId);
                                        sendMessageService.sendMessage(chatId, "Медиагруппа успешно отправлена всем пользователям!", telegramClient);
                                        setBotState(chatId, BotState.NORMAL, botStates);
                                    }
                                }
                            }).start();

                        } else {
                            // Это одиночное фото
                            sendToAllService.sendPhotoToAll(fileId, caption, telegramClient, adminId);
                            sendMessageService.sendMessage(chatId, "Фотография успешно отправлена всем пользователям!", telegramClient);
                            setBotState(chatId, BotState.NORMAL, botStates);
                        }
                    } else {
                        sendMessageService.sendMessage(chatId, "Не удалось получить ID фотографии. Пожалуйста, попробуйте еще раз.", telegramClient);
                        setBotState(chatId, BotState.NORMAL, botStates);
                    }
                } else {
                    sendMessageService.sendMessage(chatId, "Пожалуйста, отправьте фотографию (или группу фотографий) или /cancel_send для отмены.", telegramClient);
                }
                break;
        }
    }

    private void handleNormalStateMessage(long chatId, String text, Update update, TelegramClient telegramClient, Map<Long, BotState> botStates, long adminId) {
        if (text == null) {
            return;
        }

        if (text.equalsIgnoreCase("/start")) {
            if (userRepository.findByUserId(update.getMessage().getChat().getId()).isEmpty()) {
                sendMessageService.sendMessage(chatId, "Добро пожаловать! Это неофициальный бот для получения расписания в ЯГК.", telegramClient);
                SendMessage msg = selectGroupService.changeGroup(update);
                try {
                    telegramClient.execute(msg);
                } catch (TelegramApiException e) {
                    System.out.println(e.getMessage());
                }
            } else {
                sendMessageService.sendMessage(chatId, "Вы уже вошли", telegramClient);
            }
        } else if (text.equalsIgnoreCase("Расписание")) {
            String result = scheduleService.getSchedule(update);
            sendMessageService.sendMessage(chatId, result, telegramClient);
        } else if (text.equalsIgnoreCase("/parse") && chatId == adminId) {
            sendMessageService.sendMessage(chatId, "Введите название файла", telegramClient);
            setBotState(chatId, BotState.SEND_FILENAME, botStates);
        } else if (text.equalsIgnoreCase("/stats") && chatId == adminId) {
            long count = statsService.getCountUsers();
            sendMessageService.sendMessage(chatId, "На данный момент ботом пользуется " + count + " пользователей", telegramClient);
        } else if (text.equalsIgnoreCase("/send") && chatId == adminId) {
            SendMessage sendMessage = SendMessage
                    .builder().chatId(chatId).text("Введите сообщение")
                    .replyMarkup(
                            InlineKeyboardMarkup
                                    .builder()
                                    .keyboardRow(new InlineKeyboardRow(
                                            InlineKeyboardButton.builder()
                                                    .text("Отменить")
                                                    .callbackData("CANCEL_SEND")
                                                    .build()
                                    ))
                                    .build()
                    )
                    .build();
            try {
                telegramClient.execute(sendMessage);
            } catch (TelegramApiException e) {
                System.out.println(e.getMessage());
            }
            setBotState(chatId, BotState.SEND_MESSAGE, botStates);
        } else if (text.equalsIgnoreCase("/sendphoto") && chatId == adminId) {
            SendMessage sendMessage = SendMessage
                    .builder().chatId(chatId).text("Отправьте одну фотографию или группу фотографий (до 10 штук).")
                    .replyMarkup(
                            InlineKeyboardMarkup
                                    .builder()
                                    .keyboardRow(new InlineKeyboardRow(
                                            InlineKeyboardButton.builder()
                                                    .text("Отменить")
                                                    .callbackData("CANCEL_SEND")
                                                    .build()
                                    ))
                                    .build()
                    )
                    .build();
            try {
                telegramClient.execute(sendMessage);
            } catch (TelegramApiException e) {
                System.out.println(e.getMessage());
            }
            setBotState(chatId, BotState.SEND_PHOTO, botStates);
        }
        else if (text.equalsIgnoreCase("Группа")) {
            if (userRepository.findByUserId(update.getMessage().getChat().getId()).isEmpty()) {
                sendMessageService.sendMessage(chatId, "Выберите группу", telegramClient);
                return;
            }
            userRepository.findByUserId(update.getMessage().getChat().getId()).ifPresentOrElse(user -> {
                String groupName = user.getGroup().getName();
                sendMessageService.sendMessage(chatId, "Ваша текущая группа " + groupName, telegramClient);

                SendMessage message = SendMessage.builder()
                        .chatId(chatId)
                        .text("Ваша текущая группа")
                        .replyMarkup(
                                InlineKeyboardMarkup.builder()
                                        .keyboardRow(
                                                new InlineKeyboardRow(
                                                        InlineKeyboardButton
                                                                .builder()
                                                                .text("Выбрать другую")
                                                                .callbackData("CHANGE_GROUP")
                                                                .build()
                                                )
                                        ).build()
                        )
                        .build();
                try {
                    telegramClient.execute(message);
                } catch (TelegramApiException e) {
                    System.out.println(e.getMessage());
                }
            }, () -> sendMessageService.sendMessage(chatId, "Пользователь не найден. Пожалуйста, начните с /start", telegramClient));
        } else if (text.equalsIgnoreCase("Знаменатель") || text.equalsIgnoreCase("Числитель")) {
            String message = scheduleService.getFullSchedule(update, text);
            sendMessageService.sendMessage(chatId, message, telegramClient);
        }
    }

    private void setBotState(Long chatId, BotState state, Map<Long, BotState> botStates) {
        botStates.put(chatId, state);
    }
}