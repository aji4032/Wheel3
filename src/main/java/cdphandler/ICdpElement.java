package cdphandler;

import java.time.Duration;
import java.util.List;

public interface ICdpElement {
    String captureScreenshot();
    void clear();
    void click();
    void doubleClick();
    void dragDrop(int xOffset, int yOffset);
    boolean equals(Object object);
    ICdpElement findElement(CdpBy by);
    ICdpElement findElement(CdpBy by, Duration duration);
    List<ICdpElement> findElements(CdpBy by);
    List<ICdpElement> findElements(CdpBy by, Duration duration);
    String getAttribute(String attributeName);
    CdpBy getBy();
    ICdpDriver getCdpDriver();
    CdpPoint getCenterLocation();
    String getCssValue(String propertyName);
    CdpPoint getLocation();
    CdpRect getRect();
    String getReferenceId();
    int getScrollHeight();
    int getScrollLeft();
    int getScrollTop();
    CdpDimension getSize();
    String getText();
    int hashCode();
    boolean isDisplayed();
    boolean isElementActionable();
    boolean isElementActionable(Duration timeout);
    boolean isElementObscured();
    boolean isElementPresent(CdpBy by);
    boolean isEnabled();
    boolean isSelected();
    void mouseMove();
    void mouseMove(int xOffset, int yOffset);
    void scrollBy(int x, int y);
    void scrollIntoView();
    void sendKeys(String text);
    String toString();
}
