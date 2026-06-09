import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;
  const apiUrl = 'http://localhost:8080';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should request groups with one-based page numbers and trimmed search', () => {
    service.getGroups(0, 25, '  platform  ').subscribe((response) => {
      expect(response.items).toEqual([]);
      expect(response.currentPage).toBe(1);
    });

    const req = httpMock.expectOne(
      (request) =>
        request.method === 'GET' &&
        request.url === `${apiUrl}/api/groups` &&
        request.params.get('page') === '1' &&
        request.params.get('size') === '25' &&
        request.params.get('search') === 'platform'
    );

    req.flush({
      items: [],
      currentPage: 1,
      pageSize: 25,
      totalPages: 0,
      totalItems: 0,
    });
  });

  it('should omit blank search terms from project requests', () => {
    service.getProjects(12, 2, 10, '   ').subscribe();

    const req = httpMock.expectOne(
      (request) =>
        request.method === 'GET' &&
        request.url === `${apiUrl}/api/groups/subgroups/12/projects` &&
        request.params.get('page') === '3' &&
        request.params.get('size') === '10' &&
        !request.params.has('search')
    );

    expect(req.request.params.has('search')).toBeFalse();
    req.flush({
      items: [],
      currentPage: 3,
      pageSize: 10,
      totalPages: 0,
      totalItems: 0,
    });
  });

  it('should include parent group id and trimmed search when loading custom rules', () => {
    service.getCustomRules(1, 20, 99, '  naming  ').subscribe();

    const req = httpMock.expectOne(
      (request) =>
        request.method === 'GET' &&
        request.url === `${apiUrl}/api/custom-rules` &&
        request.params.get('page') === '2' &&
        request.params.get('size') === '20' &&
        request.params.get('parentGroupId') === '99' &&
        request.params.get('search') === 'naming'
    );

    expect(req.request.params.get('parentGroupId')).toBe('99');
    req.flush({
      items: [],
      currentPage: 2,
      pageSize: 20,
      totalPages: 0,
      totalItems: 0,
    });
  });

  it('should request branch summaries as text responses', () => {
    service
      .generateBranchSummary(123, 'abc123', 'feature', 'main')
      .subscribe((summary) => expect(summary).toBe('summary text'));

    const req = httpMock.expectOne(
      (request) =>
        request.method === 'GET' &&
        request.url === `${apiUrl}/api/projects/123/branches/compare/summary` &&
        request.params.get('commitId') === 'abc123' &&
        request.params.get('from') === 'feature' &&
        request.params.get('to') === 'main' &&
        request.responseType === 'text'
    );

    req.flush('summary text');
  });
});
