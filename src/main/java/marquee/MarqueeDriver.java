package marquee;

import logger.Log;
import logger.Logger;
import mmarquee.automation.AutomationException;
import mmarquee.automation.UIAutomation;
import mmarquee.automation.controls.Window;
import tools.Utilities;

import java.time.Duration;

public class MarqueeDriver {
    private static final Logger log = Log.getLogger(MarqueeDriver.class);
    public static final MarqueeDriver INSTANCE = new MarqueeDriver();
    private final UIAutomation objUIAutomation;

    private MarqueeDriver() {
        objUIAutomation = UIAutomation.getInstance();
    }

    public static MarqueeDriver getInstance() {
        return INSTANCE;
    }

    protected UIAutomation getUIAutomation() {
        return objUIAutomation;
    }

    public MarqueeWindow getWindow(String title) {
        try {
            Window window = objUIAutomation.getWindow(title);
            boolean isWindowFound = waitForWindow(title, Duration.ofMinutes(1));
            if(!isWindowFound)
                throw new AutomationException("Window not found!");

            log.info("Found window: " + title);
            return new MarqueeWindow(this, window, title);
        } catch (AutomationException e) {
            log.error(String.format("Failed to get window: %s", title));
            return null;
        }
    }

    private boolean waitForWindow(String title, Duration timeout) {
        return Utilities.waitUntil(() -> {
            try {
                objUIAutomation.getDesktopWindow(title);
                return true;
            } catch (AutomationException e) {
                return false;
            }
        }, timeout);
    }
}
