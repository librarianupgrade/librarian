package com.strange.common.enums;

import lombok.Getter;

@Getter
public enum FixTypeEnum {

    FixCompilationError(0, "Fix Compilation Error"),

    FixDeprecation(1, "Fix Deprecation"),

    Both(2, "Fix Both(Compilation Error and Deprecation)");

    private final Integer typeId;

    private final String typeName;

    FixTypeEnum(Integer typeId, String typeName) {
        this.typeId = typeId;
        this.typeName = typeName;
    }

    public static FixTypeEnum getTypeById(Integer typeId) {
        for (FixTypeEnum fixTypeEnum : FixTypeEnum.values()) {
            if(fixTypeEnum.getTypeId().equals(typeId)) {
                return fixTypeEnum;
            }
        }
        return null;
    }

}
