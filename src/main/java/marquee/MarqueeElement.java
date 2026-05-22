package marquee;

import mmarquee.automation.AutomationException;
import mmarquee.automation.controls.AutomationBase;
import mmarquee.automation.controls.Button;
import mmarquee.automation.controls.Container;
import mmarquee.automation.controls.EditBox;
import tools.Log;
import tools.Logger;

@SuppressWarnings("unused")
public class MarqueeElement {
    private static final Logger log = Log.getLogger(MarqueeElement.class);
    private final MarqueeDriver driver;
    private final MarqueeWindow window;
    private final AutomationBase element;
    private final MarqueeBy by;

    protected MarqueeElement(MarqueeDriver driver, MarqueeWindow window, AutomationBase element, MarqueeBy by) {
        this.driver = driver;
        this.window = window;
        this.element = element;
        this.by = by;
    }

    public MarqueeElement findElement(MarqueeBy childBy) {
        AutomationBase child;
        try {
            Container container = (Container) element;
            switch(by.type()) {
                case AUTOMATION_ID  -> child = container.getControlByAutomationId(childBy.locator());
                case CLASS_NAME     -> child = container.getControlByClassName(childBy.locator());
                case NAME           -> child = container.getControlByName(childBy.locator());
                default -> throw new IllegalStateException("Unexpected value: " + childBy.type());
            }
            return new MarqueeElement(driver, window, child, childBy);
        } catch (AutomationException e) {
            log.fail(String.format("Failed to get element: '%s'", childBy.name()));
            return null;
        }
    }

    public void clickButton() {
        try {
            ((Button) element).click();
            log.info(String.format("Clicked button: '%s'", by.name()));
        } catch (AutomationException e) {
            log.fail(String.format("Failed to click button: '%s'", by.name()));
        }
    }

    private EditBox getEditBox() {
        return (EditBox) element;
    }

    public void setEditBoxValue(String value) {
        try {
            getEditBox().setValue(value);
            log.info(String.format("Setting '%s' edit box value to: '%s'", by.name(), value));
        } catch (AutomationException e) {
            log.fail(String.format("Failed to set '%s' edit box value to: '%s'", by.name(), value));
        }
    }

    public String getEditBoxValue() {
        try {
            return getEditBox().getValue();
        } catch (AutomationException e) {
            log.fail(String.format("Failed to get '%s' edit box value!", by.name()));
            return null;
        }
    }

    public String getText() {
        try {
            return element.getName();
        } catch (AutomationException e) {
            log.fail(String.format("Failed to get text for element: '%s'", by.name()));
            return null;
        }
    }
}
