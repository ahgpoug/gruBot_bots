package gruBot.telegram;

import gruBot.telegram.bot.GruBot;
import gruBot.telegram.logger.Logger;
import gruBot.telegram.utils.XMLReader;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.telegram.telegrambots.ApiContext;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class Main {
    public static void main(String[] args) {
        Logger.log("Starting GruBot...", Logger.INFO);
        XMLReader.readBotConfig();
        Logger.log("Initializing GruBot...", Logger.INFO);

        try {
            DefaultBotOptions options = new DefaultBotOptions();

            if (args.length == 2) {
                Logger.log("Proxy settings found. Trying to connect...", Logger.INFO);
                HttpHost proxy = new HttpHost(args[0], Integer.valueOf(args[1]));
                RequestConfig config = RequestConfig.custom().setProxy(proxy).setConnectTimeout(5000).setSocketTimeout(5000).build();
                options.setRequestConfig(config);
            }

            ApiContextInitializer.init();
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi();

            telegramBotsApi.registerBot(new GruBot(options));
        } catch (TelegramApiException e) {
            Logger.log(e.getMessage(), Logger.ERROR);
        }
    }
}
