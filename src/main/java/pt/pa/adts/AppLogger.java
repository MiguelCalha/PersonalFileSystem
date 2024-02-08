package pt.pa.adts;

import java.util.logging.*;
import java.io.IOException;

/**
 * Utilizada para registar as operações atómicas após efetuadas num ficheiro
 */
public class AppLogger {
    private static final Logger logger = Logger.getLogger(AppLogger.class.getName());

    static {
        try {
            String desktopPath = System.getProperty("user.home") + "/Desktop/app_logs.txt";
            FileHandler fileHandler = new FileHandler(desktopPath, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
            logger.setUseParentHandlers(false);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error initializing logger FileHandler", e);
        }
    }

    public static Logger getLogger() {
        return logger;
    }
}