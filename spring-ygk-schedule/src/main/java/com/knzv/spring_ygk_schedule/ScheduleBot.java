package com.knzv.spring_ygk_schedule;

import com.knzv.spring_ygk_schedule.enums.BotState;
import com.knzv.spring_ygk_schedule.processor.CallbackQueryProcessor;
import com.knzv.spring_ygk_schedule.processor.MessageProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class ScheduleBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    @Value("${app.token}")
    private String token;
    @Value("${app.admin_id}")
    private long adminId;

    @Autowired
    private MessageProcessor messageProcessor;
    @Autowired
    private CallbackQueryProcessor callbackQueryProcessor;

    private final Map<Long, BotState> botStates = new HashMap<>();

    public ScheduleBot(@Value("${app.token}") String token) {
        this.telegramClient = new OkHttpTelegramClient(token);
        System.out.println("ScheduleBot initialized with token."); // Лог инициализации
    }

    @Override
    public void consume(Update update) {
        Long chatId = getChatId(update); // Убедимся, что chatId не null
        if (chatId == null) {
            System.out.println("Received update without identifiable chat ID. Skipping.");
            return;
        }

        // Получаем текущее состояние для чата. Если нет, то NORMAL.
        BotState currentState = botStates.getOrDefault(chatId, BotState.NORMAL);

        System.out.println("Consumed update for chat: " + chatId + ", current state from map: " + currentState +
                ", hasMessage: " + update.hasMessage() + ", hasCallbackQuery: " + update.hasCallbackQuery() +
                (update.hasMessage() ? ", hasText: " + update.getMessage().hasText() + ", hasPhoto: " + update.getMessage().hasPhoto() : "")); // Подробный лог

        if (update.hasMessage()) {
            // **ИЗМЕНЕНИЕ ЗДЕСЬ**: Передаем сообщение в MessageProcessor независимо от наличия текста.
            // MessageProcessor сам разберется, это текст, фото или комбинация.
            messageProcessor.processMessage(update, currentState, telegramClient, botStates, adminId);
        } else if (update.hasCallbackQuery()) {
            // Для CallbackQueryProcessor состояние SEND_PHOTOS не актуально,
            // но мы все равно передаем текущее состояние бота, если оно нужно
            // для принятия решений внутри CallbackQueryProcessor.
            callbackQueryProcessor.processCallbackQuery(update, telegramClient, botStates);
        }
        // Здесь можно добавить обработку других типов Update, если потребуется.
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    private Long getChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        // Добавляем обработку для других типов Update, если они могут приходить без chatId в message/callback_query
        // Например, update.hasInlineQuery()
        return null; // Возвращаем null, если chatId не найден
    }
}