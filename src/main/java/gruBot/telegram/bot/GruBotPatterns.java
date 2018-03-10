package gruBot.telegram.bot;

public class GruBotPatterns {
    public static final String announcement = "^\\![^!\\r\\n]+\\!\\n.+$";
    public static final String announcementTitle = "^\\![^!\\r\\n]+\\!\\n";
    public static final String announcementText = "\\n.+$";

    public static final String vote = "^\\?[^?\\r\\n]+\\?\\n.*$"; //dotall, multiline
    public static final String voteTitle = "^\\?[^?\\r\\n]+\\?\\n"; //multiline
    public static final String voteText = "\\n.*"; //multiline
    public static final String voteOptionTextOnly = "\\d+\\.\\s+";
}
