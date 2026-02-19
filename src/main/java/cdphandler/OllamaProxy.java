package cdphandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public class OllamaProxy implements InvocationHandler {

    private final ICdpDriver cdpDriver;

    public OllamaProxy(ICdpDriver cdpDriver) {
        this.cdpDriver = cdpDriver;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("findElement") || method.getName().equals("findElements")) {
            CdpBy by = (CdpBy) args[0];
            if (by.type() == CdpLocatorType.NATURAL_LANGUAGE) {
                String html = cdpDriver.getPageSource();
                String selector = OllamaUtility.getSelector(html, by.locator());
                CdpBy newBy = new CdpBy(by.name(), selector.startsWith("/") ? CdpLocatorType.XPATH : CdpLocatorType.CSS,
                        selector);
                args[0] = newBy;
            }
        }

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
