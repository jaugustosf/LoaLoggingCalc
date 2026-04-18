import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MarketPrices {
  sturdyPrice: number;
  abidosPrice: number;
  tenderPrice: number;
  timberPrice: number;
  fusionPrice: number;
  superiorFusionPrice: number;
  craftCostGold: number;
  superiorCraftCostGold: number;
}

export interface UserInventory {
  sturdyCount: number;
  abidosCount: number;
  tenderCount: number;
  timberCount: number;
}

export interface CalculationRequest {
  prices: MarketPrices;
  inventory: UserInventory;
}

export interface ScenarioResult {
  name: string;
  netProfit: number;
  description: string;
  details: any;
}

export interface CalculationResponse {
  scenarios: ScenarioResult[];
  bestScenarioName: string;
}

@Injectable({
  providedIn: 'root'
})
export class CalculationService {
  private apiUrl = 'http://localhost:8080/api/v1/roi/calculate';

  constructor(private http: HttpClient) { }

  calculate(request: CalculationRequest): Observable<CalculationResponse> {
    return this.http.post<CalculationResponse>(this.apiUrl, request);
  }
}
