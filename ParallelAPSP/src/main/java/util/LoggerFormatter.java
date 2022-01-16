package util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

public class LoggerFormatter extends Formatter {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_CYAN = "\u001B[36m";

    private static Map<String, ConsoleHandler> consoleHandlers = new HashMap<>();

    @Override
    public String format(LogRecord logRecord) {
        String messageString = formatMessage(logRecord);
        String format = "%s %s: %s\n";
        // use different colours depending on logging level
        if (Arrays.asList("FINEST", "FINER", "FINE").contains(logRecord.getLevel().toString())) {
            return String.format(format, ANSI_CYAN, logRecord.getLevel().getLocalizedName(), messageString + ANSI_RESET);
        } else if (Arrays.asList("INFO").contains(logRecord.getLevel().toString())) {
            return String.format(format, ANSI_YELLOW, logRecord.getLevel().getLocalizedName(), messageString + ANSI_RESET);
        } else if (Arrays.asList("WARNING", "SEVERE").contains(logRecord.getLevel().toString())) {
            return String.format(format, ANSI_RED, logRecord.getLevel().getLocalizedName(), messageString + ANSI_RESET);
        } else {
            return "";
        }
    }

    public static void setupLogger(Logger logger, Level loggerLevel) {
        // The logger has not been configured yet, but only do it once
        if (!LoggerFormatter.consoleHandlers.containsKey(logger.getName())) {
            logger.setUseParentHandlers(false);

            // make the logger print to console
            ConsoleHandler ch = new ConsoleHandler();
            ch.setFormatter(new LoggerFormatter());

            // We only add the handler once per logger
            LoggerFormatter.consoleHandlers.put(logger.getName(), ch);
            logger.addHandler(ch);
        }

        logger.setLevel(loggerLevel);
        ConsoleHandler ch = LoggerFormatter.consoleHandlers.get(logger.getName());
        ch.setLevel(loggerLevel);
    }
}
