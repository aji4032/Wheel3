package marquee;

import logger.Log;
import logger.Logger;
import mmarquee.automation.AutomationException;
import mmarquee.automation.controls.AutomationBase;
import mmarquee.automation.controls.Window;

public class MarqueeWindow {
    private static final Logger log = Log.getLogger(MarqueeWindow.class);
    private final MarqueeDriver driver;
    private final Window window;
    private final String title;

    protected MarqueeWindow(MarqueeDriver driver, Window window, String title) {
        this.driver = driver;
        this.window = window;
        this.title = title;
    }

    public String title() {
        return title;
    }

    public MarqueeElement findElement(MarqueeBy by) {
        AutomationBase element;
        try {
            switch(by.type()) {
                case AUTOMATION_ID  -> element = window.getControlByAutomationId(by.locator());
                case CLASS_NAME     -> element = window.getControlByClassName(by.locator());
                case NAME           -> element = window.getControlByName(by.locator());
                default -> throw new IllegalStateException("Unexpected value: " + by.type());
            }
            return new MarqueeElement(driver, this, element, by);
        } catch (AutomationException e) {
            log.error("Failed to get element: {}", by.name());
            return null;
        }
    }

    public void closeWindow() {
        try {
            window.close();
            log.info("Close window: {}", title);
        } catch (AutomationException e) {
            log.error("Failed to close window: {}", title);
        }
    }

    public void focusWindow() {
        window.focus();
    }

    public void maximizeWindow() {
        try {
            window.maximize();
            log.info("Maximize window: {}", title);
        } catch (AutomationException e) {
            log.error("Failed to maximize window: {}", title);
        }
    }

    public void minimizeWindow() {
        try {
            window.minimize();
            log.info("Minimize window: {}", title);
        } catch (AutomationException e) {
            log.error("Failed to minimize window: {}", title);
        }
    }
}
