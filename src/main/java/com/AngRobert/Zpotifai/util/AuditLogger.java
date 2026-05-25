package com.AngRobert.Zpotifai.util;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.*;

public class AuditLogger {
    private static final Logger logger = Logger.getLogger(AuditLogger.class.getName());

    private AuditLogger() {}

    // this block runs exactly once when the class is first mentioned in the code.
    static {
        try {
            // ensures logs directory exists
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdir();
            }

            // appends to the csv file
            FileHandler fileHandler = new FileHandler("logs/audit_log.csv", true);
            
            // custom formatter for action, timestamp format (ISO-8601)
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    String action = record.getMessage();
                    String timestamp = LocalDateTime.now().toString();
                    return action + "," + timestamp + "\n";
                }
            });

            logger.addHandler(fileHandler);

            // disables console output
            logger.setUseParentHandlers(false);
            
        } catch (IOException e) {
            System.err.println("Could not initialize audit logger: " + e.getMessage());
        }
    }

    public static void log(String action) {
        logger.info(action);
    }
}
