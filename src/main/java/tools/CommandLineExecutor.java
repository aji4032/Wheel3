package tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandLineExecutor {
    public static List<String> execute(String[] command, String[] filters) {
        try {
            Process objProcess = Runtime.getRuntime().exec(command);
            BufferedReader objBufferedInputReader = new BufferedReader(new InputStreamReader(objProcess.getInputStream()));
            BufferedReader objBufferedErrorReader = new BufferedReader(new InputStreamReader(objProcess.getErrorStream()));

            List<String> output = getOutput(objBufferedInputReader, filters);
            if(output.isEmpty()) {
                output = getOutput(objBufferedErrorReader, filters);
                if(!output.isEmpty()) {
                    Log.warn("Error: " + output);
                }
            } else {
                return output;
            }
        } catch (Exception e) {
            Log.fail("Error while executing command \"" + Arrays.toString(command) + "\" - " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private static List<String> getOutput(BufferedReader objBufferedReader, String[] filters) throws IOException {
        List<String> list = new ArrayList<>();
        String s;
        while((s = objBufferedReader.readLine()) != null) {
            boolean filtersPresent = true;
            for(int i = 0; i < filters.length; i++) {
                if(s.contains(filters[i])) {
                    filtersPresent = false;
                    break;
                }
            }
            if(filtersPresent) {
                list.add(s);
            }
        }
        return list;
    }
}