package sikuli;

import java.awt.Rectangle;
import java.io.File;

import org.sikuli.script.FindFailed;
import org.sikuli.script.Match;
import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;
import tools.Log;

/**
 * Extends Sikuli's {@link Screen} class to provide customized UI automation
 * actions.
 * This class implements a singleton pattern and includes additional features
 * like
 * result logging, screenshot capture, and coordinate tracking.
 */
public class SikuliActions extends Screen {
    private File resultLocation;
    private File screenshotsSavedLocation;
    private File regionsSavedLocation;
    private File coodinatesSavedLocation;
    private static final SikuliActions actions = new SikuliActions();

    private SikuliActions() {
    }

    /**
     * Gets the singleton instance of SikuliActions.
     * 
     * @return the SikuliActions instance
     */
    public static SikuliActions getSikuliActions() {
        return actions;
    }

    /**
     * Verifies that the result location has been set.
     * Logs a failure if the result location is null.
     */
    private void confirmResultLocationSet() {
        if (resultLocation == null)
            Log.fail("Result location not set for SikuliActions!");
    }

    /**
     * Sets the directory where execution results (screenshots, region data, and
     * coordinates) will be stored.
     * Creates the necessary subdirectories if they do not exist.
     * 
     * @param directory The root directory for saving results.
     */
    public void setResultLocation(File directory) {
        if (!directory.isDirectory()) {
            Log.fail("[error] " + directory.getAbsolutePath() + " is not a valid directory!");
        }

        resultLocation = directory;
        regionsSavedLocation = new File(directory, "Region");
        screenshotsSavedLocation = new File(directory, "Screenshots");
        coodinatesSavedLocation = new File(directory, "Coordinates");

        regionsSavedLocation.mkdirs();
        screenshotsSavedLocation.mkdirs();
        coodinatesSavedLocation.mkdirs();
    }

    /**
     * Extracts the filename from a target object (String path or Pattern).
     * 
     * @param target The target object.
     * @param <T>    The type of the target.
     * @return The filename string.
     */
    private <T> String getFilenameFromTarget(T target) {
        File file;
        if (target.getClass().isInstance(""))
            file = new File((String) target);
        else {
            Pattern p = (Pattern) target;
            file = new File(p.getFilename());
        }
        return file.getName();
    }

    /**
     * Finds the target on screen and return its region.
     * 
     * @param target The target string or Pattern.
     * @param <T>    The type of the target.
     * @return The {@link Region} where the target was found.
     * @throws FindFailed If the target cannot be found.
     */
    private <T> Region saveCoordinates(T target) throws FindFailed {
        String targetFilename = getFilenameFromTarget(target);
        File absoluteCoordinatesFilename = new File(coodinatesSavedLocation, targetFilename);
        Log.warn(absoluteCoordinatesFilename.getAbsolutePath());
        Rectangle rectangle = find(target).getRect();
        Region region = new Region(rectangle);
        // Log.debug(region);//TODO
        return region;
    }

    /**
     * Placeholder method for saving region data.
     * 
     * @param region The region to save.
     */
    private void saveRegion(Region region) {
        // Log.debug(regionsSavedLocation);//TODO
        // Log.debug(region);//TODO
    }

    /**
     * Internal method to save the region and coordinates of a target.
     * Note: Currently disabled by a internal flag.
     * 
     * @param target The target string or Pattern to track.
     * @param <T>    The type of the target.
     * @throws FindFailed If the target cannot be found.
     */
    private <T> void saveRegionAndCoordinates(T target) throws FindFailed {
        if (!!true)
            return;

        if (!(target.getClass().isInstance("") || target instanceof Pattern))
            return;
        confirmResultLocationSet();
        Region region = saveCoordinates(target);
        saveRegion(region);
    }

    @Override
    /**
     * Performs a click on the specified target.
     * 
     * @param target The target string or Pattern to click.
     * @param <T>    The type of the target.
     * @return 1 if click was successful, 0 otherwise.
     * @throws FindFailed If the target cannot be found.
     */
    public <T> int click(T target) throws FindFailed {
        saveRegionAndCoordinates(target);
        // super.wait(target, 30);
        return super.click(target);
    }

    @Override
    /**
     * Performs a double-click on the specified target.
     * 
     * @param target The target string or Pattern to double-click.
     * @param <T>    The type of the target.
     * @return 1 if successful, 0 otherwise.
     * @throws FindFailed If the target cannot be found.
     */
    public <T> int doubleClick(T target) throws FindFailed {
        saveRegionAndCoordinates(target);
        return super.doubleClick(target);
    }

    @Override
    /**
     * Performs a drag and drop operation from target1 to target2.
     * 
     * @param target1 The source target.
     * @param target2 The destination target.
     * @param <T>     The type of the targets.
     * @return 1 if successful, 0 otherwise.
     * @throws FindFailed If either target cannot be found.
     */
    public <T> int dragDrop(T target1, T target2) throws FindFailed {
        saveRegionAndCoordinates(target1);
        saveRegionAndCoordinates(target2);
        return super.dragDrop(target1, target2);
    }

    @Override
    /**
     * Hovers the mouse over the specified target.
     * 
     * @param target The target string or Pattern to hover over.
     * @param <T>    The type of the target.
     * @return 1 if successful, 0 otherwise.
     * @throws FindFailed If the target cannot be found.
     */
    public <T> int hover(T target) throws FindFailed {
        saveRegionAndCoordinates(target);
        return super.hover(target);
    }

    @Override
    /**
     * Waits for the specified target to vanish from the screen.
     * 
     * @param target  The target string or Pattern to wait for.
     * @param timeout The maximum time to wait in seconds.
     * @param <T>     The type of the target.
     * @return true if it vanished, false otherwise.
     */
    public <T> boolean waitVanish(T target, double timeout) {
        try {
            saveRegionAndCoordinates(target);
        } catch (FindFailed e) {
            Log.fail(e.getMessage());
        }
        return super.waitVanish(target, timeout);
    }

    @Override
    /**
     * Performs a mouse wheel action on the specified target.
     * 
     * @param target    The target string or Pattern.
     * @param direction The direction to scroll (WHEEL_UP, WHEEL_DOWN).
     * @param steps     The number of steps to scroll.
     * @param <T>       The type of the target.
     * @return 1 if successful, 0 otherwise.
     * @throws FindFailed If the target cannot be found.
     */
    public <T> int wheel(T target, int direction, int steps) throws FindFailed {
        saveRegionAndCoordinates(target);
        return super.wheel(target, direction, steps);
    }

    @Override
    /**
     * Checks if the specified target exists on the screen within the timeout.
     * 
     * @param target  The target string or Pattern to look for.
     * @param timeout The maximum time to wait in seconds.
     * @param <T>     The type of the target.
     * @return A {@link Match} object if found, null otherwise.
     */
    public <T> Match exists(T target, double timeout) {
        try {
            saveRegionAndCoordinates(target);
        } catch (FindFailed e) {
            Log.fail(e.getMessage());
        }
        return super.exists(target, timeout);
    }

    /**
     * Captures a screenshot of the current screen and saves it as
     * "CurrentScreen.png".
     */
    public void captureScreen() {
        capture().save(screenshotsSavedLocation.getAbsolutePath(), "CurrentScreen.png");
    }

    /**
     * Captures a screenshot of the current screen only if a file with the specified
     * name does not already exist.
     * 
     * @param fileName The name of the file to save (e.g., "my_screenshot.png").
     */
    public void captureScreenIfNotExist(String fileName) {
        confirmResultLocationSet();
        File screenshotFile = new File(screenshotsSavedLocation, fileName);
        if (!screenshotFile.exists()) {
            capture().save(screenshotsSavedLocation.getAbsolutePath(), fileName);
            Log.info("Captured current screen: " + screenshotFile.getAbsolutePath());
        }
    }
}