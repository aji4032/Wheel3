package marquee;

import mmarquee.automation.AutomationException;
import mmarquee.automation.UIAutomation;
import mmarquee.automation.controls.*;
import tools.Utilities;

import java.time.Duration;
import java.util.regex.Pattern;
import java.util.List;

public class MarqueeActions {
    public static final MarqueeActions INSTANCE = new MarqueeActions();
    private final UIAutomation objUIAutomation;

    private MarqueeActions() {
        objUIAutomation = UIAutomation.getInstance();
    }

    public static MarqueeActions getInstance() {
        return INSTANCE;
    }

    public UIAutomation getUIAutomation() {
        return objUIAutomation;
    }

    public Window getWindow(Pattern pattern) {
        try {
            Window window = objUIAutomation.getDesktopWindow(pattern);
            return window;
        } catch (AutomationException e) {
            return null;
        }
    }

    public void toggleCheckbox(Container parent, String name) {
        try {
            CheckBox checkBox = parent.getCheckBox(name);
            checkBox.toggle();
        } catch (Exception e) {
            System.exit(0);
        }
    }

    public boolean isCheckboxChecked(Container parent, String name) {
        try {
            CheckBox checkBox = parent.getCheckBox(name);
            boolean check = checkBox.getToggleState().toString().equalsIgnoreCase("On");
            return check;
        } catch (Exception e) {
            System.exit(0);
            return false;
        }
    }

    public void selectComboBoxItem(Container parent, String name, String item) {
        try {
            ComboBox comboBox = parent.getComboBox(name);
            comboBox.setText(item);
        } catch (Exception e) {
            System.exit(0);
        }
    }

    public void expandComboBox(Container parent, String name) {
        try {
            ComboBox comboBox = parent.getComboBox(name);
            comboBox.expand();
        } catch (Exception e) {
            System.exit(0);
        }
    }

    public void collapseComboBox(Container parent, String name) {
        try {
            ComboBox comboBox = parent.getComboBox(name);
            comboBox.collapse();
        } catch (Exception e) {
            System.exit(0);
        }
    }

    public void selectRadioButton(Container parent, String name) {
        try {
            RadioButton radioButton = parent.getRadioButton(name);
            radioButton.select();
        } catch (Exception e) {
            System.exit(0);
        }
    }

    public boolean isRadioButtonSelected(Container parent, String name) {
        try {
            RadioButton radioButton = parent.getRadioButton(name);
            boolean selected = radioButton.isSelected();
            return selected;
        } catch (Exception e) {
            System.exit(0);
            return false;
        }
    }

    public void selectTab(Container parent, String tabName, String page) {
        try {
            Tab tab = parent.getTab(tabName);
            tab.selectTabPage(page);
        } catch (Exception e) {
            System.exit(0);
        }
    }

    public void clickMenuItem(Window window, String menuName) {
        try {
            MainMenu menu = window.getMainMenu();
            MenuItem menuItem = menu.getMenuItem(menuName);
            menuItem.click();
        } catch (Exception e) {
            System.exit(0);
        }
    }

    public TreeView getTreeView(Container parent, String name) {
        try {
            return parent.getTreeView(name);
        } catch (AutomationException e) {
            System.exit(0);
            return null;
        }
    }

    public void selectListItem(Container parent, String name, String item) {
        try {
            ListItem listItem = parent.getList(name).getItem(item);
            listItem.click();
        } catch (Exception e) {
            System.exit(0);
        }
    }

    public DataGrid getDataGrid(Container parent, String name) {
        try {
            return parent.getDataGrid(name);
        } catch (AutomationException e) {
            System.exit(0);
            return null;
        }
    }

    public int getDataGridRowCount(DataGrid dataGrid) {
        try {
            return dataGrid.rowCount();
        } catch (AutomationException e) {
            System.exit(0);
            return 0;
        }
    }

    public String getDataGridCellValue(DataGrid dataGrid, int row, int col) {
        try {
            DataGridCell cell = dataGrid.getItem(row, col);
            return cell.getValue();
        } catch (AutomationException e) {
            System.exit(0);
            return null;
        }
    }

    public boolean isControlPresent(Container parent, String name) {
        try {
            parent.getControlByName(name);
            return true;
        } catch (AutomationException e) {
            return false;
        }
    }

    public boolean waitForControl(Container parent, String name, Duration timeout) {
        return Utilities.waitUntil(() -> isControlPresent(parent, name), timeout);
    }

    public List<Window> getDesktopWindows() {
        try {
            return objUIAutomation.getDesktopWindows();
        } catch (AutomationException e) {
            return null;
        }
    }

    public AutomationBase getControlByAutomationId(Container parent, String automationId) {
        try {
            AutomationBase control = parent.getControlByAutomationId(automationId);
            return control;
        } catch (AutomationException e) {
            return null;
        }
    }
}
