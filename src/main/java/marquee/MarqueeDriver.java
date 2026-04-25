package marquee;

import mmarquee.automation.AutomationException;
import mmarquee.automation.UIAutomation;
import mmarquee.automation.controls.Window;
import tools.Log;
import tools.Utilities;

import java.time.Duration;

public class MarqueeDriver {
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

            Log.info("Found window: " + title);
            return new MarqueeWindow(this, window, title);
        } catch (AutomationException e) {
            Log.fail(String.format("Failed to get window: %s", title));
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
