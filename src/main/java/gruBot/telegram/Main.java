package gruBot.telegram;

import com.petersamokhin.bots.sdk.clients.User;
import gruBot.telegram.bot.GruBotConfig;
import gruBot.telegram.bot.GruBotTelegram;
import gruBot.telegram.bot.GruBotVK;
import gruBot.telegram.firestore.Firestore;
import gruBot.telegram.logger.Logger;
import gruBot.telegram.utils.CustomExceptionHandler;
import gruBot.telegram.utils.XMLReader;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.log4j.varia.NullAppender;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class Main {
    public static void main(String[] args) {
        disableLog4jOutput();
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());
        Logger.log("Starting GruBot...", Logger.Type.INFO, Logger.Source.ALL);
        XMLReader.readBotConfig();

        Firestore firestore = new Firestore();

        startTelegramBot(firestore, args);
        startVkBot(firestore);
    }

    private static void startVkBot(Firestore firestore) {
        Logger.log("Initializing GruBot VK...", Logger.Type.INFO, Logger.Source.VK);
        GruBotVK gruBotVK = new GruBotVK(firestore, new User(GruBotConfig.VK_ACCESS_TOKEN));
        gruBotVK.start();
    }

    private static void startTelegramBot(Firestore firestore, String[] args) {
        Logger.log("Initializing GruBot Telegram...", Logger.Type.INFO, Logger.Source.TELEGRAM);
        try {
            DefaultBotOptions options = new DefaultBotOptions();

            if (args.length == 2) {
                Logger.log("Proxy settings found. Trying to connect...", Logger.Type.INFO, Logger.Source.TELEGRAM);
                HttpHost proxy = new HttpHost(args[0], Integer.valueOf(args[1]));
                RequestConfig config = RequestConfig.custom().setProxy(proxy).setConnectTimeout(5000).setSocketTimeout(5000).build();
                options.setRequestConfig(config);
            }

            ApiContextInitializer.init();
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi();

            telegramBotsApi.registerBot(new GruBotTelegram(options, firestore));
        } catch (TelegramApiException e) {
            Logger.log(e.getMessage(), Logger.Type.ERROR, Logger.Source.TELEGRAM);
        }
    }

    private static void disableLog4jOutput() {
        org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
        org.apache.log4j.Logger.getRootLogger().addAppender(new NullAppender());
    }
}
