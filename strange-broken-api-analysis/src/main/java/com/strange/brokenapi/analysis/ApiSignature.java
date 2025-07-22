package com.strange.brokenapi.analysis;

import cn.hutool.core.collection.CollUtil;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
public class ApiSignature implements Cloneable {

    private ApiTypeEnum brokenApiType;

    private String className;

    private String methodName;

    private List<String> methodParamList;

    private String methodReturnType;

    private String fieldName;

    private String fieldBelongedClassName; // the Class type contains this field declaration

    @Override
    public ApiSignature clone() {
        ApiSignature clone;
        try {
            clone = (ApiSignature) super.clone();
        } catch (CloneNotSupportedException e) {
            clone = new ApiSignature();
        }
        clone.setBrokenApiType(this.getBrokenApiType());
        clone.setClassName(this.getClassName());
        clone.setFieldName(this.getFieldName());
        clone.setMethodName(this.getMethodName());
        if (this.getMethodParamList() == null) {
            clone.setMethodParamList(null);
        } else {
            clone.setMethodParamList(new ArrayList<>(this.getMethodParamList()));
        }
        return clone;
    }

    @Override
    public String toString() {
        ApiTypeEnum apiTypeEnum = this.getBrokenApiType();
        switch (apiTypeEnum) {
            case METHOD -> {
                StringBuilder sb = new StringBuilder("ApiSignature(")
                        .append("brokenApiType=").append(apiTypeEnum)
                        .append(", className=").append(getClassName())
                        .append(", methodName=").append(getMethodName());
                Optional.ofNullable(methodReturnType)
                        .filter(s -> !s.isEmpty())
                        .ifPresent(s -> sb.append(", methodReturnType=").append(s));
                sb.append(", methodParamList=").append(getMethodParamList())
                        .append(")");
                return sb.toString();
            }
            case CLASS -> {
                return "ApiSignature(brokenApiType=" + apiTypeEnum + ", className=" + this.getClassName() + ")";
            }
            case FIELD -> {
                return "ApiSignature(brokenApiType=" + apiTypeEnum + ", className=" + this.getClassName() + ", fieldName=" + this.getFieldName() + ")";
            }
            case CONSTRUCTOR -> {
                return "ApiSignature(brokenApiType=" + apiTypeEnum + ", className=" + this.getClassName() + ", methodName=" + this.getMethodName() + ", methodParamList=" + this.getMethodParamList() + ")";
            }
            case ABSTRACT_METHOD -> {
                return "ApiSignature(brokenApiType=" + apiTypeEnum + ", className=" + this.getClassName() + ", methodName=" + this.getMethodName() + ", methodParamList=" + this.getMethodParamList() + ")";
            }
            case IMPORT -> {
                return "ApiSignature(brokenApiType=" + apiTypeEnum + ", className=" + this.getClassName() + ")";
            }
            default -> {
                return "ApiSignature(brokenApiType=" + apiTypeEnum + ", className=" + this.getClassName() + ", methodName=" + this.getMethodName() + ", methodParamList=" + this.getMethodParamList() + ", fieldName=" + this.getFieldName() + ")";
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApiSignature that = (ApiSignature) o;
        return brokenApiType == that.brokenApiType && Objects.equals(className, that.className)
                && Objects.equals(methodName, that.methodName) && CollUtil.isEqualList(methodParamList, that.methodParamList)
                && Objects.equals(fieldName, that.fieldName) && Objects.equals(methodReturnType, that.methodReturnType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(brokenApiType);
        result = 31 * result + Objects.hashCode(className);
        result = 31 * result + Objects.hashCode(methodName);
        result = 31 * result + Objects.hashCode(methodParamList);
        result = 31 * result + Objects.hashCode(fieldName);
        return result;
    }
}
