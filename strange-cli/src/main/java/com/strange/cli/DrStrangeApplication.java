package com.strange.cli;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.strange.cli.config.ConfigProperty;
import com.strange.cli.input.InputContext;
import com.strange.common.config.GlobalConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;

@Slf4j
@Component
@EnableScheduling
@EnableConfigurationProperties
@ConfigurationPropertiesScan
@SpringBootApplication
public class DrStrangeApplication {

    private static final String PROJECT_ROOT_DIR_KEY = "p";

    private static final String LOCAL_COMMAND_KEY = "l";

    private static final String SKIP_FIX_COMMAND_KEY = "skipFix";

    private static DrStrange drStrange;

    @Autowired
    public void setDrStrange(DrStrange drStrange) {
        DrStrangeApplication.drStrange = drStrange;
    }

    public static void main(String[] args) {
        SpringApplication.run(DrStrangeApplication.class, args);
        InputContext inputContext = parseArgs(args);
        drStrange.run(inputContext);
    }

    private static InputContext parseArgs(String[] args) {
        log.info("The input args: {}", Arrays.toString(args));

        Options options = new Options();
        options.addOption(new Option(PROJECT_ROOT_DIR_KEY, true, "Specify the project root directory (e.g. /path/to/my-project)"));
        options.addOption(new Option(LOCAL_COMMAND_KEY, false, "Run in local project directory; do not copy the project to fixspace"));
        options.addOption(new Option(SKIP_FIX_COMMAND_KEY, false, "Skip the fix process"));

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar strange-cli-1.0-SNAPSHOT.jar -p <project directory> [-l] [-skipFix]", options);
            System.exit(1);
        }

        String projectPath = commandLine.getOptionValue(PROJECT_ROOT_DIR_KEY);

        File projectDir = FileUtil.file(projectPath);
        if (projectDir == null || !projectDir.isDirectory()) {
            throw new IllegalArgumentException("The input path is not a directory: " + projectDir);
        }

        ConfigProperty configProperty = SpringUtil.getBean(ConfigProperty.class);

        String fixSpaceDirPath = configProperty.getFixSpaceDirPath();
        File fixSpaceDir = FileUtil.file(fixSpaceDirPath);
        if (StringUtils.isEmpty(fixSpaceDirPath)) {
            fixSpaceDir = GlobalConfig.DefaultFixSpace;
        }

        // prepare environment
        boolean local = commandLine.hasOption(LOCAL_COMMAND_KEY);
        projectDir = prepareEnv(projectDir, fixSpaceDir, local);

        // generate input context
        InputContext inputContext = new InputContext();
        inputContext.setProjectDir(projectDir);
        inputContext.setFixSpaceDir(fixSpaceDir);
        inputContext.setLocal(local);
        inputContext.setSkipFix(commandLine.hasOption(SKIP_FIX_COMMAND_KEY));

        File cacheDir = FileUtil.file(projectDir.getAbsolutePath(), GlobalConfig.CacheDirName);
        inputContext.setCacheDir(cacheDir);

        // gpt access token
        inputContext.setAccessToken(configProperty.getGptAccessToken());

        // library database dir
        inputContext.setLibraryDatabaseDir(FileUtil.file(configProperty.getLibraryDatabasePath()));

        // llm max retry count
        inputContext.setMaxRetryCount(configProperty.getMaxRetryCount());

        return inputContext;
    }

    // prepare the environment
    private static File prepareEnv(File projectDir, File fixSpaceDir, boolean local) {
        if (local) return projectDir;
        String projectName = projectDir.getName();
        File destDir = FileUtil.file(fixSpaceDir.getAbsolutePath(), projectName);
        // first, delete all relevant files
        FileUtil.del(destDir);
        // then, copy files to workspace
        FileUtil.copy(projectDir, fixSpaceDir, true);
        return destDir;
    }
}
