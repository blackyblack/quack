package nrs.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Handle logging for the Nxt node server
 */
public final class Logger {

    /** Log event types */
    public static enum Event {
        MESSAGE, EXCEPTION
    }

    /** Log levels */
    public static enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    /** Our logger instance */
    private static final org.slf4j.Logger log;

    /** Enable stack traces */
    private static final boolean enableStackTraces;

    /** Enable log traceback */
    private static final boolean enableLogTraceback;

    /**
     * No constructor
     */
    private Logger() {}

    /**
     * Logger initialization
     *
     * The existing Java logging configuration will be used if the Java logger has already
     * been initialized.  Otherwise, we will configure our own log manager and log handlers.
     * The nxt/conf/logging-default.properties and nxt/conf/logging.properties configuration
     * files will be used.  Entries in logging.properties will override entries in
     * logging-default.properties.
     */
    static {
        try {
            boolean foundProperties = false;
            Properties loggingProperties = new Properties();
            try (InputStream is = ClassLoader.getSystemResourceAsStream("logging-default.properties")) {
                    if (is != null) {
                        loggingProperties.load(is);
                        foundProperties = true;
                    }
            }
            try (InputStream is = ClassLoader.getSystemResourceAsStream("logging.properties")) {
                    if (is != null) {
                        loggingProperties.load(is);
                        foundProperties = true;
                    }
            }
            if (foundProperties) {
                    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                    loggingProperties.store(outStream, "logging properties");
                    ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
                    java.util.logging.LogManager.getLogManager().readConfiguration(inStream);
                    inStream.close();
                    outStream.close();
            }
            BriefLogFormatter.init();
        } catch (IOException e) {
                throw new RuntimeException("Error loading logging properties", e);
        }
        log = org.slf4j.LoggerFactory.getLogger(blackyblack.Application.class);
        enableStackTraces = /*Application.getBooleanProperty("nxt.enableStackTraces")*/true;
        enableLogTraceback = /*Application.getBooleanProperty("nxt.enableLogTraceback")*/false;
    }

    /**
     * Log a message (map to INFO)
     *
     * @param       message             Message
     */
    public static void logMessage(String message) {
        doLog(Level.INFO, message, null);
    }

    /**
     * Log an exception (map to ERROR)
     *
     * @param       message             Message
     * @param       exc                 Exception
     */
    public static void logMessage(String message, Exception exc) {
        doLog(Level.ERROR, message, exc);
    }

    /**
     * Log an ERROR message
     *
     * @param       message             Message
     */
    public static void logErrorMessage(String message) {
        doLog(Level.ERROR, message, null);
    }

    /**
     * Log an ERROR exception
     *
     * @param       message             Message
     * @param       exc                 Exception
     */
    public static void logErrorMessage(String message, Exception exc) {
        doLog(Level.ERROR, message, exc);
    }

    /**
     * Log a WARNING message
     *
     * @param       message             Message
     */
    public static void logWarningMessage(String message) {
        doLog(Level.WARN, message, null);
    }

    /**
     * Log a WARNING exception
     *
     * @param       message             Message
     * @param       exc                 Exception
     */
    public static void logWarningMessage(String message, Exception exc) {
        doLog(Level.WARN, message, exc);
    }

    /**
     * Log an INFO message
     *
     * @param       message             Message
     */
    public static void logInfoMessage(String message) {
        doLog(Level.INFO, message, null);
    }

    /**
     * Log an INFO exception
     *
     * @param       message             Message
     * @param       exc                 Exception
     */
    public static void logInfoMessage(String message, Exception exc) {
        doLog(Level.INFO, message, exc);
    }

    /**
     * Log a debug message
     *
     * @param       message             Message
     */
    public static void logDebugMessage(String message) {
        doLog(Level.DEBUG, message, null);
    }

    /**
     * Log a debug exception
     *
     * @param       message             Message
     * @param       exc                 Exception
     */
    public static void logDebugMessage(String message, Exception exc) {
        doLog(Level.DEBUG, message, exc);
    }

    /**
     * Log the event
     *
     * @param       level               Level
     * @param       message             Message
     * @param       exc                 Exception
     */
    private static void doLog(Level level, String message, Exception exc) {
        String logMessage = message;
        Exception e = exc;
        //
        // Add caller class and method if enabled
        //
        if (enableLogTraceback) {
            StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
            String className = caller.getClassName();
            int index = className.lastIndexOf('.');
            if (index != -1)
                className = className.substring(index+1);
            logMessage = className + "." + caller.getMethodName() + ": " + logMessage;
        }
        //
        // Format the stack trace if enabled
        //
        if (e != null) {
            if (!enableStackTraces) {
                logMessage = logMessage + "\n" + exc.toString();
                e = null;
            }
        }
        //
        // Log the event
        //
        switch (level) {
            case DEBUG:
                log.debug(logMessage, e);
                break;
            case INFO:
                log.info(logMessage, e);
                break;
            case WARN:
                log.warn(logMessage, e);
                break;
            case ERROR:
                log.error(logMessage, e);
                break;
        }
    }
}
