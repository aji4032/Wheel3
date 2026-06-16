package w3c.server;

import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.IUnknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import logger.Log;
import logger.Logger;

import java.util.ArrayList;
import java.util.List;

public class UIA {
    private static final Logger log = Log.getLogger(UIA.class);

    // GUIDs
    public static final Guid.GUID CLSID_CUIAutomation = new Guid.GUID("{ff48dba4-60ef-4201-aa87-54103eef594e}");
    public static final Guid.IID IID_IUIAutomation = new Guid.IID("{30cbe57d-d9d0-452a-ab13-7ac5ac4825ee}");
    public static final Guid.IID IID_IUIAutomationElement = new Guid.IID("{d22108aa-8ac5-49a5-837b-37bbb3d7591e}");
    public static final Guid.IID IID_IUIAutomationElementArray = new Guid.IID("{14314595-b4bc-4055-95f2-204545747385}");

    // Pattern IDs
    public static final int UIA_InvokePatternId = 10000;
    public static final int UIA_ValuePatternId = 10002;
    public static final int UIA_ExpandCollapsePatternId = 10005;
    public static final int UIA_GridPatternId = 10006;
    public static final int UIA_WindowPatternId = 10009;
    public static final int UIA_SelectionItemPatternId = 10010;
    public static final int UIA_TogglePatternId = 10015;

    // Pattern IIDs
    public static final Guid.IID IID_IUIAutomationInvokePattern = new Guid.IID("{FB377FBE-8EA6-46D5-9C73-6499642D3059}");
    public static final Guid.IID IID_IUIAutomationValuePattern = new Guid.IID("{A94CD8B1-0844-4CD6-9D2D-640537AB39E9}");
    public static final Guid.IID IID_IUIAutomationWindowPattern = new Guid.IID("{0FAEF453-9208-43EF-BBB2-3B485177864F}");
    public static final Guid.IID IID_IUIAutomationTogglePattern = new Guid.IID("{94CF8058-9B8D-4AB9-8BFD-4CD0A33C8C70}");
    public static final Guid.IID IID_IUIAutomationSelectionItemPattern = new Guid.IID("{A8EFA66A-0FDA-421A-9194-38021F3578EA}");
    public static final Guid.IID IID_IUIAutomationExpandCollapsePattern = new Guid.IID("{619BE086-1F4E-4EE4-BAFA-210128738730}");
    public static final Guid.IID IID_IUIAutomationGridPattern = new Guid.IID("{414c3cdc-856b-4f5b-8538-3131c6302550}");

    // Property IDs
    public static final int UIA_NamePropertyId = 30005;
    public static final int UIA_AutomationIdPropertyId = 30011;
    public static final int UIA_ClassNamePropertyId = 30012;

    // TreeScope Constants
    public static final int TreeScope_Element = 1;
    public static final int TreeScope_Children = 2;
    public static final int TreeScope_Descendants = 4;
    public static final int TreeScope_Subtree = 7;

    private static Pointer uiaPointer;
    private static Pointer[] uiaVTable;

    static {
        // Initialize COM on class load
        try {
            Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED);
            PointerByReference pbr = new PointerByReference();
            WinNT.HRESULT hr = Ole32.INSTANCE.CoCreateInstance(
                CLSID_CUIAutomation,
                null,
                WTypes.CLSCTX_INPROC_SERVER,
                IID_IUIAutomation,
                pbr
            );
            if (hr.intValue() >= 0 && pbr.getValue() != null) {
                uiaPointer = pbr.getValue();
                Pointer vTablePointer = uiaPointer.getPointer(0);
                uiaVTable = new Pointer[58];
                vTablePointer.read(0, uiaVTable, 0, uiaVTable.length);
                log.info("Direct UIAutomation initialized successfully.");
            } else {
                log.error("Failed to initialize CoCreateInstance for UIAutomation. HR: " + hr);
            }
        } catch (Exception e) {
            log.error("Exception during direct UIAutomation initialization", e);
        }
    }

    public static Pointer getRootElement() {
        if (uiaPointer == null) return null;
        PointerByReference rootRef = new PointerByReference();
        Function f = Function.getFunction(uiaVTable[5], Function.ALT_CONVENTION); // getRootElement = 5
        int res = f.invokeInt(new Object[]{uiaPointer, rootRef});
        if (res == 0) {
            return queryInterface(rootRef.getValue(), IID_IUIAutomationElement);
        }
        return null;
    }

    public static Pointer getElementFromHandle(WinDef.HWND hwnd) {
        if (uiaPointer == null) return null;
        PointerByReference elemRef = new PointerByReference();
        Function f = Function.getFunction(uiaVTable[6], Function.ALT_CONVENTION); // getElementFromHandle = 6
        int res = f.invokeInt(new Object[]{uiaPointer, hwnd, elemRef});
        if (res == 0) {
            return queryInterface(elemRef.getValue(), IID_IUIAutomationElement);
        }
        return null;
    }

    // Helper for QueryInterface
    public static Pointer queryInterface(Pointer unknownPointer, Guid.IID iid) {
        if (unknownPointer == null) return null;
        Pointer vTablePointer = unknownPointer.getPointer(0);
        Pointer[] vTable = new Pointer[3];
        vTablePointer.read(0, vTable, 0, 3);
        PointerByReference outRef = new PointerByReference();
        Function f = Function.getFunction(vTable[0], Function.ALT_CONVENTION); // QueryInterface = 0
        int res = f.invokeInt(new Object[]{unknownPointer, new Guid.REFIID(iid), outRef});
        if (res >= 0) {
            return outRef.getValue();
        }
        return null;
    }

    // Helper to Release a COM pointer
    public static void release(Pointer pointer) {
        if (pointer == null) return;
        try {
            Pointer vTablePointer = pointer.getPointer(0);
            Pointer[] vTable = new Pointer[3];
            vTablePointer.read(0, vTable, 0, 3);
            Function f = Function.getFunction(vTable[2], Function.ALT_CONVENTION); // Release = 2
            f.invokeInt(new Object[]{pointer});
        } catch (Exception e) {
            log.trace("Error releasing pointer: " + e.getMessage());
        }
    }

    public static Pointer createPropertyCondition(int propertyId, String value) {
        if (uiaPointer == null) return null;
        Variant.VARIANT.ByValue variant = new Variant.VARIANT.ByValue();
        variant.setValue(Variant.VT_BSTR, new WTypes.BSTR(value));

        PointerByReference condRef = new PointerByReference();
        Function f = Function.getFunction(uiaVTable[23], Function.ALT_CONVENTION); // createPropertyCondition = 23
        int res = f.invokeInt(new Object[]{uiaPointer, propertyId, variant, condRef});
        if (res == 0) {
            return condRef.getValue();
        }
        return null;
    }

    private static Pointer[] getElementVTable(Pointer elementPointer) {
        Pointer vTablePointer = elementPointer.getPointer(0);
        Pointer[] vTable = new Pointer[94];
        vTablePointer.read(0, vTable, 0, vTable.length);
        return vTable;
    }

    public static Pointer findFirst(Pointer parentElement, int scope, Pointer condition) {
        if (parentElement == null || condition == null) return null;
        Pointer[] vTable = getElementVTable(parentElement);
        PointerByReference outRef = new PointerByReference();
        Function f = Function.getFunction(vTable[5], Function.ALT_CONVENTION); // findFirst = 5
        int res = f.invokeInt(new Object[]{parentElement, scope, condition, outRef});
        if (res == 0 && outRef.getValue() != null) {
            return queryInterface(outRef.getValue(), IID_IUIAutomationElement);
        }
        return null;
    }

    public static List<Pointer> findAll(Pointer parentElement, int scope, Pointer condition) {
        List<Pointer> results = new ArrayList<>();
        if (parentElement == null || condition == null) return results;
        Pointer[] vTable = getElementVTable(parentElement);
        PointerByReference outRef = new PointerByReference();
        Function f = Function.getFunction(vTable[6], Function.ALT_CONVENTION); // findAll = 6
        int res = f.invokeInt(new Object[]{parentElement, scope, condition, outRef});
        if (res == 0 && outRef.getValue() != null) {
            Pointer arrayPointer = outRef.getValue();
            Pointer arrayVTablePointer = arrayPointer.getPointer(0);
            Pointer[] arrayVTable = new Pointer[5];
            arrayVTablePointer.read(0, arrayVTable, 0, 5);

            IntByReference lenRef = new IntByReference();
            Function lenFunc = Function.getFunction(arrayVTable[3], Function.ALT_CONVENTION); // getLength = 3
            lenFunc.invokeInt(new Object[]{arrayPointer, lenRef});

            int len = lenRef.getValue();
            Function getElemFunc = Function.getFunction(arrayVTable[4], Function.ALT_CONVENTION); // getElement = 4
            for (int i = 0; i < len; i++) {
                PointerByReference elemRef = new PointerByReference();
                getElemFunc.invokeInt(new Object[]{arrayPointer, i, elemRef});
                if (elemRef.getValue() != null) {
                    Pointer elem = queryInterface(elemRef.getValue(), IID_IUIAutomationElement);
                    if (elem != null) {
                        results.add(elem);
                    }
                }
            }
            release(arrayPointer);
        }
        return results;
    }

    public static String getElementName(Pointer element) {
        if (element == null) return "";
        Pointer[] vTable = getElementVTable(element);
        PointerByReference sr = new PointerByReference();
        Function f = Function.getFunction(vTable[23], Function.ALT_CONVENTION); // getCurrentName = 23
        int res = f.invokeInt(new Object[]{element, sr});
        if (res == 0 && sr.getValue() != null) {
            return sr.getValue().getWideString(0);
        }
        return "";
    }

    public static String getElementClassName(Pointer element) {
        if (element == null) return "";
        Pointer[] vTable = getElementVTable(element);
        PointerByReference sr = new PointerByReference();
        Function f = Function.getFunction(vTable[30], Function.ALT_CONVENTION); // getCurrentClassName = 30
        int res = f.invokeInt(new Object[]{element, sr});
        if (res == 0 && sr.getValue() != null) {
            return sr.getValue().getWideString(0);
        }
        return "";
    }

    public static String getElementAutomationId(Pointer element) {
        if (element == null) return "";
        Pointer[] vTable = getElementVTable(element);
        PointerByReference sr = new PointerByReference();
        Function f = Function.getFunction(vTable[29], Function.ALT_CONVENTION); // getCurrentAutomationId = 29
        int res = f.invokeInt(new Object[]{element, sr});
        if (res == 0 && sr.getValue() != null) {
            return sr.getValue().getWideString(0);
        }
        return "";
    }

    public static void setFocus(Pointer element) {
        if (element == null) return;
        Pointer[] vTable = getElementVTable(element);
        Function f = Function.getFunction(vTable[3], Function.ALT_CONVENTION); // setFocus = 3
        f.invokeInt(new Object[]{element});
    }

    public static Pointer getPattern(Pointer element, int patternId, Guid.IID patternIID) {
        if (element == null) return null;
        Pointer[] vTable = getElementVTable(element);
        PointerByReference pbr = new PointerByReference();
        Function f = Function.getFunction(vTable[16], Function.ALT_CONVENTION); // getCurrentPattern = 16
        int res = f.invokeInt(new Object[]{element, patternId, pbr});
        if (res == 0 && pbr.getValue() != null) {
            return queryInterface(pbr.getValue(), patternIID);
        }
        return null;
    }

    // Pattern invokers
    public static void invoke(Pointer element) {
        Pointer pattern = getPattern(element, UIA_InvokePatternId, IID_IUIAutomationInvokePattern);
        if (pattern != null) {
            Pointer vTablePointer = pattern.getPointer(0);
            Pointer[] vTable = new Pointer[4];
            vTablePointer.read(0, vTable, 0, 4);
            Function f = Function.getFunction(vTable[3], Function.ALT_CONVENTION); // invoke = 3
            f.invokeInt(new Object[]{pattern});
            release(pattern);
        }
    }

    public static void setValue(Pointer element, String value) {
        Pointer pattern = getPattern(element, UIA_ValuePatternId, IID_IUIAutomationValuePattern);
        if (pattern != null) {
            Pointer vTablePointer = pattern.getPointer(0);
            Pointer[] vTable = new Pointer[8];
            vTablePointer.read(0, vTable, 0, 8);
            Function f = Function.getFunction(vTable[3], Function.ALT_CONVENTION); // setValue = 3
            f.invokeInt(new Object[]{pattern, new WTypes.BSTR(value)});
            release(pattern);
        }
    }

    public static String getValue(Pointer element) {
        Pointer pattern = getPattern(element, UIA_ValuePatternId, IID_IUIAutomationValuePattern);
        if (pattern != null) {
            Pointer vTablePointer = pattern.getPointer(0);
            Pointer[] vTable = new Pointer[8];
            vTablePointer.read(0, vTable, 0, 8);
            PointerByReference sr = new PointerByReference();
            Function f = Function.getFunction(vTable[4], Function.ALT_CONVENTION); // getValue = 4
            int res = f.invokeInt(new Object[]{pattern, sr});
            String val = "";
            if (res == 0 && sr.getValue() != null) {
                val = sr.getValue().getWideString(0);
            }
            release(pattern);
            return val;
        }
        return "";
    }

    public static void closeWindow(Pointer element) {
        Pointer pattern = getPattern(element, UIA_WindowPatternId, IID_IUIAutomationWindowPattern);
        if (pattern != null) {
            Pointer vTablePointer = pattern.getPointer(0);
            Pointer[] vTable = new Pointer[18];
            vTablePointer.read(0, vTable, 0, 18);
            Function f = Function.getFunction(vTable[3], Function.ALT_CONVENTION); // close = 3
            f.invokeInt(new Object[]{pattern});
            release(pattern);
        }
    }

    public static void setWindowVisualState(Pointer element, int state) {
        Pointer pattern = getPattern(element, UIA_WindowPatternId, IID_IUIAutomationWindowPattern);
        if (pattern != null) {
            Pointer vTablePointer = pattern.getPointer(0);
            Pointer[] vTable = new Pointer[18];
            vTablePointer.read(0, vTable, 0, 18);
            Function f = Function.getFunction(vTable[5], Function.ALT_CONVENTION); // setWindowVisualState = 5
            f.invokeInt(new Object[]{pattern, state});
            release(pattern);
        }
    }

    public static void toggle(Pointer element) {
        Pointer pattern = getPattern(element, UIA_TogglePatternId, IID_IUIAutomationTogglePattern);
        if (pattern != null) {
            Pointer vTablePointer = pattern.getPointer(0);
            Pointer[] vTable = new Pointer[8];
            vTablePointer.read(0, vTable, 0, 8);
            Function f = Function.getFunction(vTable[3], Function.ALT_CONVENTION); // toggle = 3
            f.invokeInt(new Object[]{pattern});
            release(pattern);
        }
    }

    public static boolean getToggleState(Pointer element) {
        Pointer pattern = getPattern(element, UIA_TogglePatternId, IID_IUIAutomationTogglePattern);
        if (pattern != null) {
            Pointer vTablePointer = pattern.getPointer(0);
            Pointer[] vTable = new Pointer[8];
            vTablePointer.read(0, vTable, 0, 8);
            IntByReference ibr = new IntByReference();
            Function f = Function.getFunction(vTable[4], Function.ALT_CONVENTION); // getCurrentToggleState = 4
            int res = f.invokeInt(new Object[]{pattern, ibr});
            boolean state = false;
            if (res == 0) {
                state = ibr.getValue() == 1;
            }
            release(pattern);
            return state;
        }
        return false;
    }

    public static void selectItem(Pointer element) {
        Pointer pattern = getPattern(element, UIA_SelectionItemPatternId, IID_IUIAutomationSelectionItemPattern);
        if (pattern != null) {
            Pointer vTablePointer = pattern.getPointer(0);
            Pointer[] vTable = new Pointer[8];
            vTablePointer.read(0, vTable, 0, 8);
            Function f = Function.getFunction(vTable[3], Function.ALT_CONVENTION); // select = 3
            f.invokeInt(new Object[]{pattern});
            release(pattern);
        }
    }

    public static boolean isItemSelected(Pointer element) {
        Pointer pattern = getPattern(element, UIA_SelectionItemPatternId, IID_IUIAutomationSelectionItemPattern);
        if (pattern != null) {
            Pointer vTablePointer = pattern.getPointer(0);
            Pointer[] vTable = new Pointer[8];
            vTablePointer.read(0, vTable, 0, 8);
            IntByReference ibr = new IntByReference();
            Function f = Function.getFunction(vTable[6], Function.ALT_CONVENTION); // getCurrentIsSelected = 6
            int res = f.invokeInt(new Object[]{pattern, ibr});
            boolean selected = false;
            if (res == 0) {
                selected = ibr.getValue() != 0;
            }
            release(pattern);
            return selected;
        }
        return false;
    }

    public static void expand(Pointer element) {
        Pointer pattern = getPattern(element, UIA_ExpandCollapsePatternId, IID_IUIAutomationExpandCollapsePattern);
        if (pattern != null) {
            Pointer vTablePointer = pattern.getPointer(0);
            Pointer[] vTable = new Pointer[6];
            vTablePointer.read(0, vTable, 0, 6);
            Function f = Function.getFunction(vTable[3], Function.ALT_CONVENTION); // expand = 3
            f.invokeInt(new Object[]{pattern});
            release(pattern);
        }
    }

    public static void collapse(Pointer element) {
        Pointer pattern = getPattern(element, UIA_ExpandCollapsePatternId, IID_IUIAutomationExpandCollapsePattern);
        if (pattern != null) {
            Pointer vTablePointer = pattern.getPointer(0);
            Pointer[] vTable = new Pointer[6];
            vTablePointer.read(0, vTable, 0, 6);
            Function f = Function.getFunction(vTable[4], Function.ALT_CONVENTION); // collapse = 4
            f.invokeInt(new Object[]{pattern});
            release(pattern);
        }
    }

    public static int getGridRowCount(Pointer element) {
        Pointer pattern = getPattern(element, UIA_GridPatternId, IID_IUIAutomationGridPattern);
        if (pattern != null) {
            Pointer vTablePointer = pattern.getPointer(0);
            Pointer[] vTable = new Pointer[8];
            vTablePointer.read(0, vTable, 0, 8);
            IntByReference retVal = new IntByReference();
            Function f = Function.getFunction(vTable[4], Function.ALT_CONVENTION); // getCurrentRowCount = 4
            int res = f.invokeInt(new Object[]{pattern, retVal});
            int val = 0;
            if (res == 0) {
                val = retVal.getValue();
            }
            release(pattern);
            return val;
        }
        return 0;
    }

    public static Pointer getGridItem(Pointer element, int row, int col) {
        Pointer pattern = getPattern(element, UIA_GridPatternId, IID_IUIAutomationGridPattern);
        if (pattern != null) {
            Pointer vTablePointer = pattern.getPointer(0);
            Pointer[] vTable = new Pointer[8];
            vTablePointer.read(0, vTable, 0, 8);
            PointerByReference outRef = new PointerByReference();
            Function f = Function.getFunction(vTable[3], Function.ALT_CONVENTION); // getItem = 3
            int res = f.invokeInt(new Object[]{pattern, row, col, outRef});
            Pointer item = null;
            if (res == 0 && outRef.getValue() != null) {
                item = queryInterface(outRef.getValue(), IID_IUIAutomationElement);
            }
            release(pattern);
            return item;
        }
        return null;
    }
}
