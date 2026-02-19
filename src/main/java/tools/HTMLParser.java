package tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

public class HTMLParser {
    public static String getAttributeValue(String url, String cssSelector, String attributeKey) {
        try {
            Document objDocument = Jsoup.connect(url).get();
            Element newHeadlines = objDocument.select(cssSelector).get(0);
            return newHeadlines.attr(attributeKey);
        } catch (IOException e) {
            return null;
        }
    }
}
