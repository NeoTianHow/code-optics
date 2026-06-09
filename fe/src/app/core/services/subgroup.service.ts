import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Subgroup } from '../models/subgroup.model';
import { ApiService } from './api.service';
import { PaginatedResponse } from '../models/PaginatedResponse.model';

@Injectable({
  providedIn: 'root',
})
export class SubgroupService {
  constructor(private apiService: ApiService) {}

  getSubgroups(
    groupId: number,
    page: number,
    pageSize: number,
    search?: string
  ): Observable<PaginatedResponse<Subgroup>> {
    return this.apiService.getSubgroups(groupId, page, pageSize, search);
  }
}
