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
        
        double sturdyVal = (nvl(inv.sturdyCount()) * nvl(prices.sturdyPrice()) / 100.0) * MARKET_TAX;
        double abidosVal = (nvl(inv.abidosCount()) * nvl(prices.abidosPrice()) / 100.0) * MARKET_TAX;
        double tenderVal = (nvl(inv.tenderCount()) * nvl(prices.tenderPrice()) / 100.0) * MARKET_TAX;
        double timberVal = (nvl(inv.timberCount()) * nvl(prices.timberPrice()) / 100.0) * MARKET_TAX;
        
        double totalRevenue = sturdyVal + abidosVal + tenderVal + timberVal;

        Map<String, Long> details = new LinkedHashMap<>();
        details.put("Valor líquido Sturdy (Gold)", (long)sturdyVal);
        details.put("Valor líquido Abidos Timber (Gold)", (long)abidosVal);
        details.put("Valor líquido Tender (Gold)", (long)tenderVal);
        details.put("Valor líquido Timber Comum (Gold)", (long)timberVal);
        details.put("Total de itens processados", (nvl(inv.sturdyCount()) + nvl(inv.abidosCount()) + nvl(inv.tenderCount()) + nvl(inv.timberCount())));

        return new ScenarioResult("Cenário A: Venda Total Bruta", totalRevenue, 
            "Valor líquido se você vender todo o seu inventário agora no mercado.", details);
    }

    private ScenarioResult calculateSturdyConversionIsolated(CalculationRequest request) {
        UserInventory inv = request.inventory();
        MarketPrices prices = request.prices();
        
        double reduction = 1.0 - (nvl(prices.costReductionPercentage()) / 100.0);
        double effectiveAbidosCost = abidosCost * reduction;
        double effectiveSuperiorCost = superiorCost * reduction;

        // 1. Converter Sturdy para Timber (1:10) conforme solicitado
        long totalTimber = nvl(inv.timberCount()) + (nvl(inv.sturdyCount()) * 10);
        long totalTender = nvl(inv.tenderCount());
        long totalAbidos = nvl(inv.abidosCount());

        // 2. Lógica de Troca (Dust/NPC): 
        // Em vez de compra, vamos considerar que o usuário "mói" o excesso para pegar o que falta.
        // Como o Abidos Timber é o gargalo, vamos ver quanto Timber/Tender sobram e converter em Abidos Timber.
        // Taxa média de conversão via NPC (aproximada): 80 unidades de comum/incomum valem ~10 de épico (Abidos Timber)
        
        // Vamos tentar equilibrar os materiais para maximizar crafts
        // Superior Fusion precisa de: 43 Abidos Timber, 59 Tender, 112 Timber
        
        long superiorCrafts = 0;
        long tempAbidos = totalAbidos;
        long tempTender = totalTender;
        long tempTimber = totalTimber;

        // Loop de simulação de craft com troca interna
        while (true) {
            // Se falta Abidos Timber, tenta converter excesso de Timber (proporção 10:1)
            if (tempAbidos < superiorMatAbidos && tempTimber > (superiorMatTimber + 100)) {
                tempTimber -= 100;
                tempAbidos += 10;
                continue;
            }
            // Se falta Abidos Timber, tenta converter excesso de Tender (proporção 5:1)
            if (tempAbidos < superiorMatAbidos && tempTender > (superiorMatTender + 50)) {
                tempTender -= 50;
                tempAbidos += 10;
                continue;
            }

            if (tempAbidos >= superiorMatAbidos && tempTender >= superiorMatTender && tempTimber >= superiorMatTimber) {
                superiorCrafts++;
                tempAbidos -= superiorMatAbidos;
                tempTender -= superiorMatTender;
                tempTimber -= superiorMatTimber;
            } else {
                break;
            }
        }

        // Repetir para Abidos Fusion com o que sobrou
        long abidosCrafts = 0;
        while (true) {
            if (tempAbidos < abidosMatAbidos && tempTimber > (abidosMatTimber + 100)) {
                tempTimber -= 100;
                tempAbidos += 10;
                continue;
            }
            if (tempAbidos >= abidosMatAbidos && tempTender >= abidosMatTender && tempTimber >= abidosMatTimber) {
                abidosCrafts++;
                tempAbidos -= abidosMatAbidos;
                tempTender -= abidosMatTender;
                tempTimber -= abidosMatTimber;
            } else {
                break;
            }
        }

        double revenueSuperior = (superiorCrafts * 10 * nvl(prices.superiorFusionPrice()) / 10.0) * MARKET_TAX;
        double revenueAbidos = (abidosCrafts * 10 * nvl(prices.fusionPrice()) / 10.0) * MARKET_TAX;
        double totalCost = (superiorCrafts * effectiveSuperiorCost) + (abidosCrafts * effectiveAbidosCost);
        double netProfit = (revenueSuperior + revenueAbidos) - totalCost;

        Map<String, Long> details = new LinkedHashMap<>();
        details.put("Superior Fusion Produzidos", superiorCrafts * 10);
        details.put("Abidos Fusion Produzidos", abidosCrafts * 10);
        details.put("Valor Total das Vendas (Líquido)", (long)(revenueSuperior + revenueAbidos));
        details.put("Custo de Fabricação (Gold)", (long)totalCost);
        details.put("Abidos Timber 'criados' via troca", (long)((superiorCrafts * superiorMatAbidos + abidosCrafts * abidosMatAbidos) - totalAbidos));

        return new ScenarioResult("Cenário B: Craft via Trocas (Dust)", netProfit, 
            "Calcula o máximo de fusions trocando materiais excedentes no NPC (sem comprar nada).", details);
    }

    private ScenarioResult calculateAbidosFusionIsolated(CalculationRequest request) {
        UserInventory inv = request.inventory();
        MarketPrices prices = request.prices();
        double reduction = 1.0 - (nvl(prices.costReductionPercentage()) / 100.0);
        double effectiveCost = abidosCost * reduction;

        long craftCount = Math.min(nvl(inv.abidosCount()) / abidosMatAbidos, Math.min(nvl(inv.tenderCount()) / abidosMatTender, nvl(inv.timberCount()) / abidosMatTimber));
        double netProfit = (craftCount > 0) ? (((craftCount * nvl(prices.fusionPrice())) * MARKET_TAX) - (craftCount * effectiveCost)) : 0;
        
        Map<String, Long> details = new LinkedHashMap<>();
        if (craftCount > 0) {
            details.put("Total de Abidos Fusion produzidos", craftCount * 10);
            details.put("Custo de Fabricação (Gold)", (long)(craftCount * effectiveCost));
        }
        return new ScenarioResult("Cenário C: Craft Direto Abidos (Sem Trocas)", netProfit, "Lucro usando apenas materiais que você já tem em mãos, sem trocas ou conversões.", details);
    }

    private ScenarioResult calculateSuperiorFusionIsolated(CalculationRequest request) {
        UserInventory inv = request.inventory();
        MarketPrices prices = request.prices();
        double reduction = 1.0 - (nvl(prices.costReductionPercentage()) / 100.0);
        double effectiveCost = superiorCost * reduction;

        long craftCount = Math.min(nvl(inv.abidosCount()) / superiorMatAbidos, Math.min(nvl(inv.tenderCount()) / superiorMatTender, nvl(inv.timberCount()) / superiorMatTimber));
        double netProfit = (craftCount > 0) ? (((craftCount * nvl(prices.superiorFusionPrice())) * MARKET_TAX) - (craftCount * effectiveCost)) : 0;

        Map<String, Long> details = new LinkedHashMap<>();
        if (craftCount > 0) {
            details.put("Total de Superior Fusion produzidos", craftCount * 10);
            details.put("Custo de Fabricação (Gold)", (long)(craftCount * effectiveCost));
        }
        return new ScenarioResult("Cenário C: Craft Direto Superior (Sem Trocas)", netProfit, "Lucro usando apenas materiais que você já tem em mãos, sem trocas ou conversões.", details);
    }
}
