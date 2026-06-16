package logger;

import com.aventstack.chaintest.plugins.ChainTestListener;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.testng.Reporter;

@Plugin(name = "TestNgLogger", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class TestNgLogger extends AbstractAppender {
    protected TestNgLogger(String name, Filter filter, Layout<?> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    @PluginFactory
    public static TestNgLogger createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<?> layout) {
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new TestNgLogger(name, filter, layout, true, null);
    }

    @Override
    public void append(LogEvent event) {
        Layout<?> layout = getLayout();
        String formattedMessage = new String(layout.toByteArray(event));
        String htmlEscapedMessage = StringEscapeUtils.escapeHtml4(formattedMessage);
        Reporter.log(htmlEscapedMessage);
        ChainTestListener.log(htmlEscapedMessage);
    }
}
