package com.strange.common.enums;

import com.strange.common.utils.CommandUtil;

import java.io.IOException;

/**
 * Define the JDK version for the Java project being detected.
 */
public enum JDKVersionEnum {
    JDK_1_8("jenv global 1.8"),
    JDK_11("jenv global 11"),
    JDK_17("jenv global 17"),
    JDK_23("jenv global 23");

    private final String JDKCommand;

    JDKVersionEnum(String JDKCommand) {
        this.JDKCommand = JDKCommand;
    }

    public static JDKVersionEnum getJDKVersion(final String name) {
        return switch (name) {
            case "1.8" -> JDK_1_8;
            case "11" -> JDK_11;
            case "17" -> JDK_17;
            case "23" -> JDK_23;
            default -> JDK_1_8;
        };
    }

    @Deprecated
    public void runSetJDKEnvironmentCommand() {
        try {
            CommandUtil.execCommand(this.JDKCommand);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
