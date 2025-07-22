package com.strange.fix.engine.extraction.hint;

import lombok.Data;
import lombok.NonNull;

@Data
public class FixHint {
    private JavaDocHint javaDocHint;
    private MigrationCaseHint migrationCaseHint;

    public FixHint(JavaDocHint javaDocHint, MigrationCaseHint migrationCaseHint) {
        this.javaDocHint = javaDocHint;
        this.migrationCaseHint = migrationCaseHint;
    }
}
