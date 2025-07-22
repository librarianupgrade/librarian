package com.strange.fix.engine.konwledge;

import lombok.Data;

import java.io.File;

@Data
public class LibraryKnowledgeDirectory {

    private File pomDir;

    private File jarDir;

    private File javaDocDir;

    private File sourceDir;

    private File testcaseDir;
}
