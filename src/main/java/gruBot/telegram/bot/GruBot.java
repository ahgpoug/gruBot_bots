package gruBot.telegram.bot;

import gruBot.telegram.firestore.Firestore;
import gruBot.telegram.logger.Logger;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.File;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.UserProfilePhotos;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.HashMap;
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

                Matcher m = Pattern.compile(GruBotPatterns.announcement, Pattern.MULTILINE).matcher(message.getText());
                if(m.matches()) {
                    processAnnouncement(update);
                }
            } catch (Exception e) {
                Logger.log(e.getMessage(), Logger.ERROR);
            }
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

    private Message sendTextMessage(Update update, String text) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage()
                .setText(text)
                .setChatId(update.getMessage().getChatId());

        return execute(sendMessage);
    }

    public UserProfilePhotos getUserPhotos(GetUserProfilePhotos request) throws TelegramApiException{
        return execute(request);
    }

    public File getFileByRequest(GetFile request) throws TelegramApiException {
        return execute(request);
    }
}
