package gruBot.telegram.utils;

import gruBot.telegram.bot.GruBotConfig;

public class Utils {
    public static String createUrlForTelegramFile(String filePath) {
        return String.format("https://api.telegram.org/file/bot%s/%s", GruBotConfig.BOT_TOKEN, filePath);
    }
}
