package gruBot.telegram.bot;

import gruBot.telegram.firestore.Firestore;
import gruBot.telegram.logger.Logger;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GruBot extends TelegramLongPollingBot {
    private Firestore firestore;

    @Override
    public String getBotUsername() {
        return GruBotConfig.BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return GruBotConfig.BOT_TOKEN;
    }

    public GruBot() {
        Logger.log("Initializing Firestore...", Logger.INFO);
        this.firestore = new Firestore();
        Logger.log("Started", Logger.INFO);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && (update.getMessage().getChat().isGroupChat() || update.getMessage().getChat().isSuperGroupChat())) {
            Message message = update.getMessage();
            try {
                processCommonMessage(message);

                if (!firestore.checkGroupExists(message.getChatId()))
                    firestore.createNewGroup(update);

                firestore.checkUserExistsInGroup(update, this);

                firestore.saveMessage(message);

                Matcher m = Pattern.compile(GruBotPatterns.announcement, Pattern.DOTALL).matcher(message.getText());
                if (m.matches()) {
                    if (true) //canUserPinMessages(message))
                        processAnnouncement(update);
                    else
                        sendTextMessage(update, "У пользователя недостаточно прав для создания объявлений");
                }

                m = Pattern.compile(GruBotPatterns.vote, Pattern.DOTALL).matcher(message.getText());
                if (m.matches()) {
                    if (true)//canUserPinMessages(message))
                        processVote(update);
                    else
                        sendTextMessage(update, "У пользователя недостаточно прав для создания голосований");
                }
            } catch (Exception e) {
                Logger.log(e.getMessage(), Logger.ERROR);
                e.printStackTrace();
            }
        } else if (update.hasCallbackQuery()) {

        }
    }

    private void processCommonMessage(Message message) {
        String chatName = message.getChat().getTitle();
        String messageText = message.getText();
        String messageAuthor = message.getFrom().getUserName();

        String result = String.format("'%s' wrote to '%s': '%s'", messageAuthor, chatName, messageText);
        Logger.log(result, Logger.INFO);
    }

    private void processAnnouncement(Update update) throws TelegramApiException {
        Message message = update.getMessage();
        Logger.log("Announcement is detected", Logger.INFO);
        HashMap<String, Object> announcement = firestore.createNewAnnouncement(update);
        String announcementText = String.format("Объявление:\r\n%s\r%s", announcement.get("desc").toString(), announcement.get("text").toString());

        Message announcementMessage = sendTextMessage(update, announcementText);

        if (message.getChat().isGroupChat())
            sendTextMessage(update, "Закреплять сообщения можно только в супер-чатах.\nИзмените группу для активации данного функционала");
        else {
            PinChatMessage pinChatMessage = new PinChatMessage()
                    .setChatId(message.getChatId())
                    .setMessageId(announcementMessage.getMessageId());
            execute(pinChatMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private void processVote(Update update) throws TelegramApiException {
        Message message = update.getMessage();
        Logger.log("Vote is detected", Logger.INFO);
        HashMap<String, Object> vote = firestore.createNewVote(update);
        StringBuilder options = new StringBuilder();
        for (Map.Entry<String, String> option : ((HashMap<String, String>) vote.get("voteOptions")).entrySet())
            options.append(option.getKey()).append(". ").append(option.getValue()).append("\r\n");

        String announcementText = String.format("Голосование:\r\n%s\r%s", vote.get("desc").toString(), options);


        SendMessage sendVoteMessage = new SendMessage()
                .setChatId(message.getChatId())
                .setText(announcementText)
                .setReplyMarkup(getVoteKeyboard((HashMap<String, String>) vote.get("voteOptions")));
        Message voteMessage = execute(sendVoteMessage);

        if (message.getChat().isGroupChat())
            sendTextMessage(update, "Закреплять сообщения можно только в супер-чатах.\nИзмените группу для активации данного функционала");
        else {
            PinChatMessage pinChatMessage = new PinChatMessage()
                    .setChatId(message.getChatId())
                    .setMessageId(voteMessage.getMessageId());
            execute(pinChatMessage);
        }
    }

    public Message sendTextMessage(Update update, String text) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage()
                .setText(text)
                .setChatId(update.getMessage().getChatId());

        return execute(sendMessage);
    }

    private InlineKeyboardMarkup getVoteKeyboard(HashMap<String, String> options) throws TelegramApiException {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        for (Map.Entry<String, String> option : options.entrySet())
            rowInline.add(new InlineKeyboardButton().setText(option.getValue()).setCallbackData("update_poll"));
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    private boolean canUserPinMessages(Message message) {
        try {
            GetChatAdministrators getChatAdministrators = new GetChatAdministrators()
                    .setChatId(message.getChatId());

            ArrayList<ChatMember> administrators = execute(getChatAdministrators);


            execute(getChatAdministrators).forEach(chatMember -> {
                if (chatMember.getUser().getId().equals(message.getFrom().getId()) && chatMember.getCanPinMessages())
                    Logger.log(chatMember.getCanPinMessages().toString(), Logger.WARNING);
            });


            return false;
        } catch (Exception e) {
            Logger.log(e.getMessage(), Logger.ERROR);
            e.printStackTrace();
            return false;
        }
    }

    public UserProfilePhotos getUserPhotos(GetUserProfilePhotos request) throws TelegramApiException {
        return execute(request);
    }

    public File getFileByRequest(GetFile request) throws TelegramApiException {
        return execute(request);
    }
}
