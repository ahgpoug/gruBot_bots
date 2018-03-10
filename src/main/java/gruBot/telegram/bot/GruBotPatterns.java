package gruBot.telegram.bot;

public class GruBotPatterns {
    public static final String announcement = "^\\![^!\\r\\n]+\\!\\n[^!\\r\\n]+$";
    public static final String announcementTitle = "^\\![^!\\r\\n]+\\!\\n";
    public static final String announcementText = "[^!\\r\\n]+$";
}
