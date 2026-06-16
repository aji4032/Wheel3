package w3c;

import w3c.server.W3CClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class W3CActions {
    public static final W3CActions INSTANCE = new W3CActions();

    private W3CActions() {
    }

    public static W3CActions getInstance() {
        return INSTANCE;
    }

    public void toggleCheckbox(W3CElement checkbox) {
        checkbox.clickButton();
    }

    public boolean isCheckboxChecked(W3CElement checkbox) {
        try {
            W3CClient client = W3CDriver.getClient();
            JsonNode res = client.execute("GET", "/session/" + checkbox.sessionId() + "/element/" + checkbox.elementId() + "/selected", null);
            return res.get("value").asBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    public void selectComboBoxItem(W3CElement comboBox, String item) {
        comboBox.setEditBoxValue(item);
    }

    public void expandComboBox(W3CElement comboBox) {
        try {
            W3CDriver.getClient().execute("POST", "/session/" + comboBox.sessionId() + "/element/" + comboBox.elementId() + "/expand", null);
        } catch (Exception e) {
            // ignore
        }
    }

    public void collapseComboBox(W3CElement comboBox) {
        try {
            W3CDriver.getClient().execute("POST", "/session/" + comboBox.sessionId() + "/element/" + comboBox.elementId() + "/collapse", null);
        } catch (Exception e) {
            // ignore
        }
    }

    public void selectRadioButton(W3CElement radioButton) {
        radioButton.clickButton();
    }

    public boolean isRadioButtonSelected(W3CElement radioButton) {
        try {
            W3CClient client = W3CDriver.getClient();
            JsonNode res = client.execute("GET", "/session/" + radioButton.sessionId() + "/element/" + radioButton.elementId() + "/selected", null);
            return res.get("value").asBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    public void selectTab(W3CElement tab, String page) {
        try {
            Map<String, String> body = Map.of("name", page);
            W3CDriver.getClient().execute("POST", "/session/" + tab.sessionId() + "/element/" + tab.elementId() + "/selectTabPage", body);
        } catch (Exception e) {
            // ignore
        }
    }

    public void clickMenuItem(W3CWindow window, W3CElement menuItem) {
        menuItem.clickButton();
    }

    public void selectListItem(W3CElement list, String item) {
        W3CElement child = list.findElement(W3CBy.ByName(item, item));
        if (child != null) {
            child.clickButton();
        }
    }

    public int getDataGridRowCount(W3CElement dataGrid) {
        try {
            W3CClient client = W3CDriver.getClient();
            JsonNode res = client.execute("GET", "/session/" + dataGrid.sessionId() + "/element/" + dataGrid.elementId() + "/rowcount", null);
            return res.get("value").asInt();
        } catch (Exception e) {
            return 0;
        }
    }

    public String getDataGridCellValue(W3CElement dataGrid, int row, int col) {
        try {
            W3CClient client = W3CDriver.getClient();
            JsonNode res = client.execute("GET", "/session/" + dataGrid.sessionId() + "/element/" + dataGrid.elementId() + "/cell/" + row + "/" + col, null);
            return res.get("value").asText();
        } catch (Exception e) {
            return null;
        }
    }
}
