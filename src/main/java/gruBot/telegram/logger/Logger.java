package gruBot.telegram.logger;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    public static final String INFO = "INFO";
    public static final String WARNING = "WARNING";
    public static final String ERROR = "ERROR";

    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_WHITE = "\u001B[37m";


    public static void log(String message, String type) {
        try {
            PrintStream consoleOut = new PrintStream(System.out, true, "UTF-8");

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            String date = dateFormat.format(new Date());

            String color;
            switch (type) {
                case INFO:
                    color = ANSI_CYAN;
                    break;
                case WARNING:
                    color = ANSI_GREEN;
                    break;
                case ERROR:
                    color = ANSI_RED;
                    break;
                default:
                    color = ANSI_WHITE;
                    break;
            }

            String line = String.format("%s[%s] [%s] %s%s", color, type, date, message, ANSI_WHITE);
            consoleOut.println(line);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
