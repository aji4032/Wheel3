package cdphandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CdpDriverProxy implements InvocationHandler {

    private final ICdpDriver cdpDriver;

    public CdpDriverProxy(ICdpDriver cdpDriver) {
        this.cdpDriver = cdpDriver;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("Calling driver method: " + method.getName() + " with args: " + Arrays.toString(args));
        Object result = method.invoke(cdpDriver, args);

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
