package cdphandler;

public class CdpScripts {
    protected static final String CDP_ELEMENTS_CLEANUP_SCRIPT = """
            document.cdpElements = null;
            document.cdpElementIds = null;
            async function cdpElementsCleanup() {
                var counter = 0;
                var listToBeDeleted = [];
                for(let cdpElement of document.cdpElements) {
                    if(!cdpElement[1].isConnected || cdpElement[1] === undefined || cdpElement[1] === 'undefined')
                        listToBeDeleted.push(cdpElement[0]);
                }
                for(let i = 0; i < listToBeDeleted.length; i++) {
                    document.cdpElements.delete(listToBeDeleted[i]);
                    counter++;
                }
            }
            if(!document.cdpElements) {
                console.log('CDP Driver elements cleanup script registered...');
                document.cdpElementsScheduler = setInterval(cdpElementsCleanup, 3000);
            }""";
    protected static final String WRAPPER_PRE_SCRIPT = "(function() {";
    protected static final String WRAPPER_POST_SCRIPT = "})();";
    protected static final String FETCH_ELEMENT = """
                var element = document.cdpElements.get(`%s`);
                if(!element) return null;""";

    protected static final String BACK_SCRIPT = "window.history.back();";
    protected static final String FORWARD_SCRIPT = "window.history.forward();";
    protected static final String GET_CURRENT_URL_SCRIPT = WRAPPER_PRE_SCRIPT + "return document.URL;" + WRAPPER_POST_SCRIPT;
    protected static final String GET_PAGE_SOURCE = WRAPPER_PRE_SCRIPT + "return document.documentElement.outerHTML;" + WRAPPER_POST_SCRIPT;
    protected static final String GET_TITLE_SCRIPT = "(function() {return document.title;})();";
    protected static final String WAIT_UNTIL_DOCUMENT_READY = "(function() {return document.readyState === 'complete';})();";

    protected static final String ID_LOCATOR_SCRIPT    = "var el = document.getElementById(`%s`); if(el) elements.push(el);";
    protected static final String CSS_LOCATOR_SCRIPT   = "var nodes = referenceElement.querySelectorAll(`%s`); for(var i=0; i<nodes.length; i++) elements.push(nodes[i]);";
    protected static final String XPATH_LOCATOR_SCRIPT = "var res = document.evaluate(`%s`, referenceElement, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null); for(var i=0; i<res.snapshotLength; i++) elements.push(res.snapshotItem(i));";
    protected static final String FIND_ELEMENT_SCRIPT  = """
            (function() {
                if (!document.cdpElements) document.cdpElements = new Map();
                if (!document.cdpElementIds) document.cdpElementIds = new WeakMap();
                var elements = [];
                var referenceId = `%s`;
                var referenceElement = document;
                if(referenceId !== ``)
                    referenceElement = document.cdpElements.get(referenceId);
                <locatorScript>
                var ids = [];
                for(var i=0; i<elements.length; i++) {
                    var el = elements[i];
                    var id = document.cdpElementIds.get(el);
                    if (!id) {
                        id = (new Date().getTime()) + `_` + Math.random().toString(36).substring(2);
                        document.cdpElements.set(id, el);
                        document.cdpElementIds.set(el, id);
                    }
                    ids.push(id);
                }
                return ids;
            })();""";
    protected static final String CLEAR_ELEMENT_SCRIPT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
            element.value = '';""" + WRAPPER_POST_SCRIPT;
    protected static final String GET_ATTRIBUTE_SCRIPT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
            return element.getAttribute(`%s`);""" + WRAPPER_POST_SCRIPT;
    protected static final String IN_VIEW_CENTER_POINT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
             var rect    = element.getClientRects()[0];
             var _left   = (Math.max(0, Math.min(rect.x, rect.x + rect.width)));
             var _right  = (Math.min(window.innerWidth, Math.max(rect.x, rect.x + rect.width)));
             var _top    = (Math.max(0, Math.min(rect.y, rect.y + rect.height)));
             var _bottom = (Math.min(window.innerHeight, Math.max(rect.y, rect.y + rect.height)));
             var x = (0.5 * (_left + _right));
             var y = (0.5 * (_top + _bottom));
             return JSON.stringify({"x":x,"y":y});""" + WRAPPER_POST_SCRIPT;
    protected static final String GET_CSS_VALUE_SCRIPT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
                return window.getComputedStyle(element).getPropertyValue(`%s`);""" + WRAPPER_POST_SCRIPT;
    protected static final String GET_RECT_SCRIPT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
                var rect = element.getBoundingClientRect();
                return JSON.stringify({x: rect.x, y: rect.y, width: rect.width, height: rect.height});""" + WRAPPER_POST_SCRIPT;
    protected static final String GET_SCROLL_HEIGHT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
                return element.scrollHeight;""" + WRAPPER_POST_SCRIPT;
    protected static final String GET_SCROLL_LEFT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
                return element.scrollLeft;""" + WRAPPER_POST_SCRIPT;
    protected static final String GET_SCROLL_TOP = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
                return element.scrollTop;""" + WRAPPER_POST_SCRIPT;
    protected static final String GET_INNER_TEXT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
                return element.innerText;""" + WRAPPER_POST_SCRIPT;
    protected static final String IS_DISPLAYED_SCRIPT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
                var style = window.getComputedStyle(element);
                if(style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') return false;
                var rect = element.getBoundingClientRect();
                return rect.width > 0 && rect.height > 0 && rect.x < window.innerWidth && rect.y < window.innerHeight && rect.x + rect.width > 0 && rect.y + rect.height > 0;
            """ + WRAPPER_POST_SCRIPT;
    protected static final String IS_ELEMENT_OBSCURED_SCRIPT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
                var elementAtPoint = document.elementFromPoint(%s, %s);
                return !(element === elementAtPoint || element.contains(elementAtPoint));""" + WRAPPER_POST_SCRIPT;
    protected static final String IS_ENABLED_SCRIPT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
                return element.disabled === undefined || element.disabled === false;""" + WRAPPER_POST_SCRIPT;
    protected static final String IS_SELECTED_SCRIPT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
                return element.selected || element.checked || element.enabled;""" + WRAPPER_POST_SCRIPT;
    protected static final String SCROLL_BY_SCRIPT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
                element.scrollBy(%s, %s);""" + WRAPPER_POST_SCRIPT;
    protected static final String SCROLL_INTO_VIEW_SCRIPT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
                element.scrollIntoViewIfNeeded(true);""" + WRAPPER_POST_SCRIPT;
    protected static final String SET_ELEMENT_VALUE_SCRIPT = WRAPPER_PRE_SCRIPT + FETCH_ELEMENT + """
                element.value = element.value + `%s`;""" + WRAPPER_POST_SCRIPT;
}