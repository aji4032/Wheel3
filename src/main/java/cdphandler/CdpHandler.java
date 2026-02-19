package cdphandler;

import java.lang.reflect.Proxy;

public class CdpHandler {

    public static ICdpDriver createDriver(String websocketDebuggerAddress) {
        CdpDriver cdpDriver = new CdpDriver(websocketDebuggerAddress);
        return (ICdpDriver) Proxy.newProxyInstance(
                ICdpDriver.class.getClassLoader(),
                new Class<?>[]{ICdpDriver.class},
                new OllamaProxy(cdpDriver)
        );
    }

    public static ICdpElement createElement(ICdpElement cdpElement) {
        return (ICdpElement) Proxy.newProxyInstance(
                ICdpElement.class.getClassLoader(),
                new Class<?>[]{ICdpElement.class},
                new CdpElementProxy(cdpElement)
        );
    }
}
