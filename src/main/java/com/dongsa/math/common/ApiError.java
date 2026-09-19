package com.dongsa.math.common;

import java.util.Map;

/** 에러 응답 형태를 하나로 고정한다. fields 는 입력값 검증에 걸렸을 때만 채워진다. */
public record ApiError(String code, String message, Map<String, String> fields) {

    public static ApiError of(ErrorCode code) {
        return new ApiError(code.name(), code.message(), null);
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }
}
