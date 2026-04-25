package apps.calculator;

import marquee.MarqueeBy;
import marquee.MarqueeDriver;
import marquee.MarqueeWindow;
import tools.Log;

public class CalculatorApp {
    private final MarqueeDriver driver;
    private static final MarqueeBy NUM_0_BUTTON = MarqueeBy.ByAutomationId("0", "num0Button");
    private static final MarqueeBy NUM_1_BUTTON = MarqueeBy.ByAutomationId("1", "num1Button");
    private static final MarqueeBy NUM_2_BUTTON = MarqueeBy.ByAutomationId("2", "num2Button");
    private static final MarqueeBy NUM_3_BUTTON = MarqueeBy.ByAutomationId("3", "num3Button");
    private static final MarqueeBy NUM_4_BUTTON = MarqueeBy.ByAutomationId("4", "num4Button");
    private static final MarqueeBy NUM_5_BUTTON = MarqueeBy.ByAutomationId("5", "num5Button");
    private static final MarqueeBy NUM_6_BUTTON = MarqueeBy.ByAutomationId("6", "num6Button");
    private static final MarqueeBy NUM_7_BUTTON = MarqueeBy.ByAutomationId("7", "num7Button");
    private static final MarqueeBy NUM_8_BUTTON = MarqueeBy.ByAutomationId("8", "num8Button");
    private static final MarqueeBy NUM_9_BUTTON = MarqueeBy.ByAutomationId("9", "num9Button");

    private static final MarqueeBy PLUS_BUTTON      = MarqueeBy.ByAutomationId("+", "plusButton");
    private static final MarqueeBy MINUS_BUTTON     = MarqueeBy.ByAutomationId("-", "minusButton");
    private static final MarqueeBy MULTIPLY_BUTTON  = MarqueeBy.ByAutomationId("*", "multiplyButton");
    private static final MarqueeBy DIVIDE_BUTTON    = MarqueeBy.ByAutomationId("/", "divideButton");
    private static final MarqueeBy PERCENT_BUTTON   = MarqueeBy.ByAutomationId("%", "percentButton");
    private static final MarqueeBy EQUAL_BUTTON     = MarqueeBy.ByAutomationId("=", "equalButton");
    private static final MarqueeBy DECIMAL_BUTTON   = MarqueeBy.ByAutomationId(".", "decimalSeparatorButton");
    private static final MarqueeBy NEGATE_BUTTON    = MarqueeBy.ByAutomationId("+/-", "negateButton");

    private static final MarqueeBy CLEAR_ENTRY_BUTTON   = MarqueeBy.ByAutomationId("CE", "clearEntryButton");
    private static final MarqueeBy CLEAR_BUTTON         = MarqueeBy.ByAutomationId("C", "clearButton");
    private static final MarqueeBy BACKSPACE_BUTTON     = MarqueeBy.ByAutomationId("Bksp", "backSpaceButton");

    private static final MarqueeBy RESULT_AREA = MarqueeBy.ByAutomationId("Result Area", "CalculatorResults");

    private MarqueeWindow window = null;

    public CalculatorApp() {
        this.driver = MarqueeDriver.getInstance();
    }

    private MarqueeWindow getWindow() {
        if(window == null) {
            window = driver.getWindow("Calculator");
        }
        window.focusWindow();
        return window;
    }

    private void clickButton(MarqueeBy buttonBy) {
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
            Log.fail(String.format("Result: Expected = '%s'; Actual = '%s'", expectedResult, actualResult));
        Log.info(String.format("Verified result: '%s'", actualResult));
    }
}
