package cdphandler;

import java.time.Duration;
import java.util.List;

public interface ICdpDriver extends AutoCloseable {
    void back();
    String captureScreenshot();
    void close();
    void closeBrowser();
    void closeTab();
    void closeWindow();
    ICdpElement findElement(CdpBy by);
    ICdpElement findElement(CdpBy by, Duration duration);
    List<ICdpElement> findElements(CdpBy by);
    List<ICdpElement> findElements(CdpBy by, Duration duration);
    void forward();
    void fullScreenWindow();
    void get(String url);
    CdpUtility getCdpUtility();
    int getCurrentModifierValue();
    String getCurrentUrl();
    Duration getDefaultTimeout();
    Duration getPageLoadTimeout();
    String getPageSource();
    Duration getPollingInterval();
    String getTitle();
    String getWindowHandle();
    List<String> getWindowHandles();
    CdpRect getWindowRect();
    boolean isElementPresent(CdpBy by);
    void keyDown(CdpKey key);
    void keyUp(CdpKey key);
    void keyPress(CdpKey key);
    void maximizeWindow();
    void minimizeWindow();
    void refresh();
    void sendKeys(String text);
    void setDefaultTimeout(Duration DEFAULT_TIMEOUT);
    void setPageLoadTimeout(Duration PAGE_LOAD_TIMEOUT);
    void setPollingInterval(Duration POLLING_INTERVAL);
    void setWindowRect(CdpRect windowRect);
    void sleep(Duration duration);
    void switchToWindow(String windowHandle);
}
