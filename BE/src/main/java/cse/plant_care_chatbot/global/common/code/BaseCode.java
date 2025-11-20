package cse.plant_care_chatbot.global.common.code;

import org.springframework.http.HttpStatus;

public interface BaseCode {
    HttpStatus getHttpStatus();
    String getMessage();
}
