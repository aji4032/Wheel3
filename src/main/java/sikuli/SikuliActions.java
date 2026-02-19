package sikuli;

import java.awt.Rectangle;
import java.io.File;

import org.sikuli.script.FindFailed;
import org.sikuli.script.Match;
import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;
import tools.Log;

public class SikuliActions extends Screen {
    private File resultLocation;
    private File regionsSavedLocation;
    private File coodinatesSavedLocation;
    private static final SikuliActions actions = new SikuliActions();

    private SikuliActions(){}

    public static SikuliActions getSikuliActions(){
        return actions;
    }

    private void confirmResultLocationSet(){
        if(resultLocation == null)
            Log.fail("Result location not set for SikuliActions!");
    }

    public void setResultLocation(File directory){
        if(!directory.isDirectory()){
            Log.fail("[error] " + directory.getAbsolutePath() + " is not a valid directory!");
        }

        resultLocation          = directory;
        regionsSavedLocation    = new File(directory, "Region");
        coodinatesSavedLocation = new File(directory, "Coordinates");

        regionsSavedLocation.mkdirs();
        coodinatesSavedLocation.mkdirs();
    }

    private <T>String getFilenameFromTarget(T target) {
        File file;
        if(target.getClass().isInstance(""))
            file = new File((String) target);
        else {
            Pattern p = (Pattern)target;
            file = new File(p.getFilename());
        }
        return file.getName();
    }

    private <T>Region saveCoordinates(T target) throws FindFailed {
        String targetFilename = getFilenameFromTarget(target);
        File absoluteCoordinatesFilename = new File(coodinatesSavedLocation, targetFilename);
        Log.warn(absoluteCoordinatesFilename.getAbsolutePath());
        Rectangle rectangle = find(target).getRect();
        Region region = new Region(rectangle);
//        Log.debug(region);//TODO
        return region;
    }

    private void saveRegion(Region region) {
//        Log.debug(regionsSavedLocation);//TODO
//        Log.debug(region);//TODO
    }

    private <T>void saveRegionAndCoordinates(T target) throws FindFailed{
        if(!!true)
            return;

        if(!(target.getClass().isInstance("") || target instanceof Pattern))
            return;
        confirmResultLocationSet();
        Region region = saveCoordinates(target);
        saveRegion(region);
    }

    @Override
    public <T>int click(T target) throws FindFailed{
        saveRegionAndCoordinates(target);
//        super.wait(target, 30);
        return super.click(target);
    }

    @Override
    public <T>int doubleClick(T target) throws FindFailed{
        saveRegionAndCoordinates(target);
        return super.doubleClick(target);
    }

    @Override
    public <T>int dragDrop(T target1, T target2) throws FindFailed{
        saveRegionAndCoordinates(target1);
        saveRegionAndCoordinates(target2);
        return super.dragDrop(target1, target2);
    }

    @Override
    public <T>int hover(T target) throws FindFailed{
        saveRegionAndCoordinates(target);
        return super.hover(target);
    }

    @Override
    public <T>boolean waitVanish(T target, double timeout) {
        try {
            saveRegionAndCoordinates(target);
        } catch (FindFailed e) {
            Log.fail(e.getMessage());
        }
        return super.waitVanish(target, timeout);
    }

    @Override
    public <T>int wheel(T target, int direction, int steps) throws FindFailed{
        saveRegionAndCoordinates(target);
        return super.wheel(target, direction, steps);
    }

    @Override
    public <T>Match exists(T target, double timeout){
        try {
            saveRegionAndCoordinates(target);
        } catch (FindFailed e) {
            Log.fail(e.getMessage());
        }
        return super.exists(target, timeout);
    }
}