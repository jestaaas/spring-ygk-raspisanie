package com.knzv.spring_ygk_schedule.processor;

import com.knzv.spring_ygk_schedule.enums.BotState;
import com.knzv.spring_ygk_schedule.repository.GroupRepository;
import com.knzv.spring_ygk_schedule.service.MessageService;
import com.knzv.spring_ygk_schedule.service.RegistrationService;
import com.knzv.spring_ygk_schedule.service.SelectGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import com.knzv.spring_ygk_schedule.entity.Group; // Импорт Group

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CallbackQueryProcessor {

    @Autowired
    private SelectGroupService selectGroupService;
    @Autowired
    private MessageService sendMessageService;
    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private GroupRepository groupRepository; // Инъекция GroupRepository

    public void processCallbackQuery(Update update, TelegramClient telegramClient, Map<Long, BotState> botStates) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        long messageId = update.getCallbackQuery().getMessage().getMessageId();
        long userId = update.getCallbackQuery().getMessage().getChat().getId();
        String username = update.getCallbackQuery().getMessage().getChat().getUserName();

        if (callbackData.equals("CHANGE_GROUP")) {
            handleChooseGroupCallback(chatId, messageId, telegramClient);
        } else if (callbackData.equals("CANCEL_SEND")) {
            handleCancelSendCallback(chatId, messageId, telegramClient, botStates);
        } else if (isDepartmentCallback(callbackData)) { // Новая логика для отделений
            handleDepartmentCallback(callbackData, chatId, messageId, telegramClient);
        } else if (callbackData.length() == 2) { // Логика для специальностей (префиксов)
            handleSpecialtyCallback(callbackData, chatId, messageId, telegramClient);
        } else if (callbackData.length() == 6) { // Логика для выбора группы
            handleGroupSelectionCallback(callbackData, userId, username, chatId, messageId, telegramClient);
        }
    }

    private void handleChooseGroupCallback(long chatId, long messageId, TelegramClient telegramClient) {
        InlineKeyboardMarkup replyMarkup = selectGroupService.getDepartmentButtons();
        EditMessageText new_message = EditMessageText.builder()
                .chatId(chatId)
                .messageId((int) messageId)
                .text("Выберите отделение")
                .replyMarkup(replyMarkup)
                .build();
        try {
            telegramClient.execute(new_message);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleCancelSendCallback(long chatId, long messageId, TelegramClient telegramClient, Map<Long, BotState> botStates) {
        setBotState(chatId, BotState.NORMAL, botStates);
        EditMessageText new_message = EditMessageText.builder()
                .chatId(chatId)
                .messageId((int) messageId)
                .text("Ввод сообщения отменен")
                .build();
        try {
            telegramClient.execute(new_message);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    // Вспомогательный метод для определения колбэков отделений
    private boolean isDepartmentCallback(String callbackData) {
        return callbackData.equals("ОИТ") || callbackData.equals("ОАР") ||
                callbackData.equals("СО") || callbackData.equals("ММО") ||
                callbackData.equals("ОЭП");
    }

    private void handleDepartmentCallback(String department, long chatId, long messageId, TelegramClient telegramClient) {
        String answer = "Выберите специальность";
        InlineKeyboardMarkup keyboardMarkup;

        switch (department) {
            case "ОИТ":
                keyboardMarkup = InlineKeyboardMarkup.builder()
                        .keyboardRow(new InlineKeyboardRow(
                                InlineKeyboardButton.builder().text("ИС").callbackData("ИС").build(),
                                InlineKeyboardButton.builder().text("СА").callbackData("СА").build(),
                                InlineKeyboardButton.builder().text("ИБ").callbackData("ИБ").build()))
                        .build();
                break;
            case "ОАР":
                keyboardMarkup = InlineKeyboardMarkup.builder()
                        .keyboardRow(new InlineKeyboardRow(
                                InlineKeyboardButton.builder().text("ДИ").callbackData("ДИ").build(),
                                InlineKeyboardButton.builder().text("АР").callbackData("АР").build(),
                                InlineKeyboardButton.builder().text("РК").callbackData("РК").build()))
                        .keyboardRow(new InlineKeyboardRow(
                                InlineKeyboardButton.builder().text("ГД").callbackData("ГД").build()))
                        .build();
                break;
            case "СО":
                keyboardMarkup = InlineKeyboardMarkup.builder()
                        .keyboardRow(new InlineKeyboardRow(
                                InlineKeyboardButton.builder().text("СТ").callbackData("СТ").build(),
                                InlineKeyboardButton.builder().text("СД").callbackData("СД").build(),
                                InlineKeyboardButton.builder().text("ЗИ").callbackData("ЗИ").build()))
                        .build();
                break;
            case "ММО":
                keyboardMarkup = InlineKeyboardMarkup.builder()
                        .keyboardRow(new InlineKeyboardRow(
                                InlineKeyboardButton.builder().text("МА").callbackData("МА").build(),
                                InlineKeyboardButton.builder().text("МО").callbackData("МО").build(),
                                InlineKeyboardButton.builder().text("ТТ").callbackData("ТТ").build()))
                        .keyboardRow(new InlineKeyboardRow(
                                InlineKeyboardButton.builder().text("МС").callbackData("МС").build(),
                                InlineKeyboardButton.builder().text("УД").callbackData("УД").build(),
                                InlineKeyboardButton.builder().text("ЗМ").callbackData("ЗМ").build()))
                        .build();
                break;
            case "ОЭП":
                keyboardMarkup = InlineKeyboardMarkup.builder()
                        .keyboardRow(new InlineKeyboardRow(
                                InlineKeyboardButton.builder().text("ЮР").callbackData("ЮР").build(),
                                InlineKeyboardButton.builder().text("ЮС").callbackData("ЮС").build(),
                                InlineKeyboardButton.builder().text("ТУ").callbackData("ТУ").build()))
                        .keyboardRow(new InlineKeyboardRow(
                                InlineKeyboardButton.builder().text("ЭК").callbackData("ЭК").build()))
                        .build();
                break;
            default:
                // Этого не должно произойти, но на всякий случай
                keyboardMarkup = InlineKeyboardMarkup.builder().build();
                break;
        }

        EditMessageText new_message = EditMessageText.builder()
                .chatId(chatId)
                .messageId((int) messageId)
                .text(answer)
                .replyMarkup(keyboardMarkup)
                .build();
        try {
            telegramClient.execute(new_message);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleSpecialtyCallback(String prefix, long chatId, long messageId, TelegramClient telegramClient) {
        List<Group> groups = groupRepository.findByPrefix(prefix);
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

        String answer = "Выберите группу " + prefix;
        EditMessageText new_message = EditMessageText.builder()
                .chatId(chatId)
                .messageId((int) messageId)
                .text(answer)
                .replyMarkup(keyboardMarkup)
                .build();
        try {
            telegramClient.execute(new_message);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleGroupSelectionCallback(String groupName, long userId, String username, long chatId, long messageId, TelegramClient telegramClient) {
        List<Group> groups = groupRepository.findAllByName(groupName); // findAllByName, если может быть несколько одинаковых имён, иначе findByName
        if (!groups.isEmpty() && groupName.equals(groups.get(0).getName())) { // Проверяем, что группа существует и имя совпадает
            registrationService.registerOrChangeGroup(userId, username, groupName);
            EditMessageText new_message = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId((int) messageId)
                    .text("Вы успешно вошли")
                    .build();
            SendMessage message = registrationService.attachKeyboard(chatId, groupName);

            try {
                telegramClient.execute(new_message);
            } catch (TelegramApiException e) {
                System.out.println(e.getMessage());
            }
            try {
                telegramClient.execute(message);
            } catch (TelegramApiException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void setBotState(Long chatId, BotState state, Map<Long, BotState> botStates) {
        botStates.put(chatId, state);
    }
}