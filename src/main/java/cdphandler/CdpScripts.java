package cdphandler;

public class CdpScripts {
    public static final String CDP_ELEMENTS_CLEANUP_SCRIPT = """
            document.cdpElements = null;
            document.cdpElementIds = null;
            async function cdpElementsCleanup() {
                var counter = 0;
                var listToBeDeleted = [];
                for(let cdpElement of document.cdpElements) {
                    if(!cdpElement[1].isConnected || cdpElement[1] === 'undefined') {
                        listToBeDeleted.push(cdpElement[0]);
                    }
                }
                for(let i = 0; i < listToBeDeleted.length; i++) {
                    document.cdpElements.delete(listToBeDeleted[i]);
                    counter++;
                }
            }
            if(!document.cdpElements) {
                console.log('CDP Driver elements cleanup script registered...');
                document.cdpElementsScheduler = setInterval(cdpElementsCleanup, 3000);
            }
            """;

    public static final String ID_LOCATOR_SCRIPT = "var el = document.getElementById(\"%s\"); if(el) elements.push(el);";
    public static final String CSS_LOCATOR_SCRIPT = "var nodes = referenceElement.querySelectorAll(\"%s\"); for(var i=0; i<nodes.length; i++) elements.push(nodes[i]);";
    public static final String XPATH_LOCATOR_SCRIPT = "var res = document.evaluate(\"%s\", referenceElement, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null); for(var i=0; i<res.snapshotLength; i++) elements.push(res.snapshotItem(i));";
    public static final String FIND_ELEMENT_SCRIPT = """
                (function() {
                    if (!document.cdpElements) document.cdpElements = new Map();
                    if (!document.cdpElementIds) document.cdpElementIds = new WeakMap();

                    var elements = [];
                    var referenceId = '%s';
                    var referenceElement = document;
                    if(referenceId !== '')
                        referenceElement = document.cdpElements.get(referenceId);
                    <locatorScript>
                    var ids = [];
                    for(var i=0; i<elements.length; i++) {
                        var el = elements[i];
                        var id = document.cdpElementIds.get(el);
                        if (!id) {
                            id = (new Date().getTime()) + "_" + Math.random().toString(36).substring(2);
                            document.cdpElements.set(id, el);
                            document.cdpElementIds.set(el, id);
                        }
                        ids.push(id);
                    }
                    return ids;
                })();
                """;
}
