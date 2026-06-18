package apps.calculator;

import logger.Log;
import logger.Logger;
import w3c.W3CBy;
import w3c.W3CDriver;
import w3c.W3CWindow;

public class CalculatorApp {
    private static final Logger log = Log.getLogger(CalculatorApp.class);
    private final W3CDriver driver;
    private static final W3CBy NUM_0_BUTTON = W3CBy.ByAutomationId("0", "num0Button");
    private static final W3CBy NUM_1_BUTTON = W3CBy.ByAutomationId("1", "num1Button");
    private static final W3CBy NUM_2_BUTTON = W3CBy.ByAutomationId("2", "num2Button");
    private static final W3CBy NUM_3_BUTTON = W3CBy.ByAutomationId("3", "num3Button");
    private static final W3CBy NUM_4_BUTTON = W3CBy.ByAutomationId("4", "num4Button");
    private static final W3CBy NUM_5_BUTTON = W3CBy.ByAutomationId("5", "num5Button");
    private static final W3CBy NUM_6_BUTTON = W3CBy.ByAutomationId("6", "num6Button");
    private static final W3CBy NUM_7_BUTTON = W3CBy.ByAutomationId("7", "num7Button");
    private static final W3CBy NUM_8_BUTTON = W3CBy.ByAutomationId("8", "num8Button");
    private static final W3CBy NUM_9_BUTTON = W3CBy.ByAutomationId("9", "num9Button");

    private static final W3CBy PLUS_BUTTON      = W3CBy.ByAutomationId("+", "plusButton");
    private static final W3CBy MINUS_BUTTON     = W3CBy.ByAutomationId("-", "minusButton");
    private static final W3CBy MULTIPLY_BUTTON  = W3CBy.ByAutomationId("*", "multiplyButton");
    private static final W3CBy DIVIDE_BUTTON    = W3CBy.ByAutomationId("/", "divideButton");
    private static final W3CBy PERCENT_BUTTON   = W3CBy.ByAutomationId("%", "percentButton");
    private static final W3CBy EQUAL_BUTTON     = W3CBy.ByAutomationId("=", "equalButton");
    private static final W3CBy DECIMAL_BUTTON   = W3CBy.ByAutomationId(".", "decimalSeparatorButton");
    private static final W3CBy NEGATE_BUTTON    = W3CBy.ByAutomationId("+/-", "negateButton");

    private static final W3CBy CLEAR_ENTRY_BUTTON   = W3CBy.ByAutomationId("CE", "clearEntryButton");
    private static final W3CBy CLEAR_BUTTON         = W3CBy.ByAutomationId("C", "clearButton");
    private static final W3CBy BACKSPACE_BUTTON     = W3CBy.ByAutomationId("Bksp", "backSpaceButton");

    private static final W3CBy RESULT_AREA = W3CBy.ByXPath("Result Area", "//Text[@AutomationId='CalculatorResults']");

    private W3CWindow window = null;

    public CalculatorApp() {
        this.driver = W3CDriver.getInstance();
    }

    private W3CWindow getWindow() {
        if (window == null) {
            window = driver.getWindow("Calculator");
            if (window == null) {
                try {
                    log.info("Calculator window not found. Launching calc.exe from client...");
                    Runtime.getRuntime().exec("calc.exe");
                    for (int i = 0; i < 10; i++) {
                        Thread.sleep(500);
                        window = driver.getWindow("Calculator");
                        if (window != null) {
                            log.info("Successfully attached to Calculator window.");
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to auto-launch calc.exe on client side", e);
                }
            }
        }
        if (window != null) {
            window.focusWindow();
        } else {
            throw new RuntimeException("Failed to locate or launch Calculator window.");
        }
        return window;
    }

    private void clickButton(W3CBy buttonBy) {
        getWindow().findElement(buttonBy).clickButton();
    }

    public void click0Button() {
        clickButton(NUM_0_BUTTON);
    }

    public void click1Button() {
        clickButton(NUM_1_BUTTON);
    }

    public void click2Button() {
        clickButton(NUM_2_BUTTON);
    }

    public void click3Button() {
        clickButton(NUM_3_BUTTON);
    }

    public void click4Button() {
        clickButton(NUM_4_BUTTON);
    }

    public void click5Button() {
        clickButton(NUM_5_BUTTON);
    }

    public void click6Button() {
        clickButton(NUM_6_BUTTON);
    }

    public void click7Button() {
        clickButton(NUM_7_BUTTON);
    }

    public void click8Button() {
        clickButton(NUM_8_BUTTON);
    }

    public void click9Button() {
        clickButton(NUM_9_BUTTON);
    }

    public void clickPlusButton() {
        clickButton(PLUS_BUTTON);
    }

    public void clickMinusButton() {
        clickButton(MINUS_BUTTON);
    }

    public void clickMultiplyButton() {
        clickButton(MULTIPLY_BUTTON);
    }

    public void clickDivideButton() {
        clickButton(DIVIDE_BUTTON);
    }

    public void clickPercentButton() {
        clickButton(PERCENT_BUTTON);
    }

    public void clickEqualButton() {
        clickButton(EQUAL_BUTTON);
    }

    public void clickDecimalButton() {
        clickButton(DECIMAL_BUTTON);
    }

    public void clickNegateButton() {
        clickButton(NEGATE_BUTTON);
    }

    public void clickClearEntryButton() {
        clickButton(CLEAR_ENTRY_BUTTON);
    }

    public void clickClearButton() {
        clickButton(CLEAR_BUTTON);
    }

    public void clickBackspaceButton() {
        clickButton(BACKSPACE_BUTTON);
    }

    public String getResult() {
        return getWindow().findElement(RESULT_AREA).getText().replace("Display is ", "").trim();
    }

    public void verifyResult(String expectedResult) {
        String actualResult = getResult();
        boolean result = actualResult.equals(expectedResult);
        if(!result)
            log.error("Result: Expected = '{}'; Actual = '{}'", expectedResult, actualResult);
        log.info("Verified result: '{}'", actualResult);
    }
}
