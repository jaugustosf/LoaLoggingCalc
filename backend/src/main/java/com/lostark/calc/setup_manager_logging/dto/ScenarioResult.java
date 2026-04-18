package com.lostark.calc.setup_manager_logging.dto;

import java.util.Map;

public record ScenarioResult(
    String name,
    double netProfit,
    String description,
    Map<String, Long> details // Optional details like quantity sold or crafted
) {}
