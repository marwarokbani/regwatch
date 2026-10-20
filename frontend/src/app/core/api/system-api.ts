import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';

export interface HealthComponent {
  status: 'UP' | 'DOWN' | 'UNKNOWN';
  details?: Record<string, unknown>;
}

export interface HealthResponse {
  status: 'UP' | 'DOWN' | 'UNKNOWN';
  components?: Record<string, HealthComponent>;
}

/** Appels de diagnostic du backend (santé des services, chaîne LLM). */
@Injectable({ providedIn: 'root' })
export class SystemApi {
  private readonly http = inject(HttpClient);

  health(): Observable<HealthResponse> {
    // Actuator renvoie 503 (avec le même corps JSON) dès qu'un composant est DOWN :
    // on récupère ce corps au lieu de traiter la réponse comme une erreur.
    return this.http
      .get<HealthResponse>('/health')
      .pipe(catchError((err) => of<HealthResponse>(err?.error?.status ? err.error : { status: 'DOWN' })));
  }

  llmProviders(): Observable<string[]> {
    return this.http.get<string[]>('/api/llm/providers').pipe(catchError(() => of([])));
  }
}
