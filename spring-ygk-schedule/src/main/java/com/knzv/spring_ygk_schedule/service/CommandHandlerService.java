package com.knzv.spring_ygk_schedule.service;

import com.knzv.spring_ygk_schedule.enums.BotState;
import com.knzv.spring_ygk_schedule.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.util.function.BiConsumer;

@Service
public class CommandHandlerService {
    private final MessageService messageService;
    private final RegistrationService registrationService;
    private final ScheduleService scheduleService;
    private final ScheduleFileParserService scheduleFileParserService;
    private final SelectGroupService selectGroupService;
    private final SendToAllService sendToAllService;
    private final StatsService statsService;
    private final UserRepository userRepository;

    @Value("${app.admin_id}")
    private long adminId;

    public CommandHandlerService(MessageService messageService, RegistrationService registrationService,
                                 ScheduleService scheduleService, ScheduleFileParserService scheduleFileParserService,
                                 SelectGroupService selectGroupService, SendToAllService sendToAllService,
                                 StatsService statsService, UserRepository userRepository) {
        this.messageService = messageService;
        this.registrationService = registrationService;
        this.scheduleService = scheduleService;
        this.scheduleFileParserService = scheduleFileParserService;
        this.selectGroupService = selectGroupService;
        this.sendToAllService = sendToAllService;
        this.statsService = statsService;
        this.userRepository = userRepository;
    }

    public void handleCommand(Update update, BotState currentState, long chatId, String text, TelegramClient telegramClient,
                              BiConsumer<Long, BotState> setBotState) {
        switch (currentState) {
            case NORMAL:
                if (text.equalsIgnoreCase("/start")) {
                    if (userRepository.findByUserId(update.getMessage().getChat().getId()).isEmpty()) {
                        messageService.sendMessage(chatId, "Добро пожаловать! Это неофициальный бот для получения расписания в ЯГК.", telegramClient);
                        SendMessage msg = selectGroupService.changeGroup(update);
                        try {
                            telegramClient.execute(msg);
                        } catch (TelegramApiException e) {
                            System.err.println("Error executing start message: " + e.getMessage());
                        }
                    } else {
                        messageService.sendMessage(chatId, "Вы уже вошли", telegramClient);
                    }
                } else if (text.equalsIgnoreCase("Расписание")) {
                    String result = scheduleService.getSchedule(update);
                    messageService.sendMessage(chatId, result, telegramClient);
                } else if (text.equalsIgnoreCase("/parse") && chatId == adminId) {
                    messageService.sendMessage(chatId, "Введите название файла", telegramClient);
                    setBotState.accept(chatId, BotState.SEND_FILENAME);
                } else if (text.equalsIgnoreCase("/stats") && chatId == adminId) {
                    long count = statsService.getCountUsers();
                    messageService.sendMessage(chatId, "На данный момент ботом пользуется " + count + " пользователей", telegramClient);
                } else if (text.equalsIgnoreCase("/send") && chatId == adminId) {
                    SendMessage sendMessage = SendMessage.builder()
                            .chatId(chatId)
                            .text("Введите сообщение")
                            .replyMarkup(InlineKeyboardMarkup.builder()
                                    .keyboardRow(new InlineKeyboardRow(
                                            InlineKeyboardButton.builder()
                                                    .text("Отменить")
                                                    .callbackData("CANCEL_SEND")
                                                    .build()))
                                    .build())
                            .build();
                    try {
                        telegramClient.execute(sendMessage);
                    } catch (TelegramApiException e) {
                        System.err.println("Error executing send command message: " + e.getMessage());
                    }
                    setBotState.accept(chatId, BotState.SEND_MESSAGE);
                } else if (text.equalsIgnoreCase("Группа")) {
                    if (userRepository.findByUserId(update.getMessage().getChat().getId()).isEmpty()) {
                        messageService.sendMessage(chatId, "Выберите группу", telegramClient);
                        return;
                    }
                    var user = userRepository.findByUserId(update.getMessage().getChat().getId()).orElseThrow();
                    String groupName = user.getGroup().getName();
                    messageService.sendMessage(chatId, "Ваша текущая группа " + groupName, telegramClient);

                    SendMessage message = SendMessage.builder()
                            .chatId(chatId)
                            .text("Ваша текущая группа")
                            .replyMarkup(InlineKeyboardMarkup.builder()
                                    .keyboardRow(new InlineKeyboardRow(
                                            InlineKeyboardButton.builder()
                                                    .text("Выбрать другую")
                                                    .callbackData("CHANGE_GROUP")
                                                    .build()))
                                    .build())
                            .build();
                    try {
                        telegramClient.execute(message);
                    } catch (TelegramApiException e) {
                        System.err.println("Error executing group message: " + e.getMessage());
                    }
                } else if (text.equalsIgnoreCase("Знаменатель") || text.equalsIgnoreCase("Числитель")) {
                    String message = scheduleService.getFullSchedule(update, text);
                    messageService.sendMessage(chatId, message, telegramClient);
                }
                break;
            case SEND_MESSAGE:
                sendToAllService.sendToAll(text, telegramClient, adminId);
                setBotState.accept(chatId, BotState.NORMAL);
                break;
            case SEND_FILENAME:
                try {
                    scheduleFileParserService.parseSchedule(text);
                    messageService.sendMessage(chatId, "Группы в базе!", telegramClient);
                } catch (IOException e) {
                    messageService.sendMessage(chatId, "Ошибка при парсинге файла: " + e.getMessage(), telegramClient);
                }
                setBotState.accept(chatId, BotState.NORMAL);
                break;
        }
    }
}