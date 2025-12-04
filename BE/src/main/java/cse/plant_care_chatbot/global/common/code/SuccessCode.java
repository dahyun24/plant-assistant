package cse.plant_care_chatbot.global.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements BaseCode {

    // health_check
    HEALTH_CHECK_SUCCESS(HttpStatus.OK, "🌱 서버가 정상적으로 작동 중입니다."),
    PLANT_ANALYSIS_SUCCESS(HttpStatus.OK, "식물 분석을 성공적으로 완료하였습니다."),
    PLANT_FEEDBACK_SUCCESS(HttpStatus.OK,"피드백 적용이 성공적으로 완료되었습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
