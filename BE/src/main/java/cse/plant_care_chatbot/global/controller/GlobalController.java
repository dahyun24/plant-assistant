package cse.plant_care_chatbot.global.controller;

import cse.plant_care_chatbot.global.common.CommonResponse;
import cse.plant_care_chatbot.global.controller.swagger.GlobalSwagger;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cse.plant_care_chatbot.global.common.code.SuccessCode.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/global")
public class GlobalController implements GlobalSwagger {

    @Override
    @GetMapping("/health-check")
    public CommonResponse<String> healthCheck() {
        return CommonResponse.success(HEALTH_CHECK_SUCCESS, "OK");
    }
}
