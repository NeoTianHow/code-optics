import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

export interface Breadcrumb {
  label: string;
  url?: string;
  queryParams?: { [key: string]: any };
}

@Component({
  selector: 'app-breadcrumb',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule],
  template: `
    <nav class="breadcrumb-container">
      <ol class="breadcrumb-list">
        <li class="breadcrumb-item">
          <a [routerLink]="['/']" class="home-link">
            <img src="/icons/home.png" alt="Home" class="icon" />
          </a>
        </li>
        <ng-container *ngFor="let item of items; let last = last">
          <li class="breadcrumb-separator">
            <img
              src="/icons/chevron_right.png"
              alt="Chevron Right"
              class="icon"
            />
          </li>
          <li class="breadcrumb-item">
            <ng-container *ngIf="!last && item.url">
              <a
                [routerLink]="[item.url]"
                [queryParams]="item.queryParams"
                class="breadcrumb-link"
              >
                {{ item.label }}
              </a>
            </ng-container>
            <ng-container *ngIf="last || !item.url">
              <span class="breadcrumb-text">{{ item.label }}</span>
            </ng-container>
          </li>
        </ng-container>
      </ol>
    </nav>
  `,
  styles: [
    `
      .icon {
        font-size: 1.25rem;
        width: 1.25rem;
        height: 1.25rem;
        opacity: 0.7;
      }

      .breadcrumb-container {
        padding: 0;
        width: 70%;
        margin: 0 auto 1rem;
      }

      .breadcrumb-list {
        display: flex;
        align-items: center;
        list-style: none;
        padding: 0;
        margin: 0;
      }

      .breadcrumb-item {
        display: flex;
        align-items: center;
        color: #4a5568;
        font-size: 0.875rem;
      }

      .breadcrumb-separator {
        display: flex;
        align-items: center;
        color: #9ca3af;

        .icon {
          font-size: 1.25rem;
          width: 1.25rem;
          height: 1.25rem;
        }
      }

      .breadcrumb-link {
        color: #3b82f6;
        text-decoration: none;
        transition: color 0.2s ease;

        &:hover {
          color: #2563eb;
          text-decoration: underline;
        }
      }

      .breadcrumb-text {
        color: #4a5568;
      }

      .home-link {
        color: #4a5568;
        text-decoration: none;
        display: flex;
        align-items: center;
        transition: color 0.2s ease;

        .mat-icon {
          font-size: 1.25rem;
          width: 1.25rem;
          height: 1.25rem;
        }
      }
    `,
  ],
})
export class BreadcrumbComponent {
  @Input() items: Breadcrumb[] = [];
}
