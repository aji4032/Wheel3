package apps.calculator;

import org.testng.annotations.Test;

public class CalculatorTest {

    @Test
    public void Test0Button() {
        CalculatorApp app = new CalculatorApp();
        app.clickClearEntryButton();
        app.click1Button();
        app.click0Button();
        app.click2Button();
        app.click3Button();
        app.click4Button();

        app.clickPlusButton();

        app.click5Button();
        app.click6Button();
        app.click7Button();
        app.click8Button();
        app.click9Button();

        app.clickEqualButton();
        app.verifyResult("67,023");
    }
}
