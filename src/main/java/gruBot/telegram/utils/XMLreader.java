package gruBot.telegram.utils;

import gruBot.telegram.bot.GruBotConfig;
import gruBot.telegram.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class XMLreader {
    public static void readBotConfig() {
        try {
            Logger.log("Reading config...", Logger.INFO);
            File file = new File("./config/gruBot-telegram.xml");
            FileInputStream fileInput = new FileInputStream(file);
            Properties properties = new Properties();
            properties.loadFromXML(fileInput);
            fileInput.close();

            GruBotConfig.BOT_TOKEN = properties.getProperty("BOT_TOKEN");
            GruBotConfig.BOT_USERNAME = properties.getProperty("BOT_USERNAME");
            GruBotConfig.PROJECT_ID = properties.getProperty("PROJECT_ID");
        } catch (Exception e) {
            Logger.log(e.getMessage(), Logger.ERROR);
        }
    }
}