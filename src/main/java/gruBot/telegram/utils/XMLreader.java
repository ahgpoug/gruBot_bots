package gruBot.telegram.utils;

import gruBot.telegram.bot.GruBotConfig;
import gruBot.telegram.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class XMLReader {
    public static void readBotConfig() {
        try {
            Logger.log("Reading config...", Logger.Type.INFO, Logger.Source.ALL);
            File file = new File("./config/gruBot-telegram.xml");
            FileInputStream fileInput = new FileInputStream(file);
            Properties properties = new Properties();
            properties.loadFromXML(fileInput);
            fileInput.close();

            GruBotConfig.BOT_TOKEN = properties.getProperty("BOT_TOKEN");
            GruBotConfig.BOT_USERNAME = properties.getProperty("BOT_USERNAME");
            GruBotConfig.PROJECT_ID = properties.getProperty("PROJECT_ID");
            GruBotConfig.VK_ACCESS_TOKEN = properties.getProperty("VK_ACCESS_TOKEN");
        } catch (Exception e) {
            Logger.log(e.getMessage(), Logger.Type.ERROR, Logger.Source.ALL);
        }
    }
}