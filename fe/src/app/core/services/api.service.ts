import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { environment } from '../../environment';
import { Observable, catchError, map, tap } from 'rxjs';
import { Project } from '../models/project.model';
import { Branch, BranchCompareResponse } from '../models/branch.model';
import { PaginatedResponse } from '../models/PaginatedResponse.model';
import { Group } from '../models/group.model';
import { Subgroup } from '../models/subgroup.model';
import { ReportFileInfo, ReportStatus } from './project.service';
import { CustomRule } from '../models/customRule.model';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // Groups API
  getGroups(
    page: number,
    pageSize: number,
    search?: string
  ): Observable<PaginatedResponse<Group>> {
    const safePage = (page ?? 0) + 1;
    const safePageSize = pageSize ?? 2;

    let params = new HttpParams()
      .set('page', safePage.toString())
      .set('size', safePageSize.toString());

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<PaginatedResponse<Group>>(
      `${this.apiUrl}/api/groups`,
      { params }
    );
  }

  // Subgroups API
  getSubgroups(
    groupId: number,
    page: number,
    pageSize: number,
    search?: string
  ): Observable<PaginatedResponse<Subgroup>> {
    const safePage = (page ?? 0) + 1;
    const safePageSize = pageSize ?? 2;

    let params = new HttpParams()
      .set('page', safePage.toString())
      .set('size', safePageSize.toString());

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<PaginatedResponse<Subgroup>>(
      `${this.apiUrl}/api/groups/${groupId}/subgroups`,
      { params }
    );
  }

  // Projects API
  getProjects(
    subgroupId: number,
    page: number,
    pageSize: number,
    search?: string
  ): Observable<PaginatedResponse<Project>> {
    const safePage = (page ?? 0) + 1;
    const safePageSize = pageSize ?? 2;

    let params = new HttpParams()
      .set('page', safePage.toString())
      .set('size', safePageSize.toString());

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<PaginatedResponse<Project>>(
      `${this.apiUrl}/api/groups/subgroups/${subgroupId}/projects`,
      { params }
    );
  }

  // Branches API
  getBranches(
    projectId: number,
    page: number,
    pageSize: number,
    defaultBranch: string,
    search?: string
  ): Observable<PaginatedResponse<Branch>> {
    const safePage = (page ?? 0) + 1;
    const safePageSize = pageSize ?? 2;

    let params = new HttpParams()
      .set('page', safePage.toString())
      .set('size', safePageSize.toString())
      .set('defaultBranch', defaultBranch);

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<PaginatedResponse<Branch>>(
      `${this.apiUrl}/api/projects/${projectId}/branches`,
      { params }
    );
  }

  generateBranchSummary(
    projectId: number,
    commitId: string,
    fromBranch: string,
    toBranch: string
  ): Observable<string> {
    return this.http.get(
      `${this.apiUrl}/api/projects/${projectId}/branches/compare/summary`,
      {
        params: new HttpParams()
          .set('commitId', commitId)
          .set('from', fromBranch)
          .set('to', toBranch),
        responseType: 'text',
      }
    );
  }

  generateProjectReport(projectData: {
    [key: string]: { name: string; defaultBranch: string };
  }): Observable<string> {
    console.log('Sending to API:', projectData);

    return this.http
      .post<string>(`${this.apiUrl}/api/projects/report`, projectData, {
        headers: new HttpHeaders({
          'Content-Type': 'application/json',
        }),
        responseType: 'text' as 'json',
      })
      .pipe(
        tap((response) => console.log('API Response:', response)),
        catchError((error) => {
          console.error('API Error details:', error);
          throw error;
        })
      );
  }

  getProjectReportStatus(reportId: string): Observable<ReportStatus> {
    return this.http.get<ReportStatus>(
      `${this.apiUrl}/api/projects/report/${reportId}/status`
    );
  }

  // Note: This is now a fallback method - the preferred approach is to use the URL from ReportStatus.downloadUrl
  downloadProjectReport(reportId: string): Observable<Blob> {
    return this.http.get(
      `${this.apiUrl}/api/projects/report/${reportId}/download`,
      { responseType: 'blob' }
    );
  }

  // New method to download a report by URL
  downloadReportByUrl(url: string): Observable<Blob> {
    return this.http.get(url, { responseType: 'blob' });
  }

  // New method to get all available reports
  getAvailableReports(): Observable<ReportFileInfo[]> {
    return this.http.get<ReportFileInfo[]>(`${this.apiUrl}/api/reports/files`);
  }

  checkBranchMergeStatus(
    projectId: number,
    sourceBranch: string,
    targetBranch: string
  ): Observable<BranchCompareResponse> {
    return this.http.get<BranchCompareResponse>(
      `${this.apiUrl}/api/projects/${projectId}/branches/compare`,
      {
        params: new HttpParams()
          .set('source', sourceBranch)
          .set('target', targetBranch),
      }
    );
  }

  getCustomRules(
    page: number,
    pageSize: number,
    parentGroupId: number,
    search?: string
  ): Observable<PaginatedResponse<CustomRule>> {
    const safePage = (page ?? 0) + 1;
    const safePageSize = pageSize ?? 10;

    let params = new HttpParams()
      .set('page', safePage.toString())
      .set('size', safePageSize.toString());

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    if (parentGroupId) {
      params = params.set('parentGroupId', parentGroupId.toString());
    }

    return this.http.get<PaginatedResponse<CustomRule>>(
      `${this.apiUrl}/api/custom-rules`,
      { params }
    );
  }
  createCustomRule(rule: Partial<CustomRule>): Observable<CustomRule> {
    return this.http.post<CustomRule>(`${this.apiUrl}/api/custom-rules`, rule);
  }

  updateCustomRule(
    id: number,
    rule: Partial<CustomRule>
  ): Observable<CustomRule> {
    return this.http.put<CustomRule>(
      `${this.apiUrl}/api/custom-rules/${id}`,
      rule
    );
  }

  deleteCustomRule(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/custom-rules/${id}`);
  }
}
