package com.knzv.spring_ygk_schedule.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

@Service
public class SelectGroupService {
    public SendMessage changeGroup(Update update) {
        SendMessage sendMessage = SendMessage
                .builder()
                .chatId(update.getMessage().getChatId())
                .text("Выберите отделение")
                .replyMarkup(
                        InlineKeyboardMarkup
                                .builder()
                                .keyboardRow(
                                        new InlineKeyboardRow(
                                                InlineKeyboardButton
                                                        .builder()
                                                        .text("ОИТ")
                                                        .callbackData("ОИТ")
                                                        .build()
                                                ,
                                                InlineKeyboardButton
                                                        .builder()
                                                        .text("ОАР")
                                                        .callbackData("ОАР")
                                                        .build()
                                        )
                                )
                                .keyboardRow(
                                        new InlineKeyboardRow(
                                                InlineKeyboardButton
                                                        .builder()
                                                        .text("СО")
                                                        .callbackData("СО")
                                                        .build()
                                                ,
                                                InlineKeyboardButton
                                                        .builder()
                                                        .text("ММО")
                                                        .callbackData("ММО")
                                                        .build()
                                        )
                                )
                                .keyboardRow(
                                        new InlineKeyboardRow(
                                                InlineKeyboardButton
                                                        .builder()
                                                        .text("ОЭП")
                                                        .callbackData("ОЭП")
                                                        .build()
                                        )
                                )
                                .build()
                )
                .build();

        return sendMessage;
    }
    public InlineKeyboardMarkup getDepartmentButtons() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("ОИТ")
                                        .callbackData("ОИТ")
                                        .build()
                                ,
                                InlineKeyboardButton
                                        .builder()
                                        .text("ОАР")
                                        .callbackData("ОАР")
                                        .build()
                        )
                )
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("СО")
                                        .callbackData("СО")
                                        .build()
                                ,
                                InlineKeyboardButton
                                        .builder()
                                        .text("ММО")
                                        .callbackData("ММО")
                                        .build()
                        )
                )
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("ОЭП")
                                        .callbackData("ОЭП")
                                        .build()
                        )
                )
                .build();
    }
}
