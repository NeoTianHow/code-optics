import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import { Branch, BranchCompareResponse } from '../models/branch.model';
import { PaginatedResponse } from '../models/PaginatedResponse.model';

@Injectable({
  providedIn: 'root',
})
export class BranchService {
  constructor(private apiService: ApiService) {}

  getBranches(
    projectId: number,
    page: number,
    pageSize: number,
    defaultBranch: string,
    search?: string
  ): Observable<PaginatedResponse<Branch>> {
    return this.apiService.getBranches(
      projectId,
      page,
      pageSize,
      defaultBranch,
      search
    );
  }

  generateBranchSummary(
    projectId: number,
    commitId: string,
    fromBranch: string,
    toBranch: string
  ): Observable<string> {
    return this.apiService.generateBranchSummary(
      projectId,
      commitId,
      fromBranch,
      toBranch
    );
  }

  checkBranchMergeStatus(
    projectId: number,
    sourceBranch: string,
    targetBranch: string
  ): Observable<BranchCompareResponse> {
    return this.apiService.checkBranchMergeStatus(
      projectId,
      sourceBranch,
      targetBranch
    );
  }
}
