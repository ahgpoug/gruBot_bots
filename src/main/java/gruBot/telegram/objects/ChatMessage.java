package gruBot.telegram.objects;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

public class ChatMessage {

    private String messageText;
    private long chatId;
    private long userId;
    private Timestamp dateCreated;

    public ChatMessage(String messageText, long chatId, long userId, Timestamp dateCreated) {
        this.messageText = messageText;
        this.chatId = chatId;
        this.userId = userId;
        this.dateCreated = dateCreated;
    }

    public String getMessageText() {
        return messageText;
    }

    public long getChatId() {
        return chatId;
    }

    public long getUserId() {
        return userId;
    }

    public Timestamp getDateCreated() {
        return dateCreated;
    }
}
