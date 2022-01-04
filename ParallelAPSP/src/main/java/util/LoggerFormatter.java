package util;

import java.util.Arrays;
import java.util.logging.*;

public class LoggerFormatter extends Formatter {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_CYAN = "\u001B[36m";

    @Override
    public String format(LogRecord logRecord) {
        String source = logRecord.getLoggerName();
        // If we for instance use a class-bound logger
        if (null != logRecord.getSourceClassName()) {
            source = logRecord.getSourceClassName();
        }

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
        logger.setUseParentHandlers(false);

        // make the logger print to console
        ConsoleHandler ch = new ConsoleHandler();
        ch.setFormatter(new LoggerFormatter());
        ch.setLevel(loggerLevel);
        logger.addHandler(ch);

        logger.setLevel(loggerLevel);
    }
}
