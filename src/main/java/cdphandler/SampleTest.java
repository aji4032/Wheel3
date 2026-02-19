package cdphandler;

public class SampleTest {

    public static void main(String[] args) {
        String ws = "ws://localhost:59251/devtools/page/05986F850454E700508B07A0F73915AD";
        try(ICdpDriver objCdpDriver = CdpHandler.createDriver(ws)) {
            objCdpDriver.get("https://www.google.com");
            ICdpElement searchBox = objCdpDriver.findElement(new CdpBy("im feeling lucky submit", CdpLocatorType.NATURAL_LANGUAGE, "I'm Feeling Lucky"));
//            searchBox.sendKeys("Howdy");
            searchBox.click();
            objCdpDriver.keyPress(CdpKey.Enter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
