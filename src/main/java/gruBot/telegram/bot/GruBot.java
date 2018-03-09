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

                if (message.hasText() && message.getText().matches(GruBotPatterns.announcement)) {
                    SendMessage sendMessage = new SendMessage()
                            .setText("This is an announcement, dudeeeeeee!")
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
