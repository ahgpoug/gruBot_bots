package gruBot.telegram;

import gruBot.telegram.bot.GruBot;
import gruBot.telegram.logger.Logger;
import gruBot.telegram.utils.XMLreader;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class Main {
    public static void main(String[] args) {
        Logger.log("Starting GruBot...", Logger.INFO);
        XMLreader.readBotConfig();
        Logger.log("Initializing GruBot...", Logger.INFO);
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new GruBot());
        } catch (TelegramApiException e) {
            Logger.log(e.getMessage(), Logger.ERROR);
        }
    }
}
