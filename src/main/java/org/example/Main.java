package org.example;

import com.google.api.services.sheets.v4.Sheets;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Main extends TelegramLongPollingBot {
    //откуда у тебя сурсы кода, ты вообще ахуел что ли?
    //Author - @kojewoflks
    static List<String> groups = new ArrayList<>();
    @Override
    public String getBotUsername() {
        return "wjhgwbot";
    }

    @Override
    public String getBotToken() {
        return "7338858631:AAG3_aphwuOWO6wGcmN9156mxuQ15fZsHKQ";
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();

        if (message != null && message.hasText()) {
            String text = message.getText();
            Long chatId = message.getChatId();
            if(!checkSubscription(update.getMessage().getFrom().getId(), chatId.toString())) {
                sendSubscriptionRequest(chatId);
                return;
            }
            if (text.equals("/start")) {
                sendInlineKeyboard(chatId);
            } else {
                new Thread(() -> {
                    try {
                        Sheets sheetsService = Services.createSheetsService();
                        List<List<Object>> schedule = Services.getSchedule(sheetsService, Services.SPREADSHEET_ID, Services.RANGE);
                        String result = "";
                        if(text.contains("/")) {
                            result = Services.printGroupSchedule(schedule, text);
                        } else {
                            result = Services.printTeacherSchedule(schedule, text);
                        }
                        Services.getAllGroups(schedule);
                        sendMessage(chatId, result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    public void sendSubscriptionRequest(long chatId) {
        InlineKeyboardButton subscribeButton = new InlineKeyboardButton();
        subscribeButton.setText("Подпишитесь на канал");
        subscribeButton.setUrl("https://t.me/etyuweew");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(subscribeButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row);

        keyboardMarkup.setKeyboard(rows);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Подпишитесь на наш канал, чтобы продолжить использовать бота.");
        sendMessage.setReplyMarkup(keyboardMarkup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private boolean checkSubscription(long userId, String chatId) {
        try {
            ChatMember chatMember = execute(new GetChatMember("-1002188160685", userId));

            if (chatMember.getStatus().equals("member") || chatMember.getStatus().equals("administrator") || chatMember.getStatus().equals("creator")) {
                return true;
            } else {
                return false;
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }
    private void sendInlineKeyboard(Long chatId) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        for (int i = 0; i < groups.size(); i++) {
            
            String buttonName = groups.get(i);
            KeyboardButton button = new KeyboardButton(buttonName);
            row.add(button);

            if ((i + 1) % 3 == 0 || i == groups.size() - 1) {
                keyboard.add(row);
                row = new KeyboardRow();
            }
        }

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Добро пожаловать! Данный бот создан для быстрого просмотра расписания занятий в УПК МЦК. Если вы студент, то напишите название вашей группы без пробелов или выберите вашу группу нажав на кнопку ниже. Если вы преподаватель, то введите свою фамилию. КОД НЕ ИДЕАЛЕН, ПРОСЬБА ПРИ ОБНАРУЖЕНИИ ОШИБОК СРАЗУ ПИСАТЬ МНЕ. Создатель - @kojewoflks");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        new Thread(() -> {
            try {
                Sheets sheetsService = Services.createSheetsService();
                List<List<Object>> schedule = Services.getSchedule(sheetsService, Services.SPREADSHEET_ID, Services.RANGE);
                groups = Services.getAllGroups(schedule);
                try {
                    botsApi.registerBot(new Main());
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    System.err.println(e.getMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
