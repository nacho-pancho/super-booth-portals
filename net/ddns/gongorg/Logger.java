package net.ddns.gongorg;

import java.util.logging.Level;

/**
 * Simple wrapper for the generic Java logging API. Basically just adds the name
 * of the plugin to every message.
 */
class Logger {

    private static final java.util.logging.Logger logger = java.util.logging.Logger
            .getLogger("Minecraft");

    private final String pluginName;
    boolean debugMode = false;

    Logger(String pluginName, boolean debug) {
        this.pluginName = pluginName;
        this.debugMode = debug;
    }

    /**
     * Log a warning message
     * 
     * @param message
     *            The message to log.
     */
    void warning(String message) {
        log(message, Level.WARNING);
    }

    /**
     * Log an info message
     * 
     * @param message
     *            The message to log.
     */
    void info(String message) {
        log(message, Level.INFO);
    }

    /**
     * Log a severe message
     * 
     * @param message
     *            The message to log.
     */
    void severe(String message) {
        log(message, Level.SEVERE);
    }

    /**
     * Log a debug message (if debugging is on.)
     * 
     * @param message
     *            The message to log.
     */
    void debug(String message) {
        if (this.debugMode)
            log(message, Level.INFO);
    }

    /**
     * Log a message
     * 
     * @param message
     *            message The message to log.
     * @param level
     *            The level of the message.
     */
    private void log(Object message, Level level) {
        logger.log(level, "[" + pluginName + "] " + message);
    }
}
