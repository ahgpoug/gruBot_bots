package gruBot.telegram.bot;

public class GruBotPatterns {
    public static final String announcement = "^\\![^!\\r\\n]+\\!\\r\\n[^!\\r\\n]+$";
    public static final String announcementTitle = "^\\![^!\\r\\n]+\\!\\r\\n";
    public static final String announcementText = "\\r\\n[^!\\r\\n]+$";
}
