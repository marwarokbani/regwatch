import { KeyValuePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { SystemApi } from '../../core/api/system-api';

/** Page d'accueil provisoire (M1) : état des services de la stack. */
@Component({
  selector: 'app-status',
  imports: [MatCardModule, KeyValuePipe],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title>État des services</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        @if (health(); as h) {
          <p>
            Global : <strong [class]="h.status">{{ h.status }}</strong>
          </p>
          <ul>
            @for (c of h.components | keyvalue; track c.key) {
              <li>
                {{ c.key }} : <strong [class]="c.value.status">{{ c.value.status }}</strong>
              </li>
            }
          </ul>
        } @else {
          <p>Chargement…</p>
        }
        <h3>Chaîne LLM (ordre de fallback)</h3>
        <ol>
          @for (p of providers(); track p) {
            <li>{{ p }}</li>
          } @empty {
            <li>Indisponible</li>
          }
        </ol>
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    .UP {
      color: #2e7d32;
    }
    .DOWN {
      color: #c62828;
    }
  `,
})
export class Status {
  private readonly api = inject(SystemApi);

  protected readonly health = toSignal(this.api.health());
  protected readonly providers = toSignal(this.api.llmProviders(), { initialValue: [] });
}
