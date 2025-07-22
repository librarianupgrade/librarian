package com.strange.code.format;

import cn.hutool.core.io.FileUtil;
import com.strange.code.format.formatter.ProjectCodeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;

@Slf4j
@Component
@SpringBootApplication
public class FormatCodeApplication {

    private static final String PROJECT_ROOT_DIR_KEY = "p";

    public static void main(String[] args) throws ParseException {
        SpringApplication.run(FormatCodeApplication.class, args);
        log.info("The input args: {}", Arrays.toString(args));

        Options options = new Options();
        options.addOption(new Option(PROJECT_ROOT_DIR_KEY, true, "project path ready to format code"));

        CommandLineParser parser = new DefaultParser();

        CommandLine commandLine = parser.parse(options, args);
        String projectPath = commandLine.getOptionValue(PROJECT_ROOT_DIR_KEY);
        if (projectPath == null) {
            throw new IllegalArgumentException(
                    String.format(
                            "Required option '--%s' is missing. Please specify the root directory of the project to format, e.g.: --%s=/path/to/project",
                            PROJECT_ROOT_DIR_KEY, PROJECT_ROOT_DIR_KEY
                    )
            );
        }

        File projectDir = FileUtil.file(projectPath);
        new ProjectCodeFormatter(projectDir).startFormat();
    }
}
