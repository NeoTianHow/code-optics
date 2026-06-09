import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class ErrorHandlingService {
  /**
   * Extract a user-friendly message from various error types
   */
  getErrorMessage(error: unknown, fallbackMsg = 'Unknown error'): string {
    if (error instanceof HttpErrorResponse) {
      // Prefer server-provided error message, or fallback to status text
      return error.error?.message || error.message || fallbackMsg;
    } else if (error instanceof Error) {
      return error.message;
    } else if (typeof error === 'string') {
      return error;
    }
    return fallbackMsg;
  }
}
