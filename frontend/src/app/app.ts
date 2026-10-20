import { Component } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, MatToolbarModule],
  template: `
    <mat-toolbar color="primary">
      <span>RegWatch</span>
      <span class="subtitle">Analyse d'impact réglementaire</span>
    </mat-toolbar>
    <main>
      <router-outlet />
    </main>
  `,
  styles: `
    .subtitle {
      margin-left: 16px;
      font-size: 14px;
      opacity: 0.7;
    }
    main {
      max-width: 960px;
      margin: 24px auto;
      padding: 0 16px;
    }
  `,
})
export class App {}
