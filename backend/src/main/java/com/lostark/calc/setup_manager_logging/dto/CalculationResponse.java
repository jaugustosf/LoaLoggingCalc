package com.lostark.calc.setup_manager_logging.dto;

import java.util.List;

public record CalculationResponse(
    List<ScenarioResult> scenarios,
    String bestScenarioName
) {}
