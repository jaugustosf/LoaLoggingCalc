package com.lostark.calc.setup_manager_logging.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
            "status", "Online",
            "projeto", "Lost Ark Logging ROI API",
            "endpoint_calculo", "/api/v1/roi/calculate"
        );
    }
}
