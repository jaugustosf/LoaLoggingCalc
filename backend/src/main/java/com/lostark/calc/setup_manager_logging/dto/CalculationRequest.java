package com.lostark.calc.setup_manager_logging.dto;

public record CalculationRequest(
    MarketPrices prices,
    UserInventory inventory
) {}
