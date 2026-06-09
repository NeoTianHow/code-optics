import { Injectable } from '@angular/core';
import { Observable, catchError, of, tap } from 'rxjs';
import { Project } from '../models/project.model';
import { ApiService } from './api.service';
import { PaginatedResponse } from '../models/PaginatedResponse.model';

export interface ReportStatus {
  reportId: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  startedAt: string;
  completedAt?: string;
  error?: string;
  filename?: string;
  downloadUrl?: string;
  progress: number;
  currentStep: string;
  // Add the new formatted date properties
  formattedStartTime?: string;
  formattedCompletedTime?: string;
}

export interface ReportFileInfo {
  reportId: string;
  filename: string;
  generatedAt: string;
  fileSize: number;
  downloadUrl: string;
}

@Injectable({
  providedIn: 'root',
})
export class ProjectService {
  activeReports: Map<string, ReportStatus> = new Map();
  availableReports: ReportFileInfo[] = [];

  constructor(private apiService: ApiService) {
    // Load any existing reports when the service starts
    this.loadAvailableReports();
  }

  getProjects(
    subgroupId: number,
    page: number,
    pageSize: number,
    search?: string
  ): Observable<PaginatedResponse<Project>> {
    return this.apiService.getProjects(subgroupId, page, pageSize, search);
  }

  generateReport(projects: Project[]): Observable<string> {
    console.log('ProjectService.generateReport called with:', projects);

    // Create a payload with project ID, name, and default branch
    const requestPayload: { [key: string]: any } = {};
    projects.forEach((project) => {
      requestPayload[project.id.toString()] = {
        name: project.name,
        defaultBranch: project.defaultBranch,
      };
    });

    console.log('Formatted request payload:', JSON.stringify(requestPayload));
    return this.apiService.generateProjectReport(requestPayload);
  }

  getReportStatus(reportId: string): Observable<ReportStatus> {
    return this.apiService.getProjectReportStatus(reportId).pipe(
      tap((status) => {
        // Update the active reports map
        this.activeReports.set(reportId, status);

        // If report is completed, refresh available reports list
        if (status.status === 'COMPLETED') {
          this.loadAvailableReports();
        }
      }),
      catchError((error) => {
        console.error('Error fetching report status:', error);
        return of({
          reportId,
          status: 'FAILED',
          startedAt: new Date().toISOString(),
          error: 'Error fetching report status',
          progress: 0,
          currentStep: 'Failed to fetch status',
        } as ReportStatus);
      })
    );
  }

  downloadReport(reportId: string): Observable<Blob> {
    const status = this.activeReports.get(reportId);

    // If we have a direct download URL, use it
    if (status?.downloadUrl) {
      return this.apiService.downloadReportByUrl(status.downloadUrl);
    }

    // Fall back to the old method
    return this.apiService.downloadProjectReport(reportId);
  }

  // New method to download a report by URL
  downloadReportByUrl(url: string): Observable<Blob> {
    return this.apiService.downloadReportByUrl(url);
  }

  // Load all available reports
  loadAvailableReports(): void {
    this.apiService.getAvailableReports().subscribe({
      next: (reports) => {
        this.availableReports = reports;
      },
      error: (err) => {
        console.error('Error loading available reports:', err);
      },
    });
  }

  // Get all available reports
  getAvailableReports(): Observable<ReportFileInfo[]> {
    return this.apiService.getAvailableReports();
  }
}
