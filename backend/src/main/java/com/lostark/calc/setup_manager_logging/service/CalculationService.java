package com.lostark.calc.setup_manager_logging.service;

import com.lostark.calc.setup_manager_logging.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CalculationService {

    private static final double MARKET_TAX = 0.95;

    @Value("${recipe.abidos.cost}") private int abidosCost;
    @Value("${recipe.abidos.mat.abidos}") private int abidosMatAbidos;
    @Value("${recipe.abidos.mat.tender}") private int abidosMatTender;
    @Value("${recipe.abidos.mat.timber}") private int abidosMatTimber;

    @Value("${recipe.superior.cost}") private int superiorCost;
    @Value("${recipe.superior.mat.abidos}") private int superiorMatAbidos;
    @Value("${recipe.superior.mat.tender}") private int superiorMatTender;
    @Value("${recipe.superior.mat.timber}") private int superiorMatTimber;

    public CalculationResponse calculate(CalculationRequest request) {
        List<ScenarioResult> scenarios = new ArrayList<>();
        scenarios.add(calculateBruteSale(request));
        scenarios.add(calculateSturdyConversionIsolated(request));
        scenarios.add(calculateAbidosFusionIsolated(request));
        scenarios.add(calculateSuperiorFusionIsolated(request));

        String bestScenario = scenarios.stream()
                .max(Comparator.comparingDouble(ScenarioResult::netProfit))
                .map(ScenarioResult::name)
                .orElse("N/A");

        return new CalculationResponse(scenarios, bestScenario);
    }

    private double nvl(Double val) { return val == null ? 0 : val; }
    private long nvl(Long val) { return val == null ? 0 : val; }

    private ScenarioResult calculateBruteSale(CalculationRequest request) {
        UserInventory inv = request.inventory();
        MarketPrices prices = request.prices();
        double revenue = (nvl(inv.sturdyCount()) * nvl(prices.sturdyPrice()) / 100.0) +
                         (nvl(inv.abidosCount()) * nvl(prices.abidosPrice()) / 100.0) +
                         (nvl(inv.tenderCount()) * nvl(prices.tenderPrice()) / 100.0) +
                         (nvl(inv.timberCount()) * nvl(prices.timberPrice()) / 100.0);
        return new ScenarioResult("Cenário A: Venda Total", revenue * MARKET_TAX, 
            "Valor líquido de todo o inventário (Patrimônio).", 
            Map.of("Total Itens", (nvl(inv.sturdyCount()) + nvl(inv.abidosCount()) + nvl(inv.tenderCount()) + nvl(inv.timberCount()))));
    }

    private ScenarioResult calculateSturdyConversionIsolated(CalculationRequest request) {
        UserInventory inv = request.inventory();
        MarketPrices prices = request.prices();
        long totalTimber = nvl(inv.timberCount()) + (nvl(inv.sturdyCount()) * 10);
        long craftCount = Math.min(nvl(inv.abidosCount()) / abidosMatAbidos, Math.min(nvl(inv.tenderCount()) / abidosMatTender, totalTimber / abidosMatTimber));
        if (craftCount <= 0) return new ScenarioResult("Cenário B: Otimização Sturdy", 0, "Materiais insuficientes.", Map.of());
        double netProfit = ((craftCount * nvl(prices.fusionPrice())) * MARKET_TAX) - (craftCount * abidosCost);
        
        Map<String, Long> details = new LinkedHashMap<>();
        details.put("Total de Abidos Fusion produzidos", craftCount * 10);
        details.put("Custo de Fabricação (Gold)", (long)craftCount * abidosCost);
        details.put("Madeira Comum gerada (1:10)", nvl(inv.sturdyCount()) * 10);

        return new ScenarioResult("Cenário B: Otimização Sturdy", netProfit, "Maximiza craft de Abidos Fusion convertendo Sturdies (1:10).", details);
    }

    private ScenarioResult calculateAbidosFusionIsolated(CalculationRequest request) {
        UserInventory inv = request.inventory();
        MarketPrices prices = request.prices();
        long craftCount = Math.min(nvl(inv.abidosCount()) / abidosMatAbidos, Math.min(nvl(inv.tenderCount()) / abidosMatTender, nvl(inv.timberCount()) / abidosMatTimber));
        double netProfit = (craftCount > 0) ? (((craftCount * nvl(prices.fusionPrice())) * MARKET_TAX) - (craftCount * abidosCost)) : 0;
        
        Map<String, Long> details = new LinkedHashMap<>();
        if (craftCount > 0) {
            details.put("Total de Abidos Fusion produzidos", craftCount * 10);
            details.put("Custo de Fabricação (Gold)", (long)craftCount * abidosCost);
        }
        return new ScenarioResult("Cenário C: Abidos Fusion", netProfit, "Lucro isolado do craft de Abidos Fusion.", details);
    }

    private ScenarioResult calculateSuperiorFusionIsolated(CalculationRequest request) {
        UserInventory inv = request.inventory();
        MarketPrices prices = request.prices();
        long craftCount = Math.min(nvl(inv.abidosCount()) / superiorMatAbidos, Math.min(nvl(inv.tenderCount()) / superiorMatTender, nvl(inv.timberCount()) / superiorMatTimber));
        double netProfit = (craftCount > 0) ? (((craftCount * nvl(prices.superiorFusionPrice())) * MARKET_TAX) - (craftCount * superiorCost)) : 0;

        Map<String, Long> details = new LinkedHashMap<>();
        if (craftCount > 0) {
            details.put("Total de Superior Fusion produzidos", craftCount * 10);
            details.put("Custo de Fabricação (Gold)", (long)craftCount * superiorCost);
        }
        return new ScenarioResult("Cenário C: Superior Fusion", netProfit, "Lucro isolado do craft de Superior Abidos Fusion.", details);
    }
}
