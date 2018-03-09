package gruBot.telegram.bot;

import gruBot.telegram.firestore.Firestore;
import gruBot.telegram.logger.Logger;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.File;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.UserProfilePhotos;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

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
        this.firestore = new Firestore();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            try {
                long chatId = message.getChatId();
                String chatName = message.getChat().getTitle();
                String messageText = message.getText();
                String messageAuthor = message.getFrom().getUserName();

                String result = String.format("'%s' wrote to '%s': '%s'", messageAuthor, chatName, messageText);
                Logger.log(result, Logger.INFO);

                if (firestore.checkGroupExists(chatId)) {

                } else {
                    firestore.createNewGroup(update);
                }

                firestore.checkUserExistsInGroup(update, this);

                Matcher m = Pattern.compile(GruBotPatterns.announcement, Pattern.UNIX_LINES).matcher(message.getText());
                if(m.matches()) {
                    Logger.log("Found", Logger.INFO);
                }

                if (message.hasText() && message.getText().matches(GruBotPatterns.announcement)) {
                    firestore.createNewAnnouncement(update);
                    SendMessage sendMessage = new SendMessage()
                            .setText("Announcement created")
                            .setChatId(chatId);
                    execute(sendMessage);
                }
            } catch (Exception e) {
                Logger.log(e.getMessage(), Logger.ERROR);
            }
        }
    }

    public UserProfilePhotos getUserPhotos(GetUserProfilePhotos request) throws TelegramApiException{
        return execute(request);
    }

    public File getFileByRequest(GetFile request) throws TelegramApiException {
        return execute(request);
    }
}
