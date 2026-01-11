package cdphandler;

public class CdpDriver {
    private final CdpUtility cdpUtility;

    public CdpDriver(String websocketDebuggerAddress) {
        this.cdpUtility = new CdpUtility(websocketDebuggerAddress);
    }
}
