package com.knzv.spring_ygk_schedule;

import com.knzv.spring_ygk_schedule.entity.Group;
import com.knzv.spring_ygk_schedule.entity.User;
import com.knzv.spring_ygk_schedule.enums.BotState;
import com.knzv.spring_ygk_schedule.repository.GroupRepository;
import com.knzv.spring_ygk_schedule.repository.UserRepository;
import com.knzv.spring_ygk_schedule.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.util.*;

@Component
public class ScheduleBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    @Value("${app.token}")
    private String token;
    @Value("${app.admin_id}")
    private long adminId;
    @Autowired
    private MessageService sendMessageService;
    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private ScheduleWebParserService scheduleWebParserService;
    @Autowired
    private ScheduleFileParserService scheduleFileParserService;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SelectGroupService selectGroupService;
    @Autowired
    private SendToAllService sendToAllService;
    @Autowired
    private StatsService statsService;
    private final Map<Long, BotState> botStates = new HashMap<>();
    public ScheduleBot(@Value("${app.token}") String token) {
        telegramClient = new OkHttpTelegramClient(token);
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            BotState currentState = botStates.getOrDefault(chatId, BotState.NORMAL);

            switch (currentState) {
                case NORMAL:
                    if (text.equalsIgnoreCase("/start")) {
                        if (userRepository.findByUserId(update.getMessage().getChat().getId()).isEmpty()) {
                            sendMessageService.sendMessage(chatId, "Добро пожаловать! Это неофициальный бот для получения расписания в ЯГК.", telegramClient);
                            SendMessage msg = selectGroupService.changeGroup(update);

                            try {
                                telegramClient.execute(msg);
                            }
                            catch (TelegramApiException e) {
                                System.out.println(e.getMessage());
                            }
                        }
                        else {
                            sendMessageService.sendMessage(chatId, "Вы уже вошли", telegramClient);
                        }
                    }
                    else if (text.equalsIgnoreCase("Расписание")) {
                        String result = scheduleService.getSchedule(update);
                        sendMessageService.sendMessage(chatId, result, telegramClient);
                    }
                    else if (text.equalsIgnoreCase("/parse") && chatId == adminId) {
                        sendMessageService.sendMessage(chatId, "Введите название файла", telegramClient);

                        setBotState(chatId, BotState.SEND_FILENAME);
                    }
                    else if (text.equalsIgnoreCase("/stats") && chatId == adminId) {
                        long count = statsService.getCountUsers();

                        sendMessageService.sendMessage(chatId, "На данный момент ботом пользуется " + count + " пользователей", telegramClient);
                    }
                    else if (text.equalsIgnoreCase("/send") && chatId == adminId) {
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
                        }
                        catch (TelegramApiException e) {
                            System.out.println(e.getMessage());
                        }
                        setBotState(chatId, BotState.SEND_MESSAGE);
                    }

                    else if (text.equalsIgnoreCase("Группа")) {

                        if (userRepository.findByUserId(update.getMessage().getChat().getId()).isEmpty()) {
                            sendMessageService.sendMessage(chatId, "Выберите группу", telegramClient);
                            return;
                        }
                        User user = userRepository.findByUserId(update.getMessage().getChat().getId()).orElseThrow();
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
                        }
                        catch (TelegramApiException e) {
                            System.out.println(e.getMessage());
                        }

                    }
                    else if (text.equalsIgnoreCase("Знаменатель")) {
                        String message = scheduleService.getFullSchedule(update, text);
                        sendMessageService.sendMessage(chatId, message, telegramClient);
                    }
                    else if (text.equalsIgnoreCase("Числитель")) {
                        String message = scheduleService.getFullSchedule(update, text);
                        sendMessageService.sendMessage(chatId, message, telegramClient);
                    }
                    break;
                case SEND_MESSAGE:
                    sendToAllService.sendToAll(text, telegramClient, adminId);
                    setBotState(chatId, BotState.NORMAL);
                    break;
                case SEND_FILENAME:
                    try {
                        scheduleFileParserService.parseSchedule(text);
                        sendMessageService.sendMessage(chatId, "Группы в базе!", telegramClient);
                    }
                    catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                    setBotState(chatId, BotState.NORMAL);
                    break;
            }
        }
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long userId = update.getCallbackQuery().getMessage().getChat().getId();
            String username = update.getCallbackQuery().getMessage().getChat().getUserName();

            if (callbackData.equals("CHANGE_GROUP")) {
                InlineKeyboardMarkup replyMarkup = selectGroupService.getDepartmentButtons();

                EditMessageText new_message = EditMessageText.builder()
                        .chatId(chatId)
                        .messageId((int)messageId)
                        .text("Выберите отделение")
                        .replyMarkup(
                                replyMarkup
                        )
                        .build();
                try {
                    telegramClient.execute(new_message);
                } catch (TelegramApiException e) {
                    System.out.println(e.getMessage());
                }
            }
            else if (callbackData.equals("CANCEL_SEND")) {
                setBotState(chatId, BotState.NORMAL);

                EditMessageText new_message = EditMessageText.builder()
                        .chatId(chatId)
                        .messageId((int)messageId)
                        .text("Ввод сообщения отменен")
                        .build();
                try {
                    telegramClient.execute(new_message);
                } catch (TelegramApiException e) {
                    System.out.println(e.getMessage());
                }
            }
            else if (callbackData.equals("ОИТ")) {
                String answer = "Выберите специальность";
                EditMessageText new_message = EditMessageText.builder()
                        .chatId(chatId)
                        .messageId((int)messageId)
                        .text(answer)
                        .replyMarkup(
                                InlineKeyboardMarkup
                                        .builder()
                                        .keyboardRow(
                                                new InlineKeyboardRow(
                                                        InlineKeyboardButton.builder().text("ИС").callbackData("ИС").build(),
                                                        InlineKeyboardButton.builder().text("СА").callbackData("СА").build(),
                                                        InlineKeyboardButton.builder().text("ИБ").callbackData("ИБ").build()
                                                )
                                        )
                                        .build()
                        )
                        .build();
                try {
                    telegramClient.execute(new_message);
                } catch (TelegramApiException e) {
                    System.out.println(e.getMessage());
                }
            }
            else if (callbackData.equals("ОАР")) {
                String answer = "Выберите специальность";
                EditMessageText new_message = EditMessageText.builder()
                        .chatId(chatId)
                        .messageId((int)messageId)
                        .text(answer)
                        .replyMarkup(
                                InlineKeyboardMarkup
                                        .builder()
                                        .keyboardRow(
                                                new InlineKeyboardRow(
                                                        InlineKeyboardButton.builder().text("ДИ").callbackData("ДИ").build(),
                                                        InlineKeyboardButton.builder().text("АР").callbackData("АР").build(),
                                                        InlineKeyboardButton.builder().text("РК").callbackData("РК").build()
                                                )
                                        )
                                        .keyboardRow(
                                                new InlineKeyboardRow(
                                                        InlineKeyboardButton.builder().text("ГД").callbackData("ГД").build()
                                                )
                                        )
                                        .build()
                        )
                        .build();
                try {
                    telegramClient.execute(new_message);
                } catch (TelegramApiException e) {
                    System.out.println(e.getMessage());
                }
            }else if (callbackData.equals("СО")) {
                String answer = "Выберите специальность";
                EditMessageText new_message = EditMessageText.builder()
                        .chatId(chatId)
                        .messageId((int)messageId)
                        .text(answer)
                        .replyMarkup(
                                InlineKeyboardMarkup
                                        .builder()
                                        .keyboardRow(
                                                new InlineKeyboardRow(
                                                        InlineKeyboardButton.builder().text("СТ").callbackData("СТ").build(),
                                                        InlineKeyboardButton.builder().text("СД").callbackData("СД").build(),
                                                        InlineKeyboardButton.builder().text("ЗИ").callbackData("ЗИ").build()
                                                )
                                        )
                                        .build()
                        )
                        .build();
                try {
                    telegramClient.execute(new_message);
                } catch (TelegramApiException e) {
                    System.out.println(e.getMessage());
                }

            }else if (callbackData.equals("ММО")) {
                String answer = "Выберите специальность";
                EditMessageText new_message = EditMessageText.builder()
                        .chatId(chatId)
                        .messageId((int)messageId)
                        .text(answer)
                        .replyMarkup(
                                InlineKeyboardMarkup
                                        .builder()
                                        .keyboardRow(
                                                new InlineKeyboardRow(
                                                        InlineKeyboardButton.builder().text("МА").callbackData("МА").build(),
                                                        InlineKeyboardButton.builder().text("МО").callbackData("МО").build(),
                                                        InlineKeyboardButton.builder().text("ТТ").callbackData("ТТ").build()
                                                )
                                        )
                                        .keyboardRow(
                                                new InlineKeyboardRow(
                                                        InlineKeyboardButton.builder().text("МС").callbackData("МС").build(),
                                                        InlineKeyboardButton.builder().text("УД").callbackData("УД").build(),
                                                        InlineKeyboardButton.builder().text("ЗМ").callbackData("ЗМ").build()
                                                        )
                                        )
                                        .build()
                        )
                        .build();
                try {
                    telegramClient.execute(new_message);
                } catch (TelegramApiException e) {
                    System.out.println(e.getMessage());
                }

            }else if (callbackData.equals("ОЭП")) {
                String answer = "Выберите специальность";
                EditMessageText new_message = EditMessageText.builder()
                        .chatId(chatId)
                        .messageId((int)messageId)
                        .text(answer)
                        .replyMarkup(
                                InlineKeyboardMarkup
                                        .builder()
                                        .keyboardRow(
                                                new InlineKeyboardRow(
                                                        InlineKeyboardButton.builder().text("ЮР").callbackData("ЮР").build(),
                                                        InlineKeyboardButton.builder().text("ЮС").callbackData("ЮС").build(),
                                                        InlineKeyboardButton.builder().text("ТУ").callbackData("ТУ").build()
                                                )
                                        )
                                        .keyboardRow(
                                                new InlineKeyboardRow(
                                                        InlineKeyboardButton.builder().text("ЭК").callbackData("ЭК").build()
                                                )
                                        )
                                        .build()
                        )
                        .build();
                try {
                    telegramClient.execute(new_message);
                } catch (TelegramApiException e) {
                    System.out.println(e.getMessage());
                }
            }
            else if (callbackData.length() == 2){
                List<String> prefixesList = groupRepository.findDistinctPrefixes();

                for (var item : prefixesList) {
                    if (callbackData.equals(item)) {
                        List<Group> groups = groupRepository.findByPrefix(item);

                        InlineKeyboardMarkup keyboardMarkup = InlineKeyboardMarkup.builder().build();
                        List<InlineKeyboardRow> rows = new ArrayList<>();

                        InlineKeyboardRow row = new InlineKeyboardRow();
                        int count = 0;

                        for (Group group : groups) {
                            InlineKeyboardButton button = InlineKeyboardButton
                                    .builder()
                                    .text(group.getName())
                                    .callbackData(group.getName())
                                    .build();

                            row.add(button);
                            count++;

                            if (count == 3) {
                                rows.add(row);
                                row = new InlineKeyboardRow();
                                count = 0;
                            }
                        }

                        if (!row.isEmpty()) {
                            rows.add(row);
                        }

                        keyboardMarkup.setKeyboard(rows);

                        String answer = "Выберите группу " + item;
                        EditMessageText new_message = EditMessageText.builder()
                                .chatId(chatId)
                                .messageId((int)messageId)
                                .text(answer)
                                .replyMarkup(
                                        keyboardMarkup
                                )
                                .build();
                        try {
                            telegramClient.execute(new_message);
                        } catch (TelegramApiException e) {
                            System.out.println(e.getMessage());
                        }
                        break;
                    }
                }
            }
            else if (callbackData.length() == 6) {
                List<Group> groups = groupRepository.findAllByName(callbackData);
                for (var group : groups) {
                    if (callbackData.equals(group.getName())) {
                        registrationService.registerOrChangeGroup(userId,username, group.getName());
                        EditMessageText new_message = EditMessageText.builder()
                                .chatId(chatId)
                                .messageId((int)messageId)
                                .text("Вы успешно вошли")
                                .build();
                        SendMessage message = registrationService.attachKeyboard(chatId, group.getName());

                        try {
                            telegramClient.execute(new_message);
                        } catch (TelegramApiException e) {
                            System.out.println(e.getMessage());
                        }
                        try {
                            telegramClient.execute(message);
                        }
                        catch (TelegramApiException e) {
                            System.out.println(e.getMessage());
                        }
                        break;
                    }
                }
            }
        }
    }
    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    private void setBotState(Long chatId, BotState state) {
        botStates.put(chatId, state);
    }

}
