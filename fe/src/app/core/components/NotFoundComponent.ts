import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CommonModule, MatButtonModule],
  template: `
    <div class="not-found-container">
      <div class="content">
        <h1>404</h1>
        <h2>Page Not Found</h2>
        <p>The page you're looking for doesn't exist or has been moved.</p>
        <button mat-flat-button color="primary" (click)="goHome()">
          Return to Home
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .not-found-container {
        min-height: 100vh;
        display: flex;
        justify-content: center;
        align-items: center;
        background-color: #f9fafb;
      }

      .content {
        text-align: center;
        padding: 2rem;
      }

      h1 {
        font-size: 8rem;
        font-weight: 700;
        color: #3b82f6;
        margin: 0;
        line-height: 1;
      }

      h2 {
        font-size: 2rem;
        color: #4b5563;
        margin: 1rem 0;
      }

      p {
        color: #6b7280;
        margin-bottom: 2rem;
        font-size: 1.1rem;
      }

      button {
        padding: 0.75rem 1.5rem;
        font-size: 1rem;
      }
    `,
  ],
})
export class NotFoundComponent {
  constructor(private router: Router) {}

  goHome(): void {
    this.router.navigate(['/']);
  }
}
