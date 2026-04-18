import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CalculationService, CalculationResponse } from '../../services/calculation';
import { debounceTime } from 'rxjs/operators';

@Component({
  selector: 'app-calculator',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './calculator.html',
  styleUrl: './calculator.css'
})
export class CalculatorComponent implements OnInit {
  roiForm!: FormGroup;
  result: CalculationResponse | null = null;
  loading = false;
  error: string | null = null;
  private readonly STORAGE_KEY = 'loa_logging_calc_data';

  constructor(private fb: FormBuilder, private calcService: CalculationService) {}

  ngOnInit(): void {
    this.roiForm = this.fb.group({
      inventory: this.fb.group({
        sturdyCount: [0, [Validators.required, Validators.min(0)]],
        abidosCount: [0, [Validators.required, Validators.min(0)]],
        tenderCount: [0, [Validators.required, Validators.min(0)]],
        timberCount: [0, [Validators.required, Validators.min(0)]]
      }),
      prices: this.fb.group({
        sturdyPrice: [0, [Validators.required, Validators.min(0)]],
        abidosPrice: [0, [Validators.required, Validators.min(0)]],
        tenderPrice: [0, [Validators.required, Validators.min(0)]],
        timberPrice: [0, [Validators.required, Validators.min(0)]],
        fusionPrice: [0, [Validators.required, Validators.min(0)]],
        superiorFusionPrice: [0, [Validators.required, Validators.min(0)]]
      })
    });

    this.loadSavedData();

    // Auto-save ao alterar valores
    this.roiForm.valueChanges.pipe(debounceTime(500)).subscribe(val => {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(val));
    });
  }

  private loadSavedData(): void {
    const saved = localStorage.getItem(this.STORAGE_KEY);
    if (saved) {
      try {
        this.roiForm.patchValue(JSON.parse(saved));
      } catch (e) {
        console.error("Erro ao carregar dados salvos", e);
      }
    }
  }

  onSubmit(): void {
    if (this.roiForm.valid) {
      this.loading = true;
      this.error = null;
      this.calcService.calculate(this.roiForm.value).subscribe({
        next: (res) => {
          this.result = res;
          this.loading = false;
        },
        error: (err) => {
          this.error = "Erro ao calcular ROI. O backend está rodando?";
          this.loading = false;
        }
      });
    }
  }
}
