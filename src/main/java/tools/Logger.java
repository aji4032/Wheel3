package tools;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.testng.Assert;
import org.testng.Reporter;
import com.aventstack.chaintest.plugins.ChainTestListener;

/**
 * Custom Logger wrapper instance.
 * Logs to Log4j (retaining caller class, method, and line number via FQCN),
 * TestNG Reporter, and ChainTestListener.
 * Supports parameterized logging (e.g. log.info("User {} logged in", username)).
 */
public class Logger {
    private static final String FQCN = Logger.class.getName();
    private final ExtendedLogger logger;

    public Logger(String name) {
        this.logger = (ExtendedLogger) LogManager.getLogger(name);
    }

    public Logger(Class<?> clazz) {
        this.logger = (ExtendedLogger) LogManager.getLogger(clazz);
    }

    private void log(Level level, String format, Object... args) {
        if (logger.isEnabled(level)) {
            ParameterizedMessage msg = new ParameterizedMessage(format, args);
            logger.logIfEnabled(FQCN, level, null, msg, msg.getThrowable());
            String formatted = msg.getFormattedMessage();
            String prefix = "[" + level.name() + "] ";
            Reporter.log(prefix + formatted);
            ChainTestListener.log(prefix + formatted);
        }
    }

    public void info(String message) {
        log(Level.INFO, message);
    }

    public void info(String format, Object... args) {
        log(Level.INFO, format, args);
    }

    public void warn(String message) {
        log(Level.WARN, message);
    }

    public void warn(String format, Object... args) {
        log(Level.WARN, format, args);
    }

    public void fail(String message) {
        log(Level.ERROR, message);
        Assert.fail(message);
    }

    public void fail(String message, Throwable cause) {
        log(Level.ERROR, message, cause);
        Assert.fail(message + ": " + cause.getMessage(), cause);
    }

    public void fail(String format, Object... args) {
        ParameterizedMessage msg = new ParameterizedMessage(format, args);
        String formatted = msg.getFormattedMessage();
        
        if (logger.isEnabled(Level.ERROR)) {
            logger.logIfEnabled(FQCN, Level.ERROR, null, msg, msg.getThrowable());
        }
        
        Reporter.log("[FAIL] " + formatted);
        ChainTestListener.log("[FAIL] " + formatted);
        
        if (msg.getThrowable() != null) {
            Assert.fail(formatted, msg.getThrowable());
        } else {
            Assert.fail(formatted);
        }
    }
}
