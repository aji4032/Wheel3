package tools;

/**
 * Unified logging factory.
 * <p>
 * This class provides factory methods to obtain {@link Logger} instances.
 */
public class Log {

    private Log() {
    }

    /**
     * Returns a Logger instance named after the specified class.
     *
     * @param clazz the class to name the logger after
     * @return the Logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz);
    }

    /**
     * Returns a Logger instance with the specified name.
     *
     * @param name the name of the logger
     * @return the Logger instance
     */
    public static Logger getLogger(String name) {
        return new Logger(name);
    }
}
