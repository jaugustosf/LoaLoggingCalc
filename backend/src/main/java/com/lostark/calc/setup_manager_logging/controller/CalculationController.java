package com.lostark.calc.setup_manager_logging.controller;

import com.lostark.calc.setup_manager_logging.dto.CalculationRequest;
import com.lostark.calc.setup_manager_logging.dto.CalculationResponse;
import com.lostark.calc.setup_manager_logging.service.CalculationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/roi")
@CrossOrigin(origins = "http://localhost:4200")
public class CalculationController {

    private final CalculationService calculationService;

    public CalculationController(CalculationService calculationService) {
        this.calculationService = calculationService;
    }

    @PostMapping("/calculate")
    public CalculationResponse calculate(@RequestBody CalculationRequest request) {
        return calculationService.calculate(request);
    }
}
