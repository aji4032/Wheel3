package cdphandler;

import tools.Log;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CdpElementProxy implements InvocationHandler {

    private final ICdpElement cdpElement;

    public CdpElementProxy(ICdpElement cdpElement) {
        this.cdpElement = cdpElement;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Log.info("Calling element method: " + method.getName() + " with args: " + Arrays.toString(args));
        Object result = method.invoke(cdpElement, args);

        if (result instanceof CdpElement) {
            return CdpHandler.createElement((CdpElement) result);
        }

        if (result instanceof List) {
            return ((List<?>) result).stream()
                    .filter(CdpElement.class::isInstance)
                    .map(e -> CdpHandler.createElement((CdpElement) e))
                    .collect(Collectors.toList());
        }

        return result;
    }
}
