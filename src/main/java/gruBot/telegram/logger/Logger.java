package gruBot.telegram.logger;

import org.apache.commons.lang3.StringUtils;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_WHITE = "\u001B[37m";

    public enum Type {
        INFO, WARNING, ERROR
    }

    public enum Source {
        VK, TELEGRAM, FIRESTORE, ALL
    }

    public static void log(String message, Type type, Source source) {
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

            String line = String.format("%s%s [%s] %s %s%s", color, fls(type), date, fls(source), message, ANSI_WHITE);
            consoleOut.println(line);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String fls(Enum enumerator) {
        return StringUtils.rightPad("[" + enumerator.toString() + "]", longestStringOfEnum(enumerator) + 2);
    }

    private static int longestStringOfEnum(Enum enumerator) {
        int maxSize = 0;

        if (enumerator instanceof Type) {
            for (Type value : Type.values()) {
                if (value.toString().length() > maxSize)
                    maxSize = value.toString().length();
            }
        } else if (enumerator instanceof Source) {
            for (Source value : Source.values()) {
                if (value.toString().length() > maxSize)
                    maxSize = value.toString().length();
            }
        }

        return maxSize;
    }
}
