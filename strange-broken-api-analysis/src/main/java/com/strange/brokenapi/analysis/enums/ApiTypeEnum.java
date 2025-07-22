package com.strange.brokenapi.analysis.enums;

public enum ApiTypeEnum {
    IMPORT("Import"),
    CLASS("Class"),
    CLASS_DEF("Class_Def"),
    CONSTRUCTOR("Constructor"),
    CONSTRUCTOR_DEF("Constructor_Def"),
    METHOD("Method"),
    METHOD_DEF("Method_Def"),
    ABSTRACT_METHOD("AbstractMethod"),
    FIELD("Field"),
    FIELD_DEF("Field_Def");

    private final String typeName;

    ApiTypeEnum(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }
}
