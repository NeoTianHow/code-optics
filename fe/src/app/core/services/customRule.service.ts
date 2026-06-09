import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment';
import { CustomRule } from '../models/customRule.model';
import { PaginatedResponse } from '../models/PaginatedResponse.model';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root',
})
export class CustomRuleService {
  constructor(private apiService: ApiService) {}

  getRules(
    page: number,
    pageSize: number,
    parentGroupId: number,
    search?: string
  ): Observable<PaginatedResponse<CustomRule>> {
    return this.apiService.getCustomRules(
      page,
      pageSize,
      parentGroupId,
      search
    );
  }
  createRule(rule: Partial<CustomRule>): Observable<CustomRule> {
    return this.apiService.createCustomRule(rule);
  }

  updateRule(id: number, rule: Partial<CustomRule>): Observable<CustomRule> {
    return this.apiService.updateCustomRule(id, rule);
  }

  deleteRule(id: number): Observable<void> {
    return this.apiService.deleteCustomRule(id);
  }
}
