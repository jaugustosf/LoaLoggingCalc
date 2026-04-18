package com.lostark.calc.setup_manager_logging;

import com.lostark.calc.setup_manager_logging.dto.*;
import com.lostark.calc.setup_manager_logging.service.CalculationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
public class CalculationTest {

    @Autowired
    private CalculationService service;

    @Test
    public void testScenarioA() {
        UserInventory inv = new UserInventory(800L, 1500L, 2500L, 6000L);
        MarketPrices prices = new MarketPrices(42.0, 28.0, 15.0, 7.0, 130.0, 290.0);
        CalculationRequest request = new CalculationRequest(prices, inv);
        
        CalculationResponse response = service.calculate(request);
        ScenarioResult scenarioA = response.scenarios().stream()
                .filter(s -> s.name().equals("Cenário A: Venda Total"))
                .findFirst().orElseThrow();
        
        System.out.println("Lucro Cenário A: " + scenarioA.netProfit());
    }
}
