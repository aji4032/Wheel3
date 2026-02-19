package tools;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;

public class PropertyFileHandler {
    static String configFile = "src/main/resources/config.properties";

    private PropertyFileHandler(){}

    public static void setProperty(String key, String value){
        setProperty(configFile, key, value);
    }
    public static void setProperty(String configFile, String key, String value){
        ArrayList<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String line;
            boolean isKeyPresent = false;
            while ((line = br.readLine()) != null) {
                if(line.contains(key + "=")){
                    lines.add(key + "=" + value);
                    isKeyPresent = true;
                }
                else
                    lines.add(line);
            }
            if(!isKeyPresent){
                lines.add(key + "=" + value);
            }
        } catch (IOException e) {
            Log.fail(e.getMessage());
        }

        try (FileWriter writer = new FileWriter(configFile)) {
            for(String line: lines) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            Log.fail(e.getMessage());
        }
    }

    public static String getProperty(String key){
        return getProperty(configFile, key);
    }
    public static String getProperty(String configFile, String key){
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(configFile)) {
            prop.load(input);
            return prop.getProperty(key);
        } catch (IOException e) {
            Log.fail(e.getMessage());
        }
        return null;
    }
}