package cse.plant_care_chatbot.global.common;

import cse.plant_care_chatbot.global.common.code.BaseCode;
import com.fasterxml.jackson.annotation.JsonInclude;

public record CommonResponse<T>(
        boolean isSuccess,
        int code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        T result
) {
    public static <T> CommonResponse<T> success(BaseCode successCode, T data) {
        return new CommonResponse<>(true, successCode.getHttpStatus().value(), successCode.getMessage(), data);
    }

    public static CommonResponse<Void> success(BaseCode successCode) {
        return new CommonResponse<>(true, successCode.getHttpStatus().value(), successCode.getMessage(), null);
    }

    public static <T> CommonResponse<T> failure(BaseCode errorCode, T data) {
        return new CommonResponse<>(false, errorCode.getHttpStatus().value(), errorCode.getMessage(), data);
    }

    public static CommonResponse<Void> failure(BaseCode errorCode) {
        return new CommonResponse<>(false, errorCode.getHttpStatus().value(), errorCode.getMessage(), null);
    }
}