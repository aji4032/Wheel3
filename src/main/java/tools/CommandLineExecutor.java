package tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandLineExecutor {
    public static List<String> execute(String[] command, String[] filters) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).start();
            try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

                List<String> output = getOutput(inputReader, filters);
                if (!output.isEmpty()) {
                    return output;
                }

                List<String> errorOutput = getOutput(errorReader, filters);
                if (!errorOutput.isEmpty()) {
                    Log.warn("Error: " + errorOutput);
                }
            }
            process.waitFor();
        } catch (Exception e) {
            Log.fail("Error while executing command \"" + Arrays.toString(command) + "\" - " + e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return Collections.emptyList();
    }

    private static List<String> getOutput(BufferedReader bufferedReader, String[] filters) throws IOException {
        return bufferedReader
                .lines()
                .filter(line -> filters == null || filters.length == 0 || Arrays.stream(filters).noneMatch(line::contains))
                .toList();
    }
}