import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Group } from '../models/group.model';
import { ApiService } from './api.service';
import { PaginatedResponse } from '../models/PaginatedResponse.model';

@Injectable({
  providedIn: 'root',
})
export class GroupService {
  constructor(private apiService: ApiService) {}

  getGroups(
    page: number,
    pageSize: number,
    search?: string
  ): Observable<PaginatedResponse<Group>> {
    return this.apiService.getGroups(page, pageSize, search);
  }
}
