package com.strange.cli.input;

import lombok.Data;

import java.io.File;

@Data
public class InputContext {
    private File projectDir;

    private File fixSpaceDir;

    private File cacheDir;

    private Integer maxRetryCount;

    private File libraryDatabaseDir;

    private String accessToken;

    private boolean local;

    private boolean skipFix;
}
