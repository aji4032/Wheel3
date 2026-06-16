package logger;

import com.aventstack.chaintest.plugins.ChainTestListener;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.testng.Assert;
import org.testng.Reporter;
import java.util.Base64;

import java.util.List;

public class Logger {
    private static final String name = Logger.class.getName();
    private final ExtendedLogger logger;
    List<Level> listOfFailingLevels = List.of(Level.FATAL, Level.ERROR);

    public Logger(String className) {
        this.logger = (ExtendedLogger) LogManager.getLogger(className);
    }

    public Logger(Class<?> clazz) {
        this.logger = (ExtendedLogger) LogManager.getLogger(clazz);
    }

    private void log(Level level, String message, Object... args) {
        ParameterizedMessage objParameterizedMessage = new ParameterizedMessage(message, args);
        logger.logIfEnabled(name, level, null, objParameterizedMessage, objParameterizedMessage.getThrowable());
        if (listOfFailingLevels.contains(level)) {
            Assert.fail(objParameterizedMessage.getFormattedMessage());
        }
    }

    public void fatal(String message, Object... args) {
        log(Level.FATAL, message, args);
    }

    public void error(String message, Throwable t) {
        log(Level.ERROR, message, t);
    }

    public void error(String message, Object... args) {
        log(Level.ERROR, message, args);
    }

    public void warn(String message, Object... args) {
        log(Level.WARN, message, args);
    }

    public void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    public void trace(String message, Object... args) {
        log(Level.TRACE, message, args);
    }

    public void visualFail(String message, byte[] expected, byte[] actual, byte[] diff) {
        log(Level.ERROR, message + ". \n\n\nExpected: {}\n\n\nActual: {}\n\n\nDiff: {}", expected, actual, diff);
    }
}