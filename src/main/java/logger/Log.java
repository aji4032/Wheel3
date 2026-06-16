package logger;

public class Log {
    private Log(){}

    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz);
    }

    public static Logger getLogger(String className) {
        return new Logger(className);
    }
}
