package com.strange.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class CommandUtil {

    public static String execCommand(String command) throws IOException, InterruptedException {
        return execCommand(null, command);
    }

    public static String execCommand(File file, String command) throws IOException, InterruptedException {
        return execCommand(file, command, false);
    }

    public static String execCommand(File file, String command, boolean logging) throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        if (OSUtil.isWindows()) {
            commands.add("cmd");
            commands.add("/c");
        }

        String[] commandArr = command.split(" ");
        commands.addAll(Arrays.asList(commandArr));

        Process process = Runtime.getRuntime().exec(commands.toArray(new String[]{}), null, file);

        InputStream inputStream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;

        StringBuilder sb = new StringBuilder();
        // output the process detail
        while ((line = reader.readLine()) != null) {
            if (logging) {
                log.info("exec command line output: {}", line);
            }
            sb.append(line);
            sb.append('\n');
        }
        process.waitFor();
        return sb.toString();
    }
}
