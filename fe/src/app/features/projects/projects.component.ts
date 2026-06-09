import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Project } from '../../core/models/project.model';
import { CommonModule } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSortModule } from '@angular/material/sort';
import {
  MatPaginator,
  MatPaginatorModule,
  PageEvent,
} from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { ErrorHandlingService } from '../../core/services/errorHandling.service';
import { SelectionModel } from '@angular/cdk/collections';
import { MatListModule } from '@angular/material/list';
import {
  ProjectService,
  ReportStatus,
} from '../../core/services/project.service';
import {
  Breadcrumb,
  BreadcrumbComponent,
} from '../../core/components/breadcrumb';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatPaginatorModule,
    MatSortModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    ReactiveFormsModule,
    MatIconModule,
    BreadcrumbComponent,
    MatListModule,
    MatCardModule,
    MatTooltipModule,
  ],
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.scss'],
})
export class ProjectsComponent implements OnInit, OnDestroy {
  breadcrumbs: Breadcrumb[] = [];

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  dataSource = new MatTableDataSource<Project>();
  pageSizeOptions = [5, 10, 15, 20];
  pageSize: number = this.pageSizeOptions[1];
  pageIndex: number = 0;
  totalItems: number = 0;

  selection = new SelectionModel<Project>(true, []);
  isLoading = false;
  error = '';
  searchControl = new FormControl('');
  subgroupId?: number;
  groupId?: number;

  generatingReport = false;
  activeReports: Map<string, ReportStatus> = new Map();
  private statusCheckInterval: any;

  tableColumns: string[] = [
    'select',
    'name',
    'activeBranches',
    'staleBranches',
    'mergedBranches',
    'unmergedBranches',
    'lastActivityAt',
    'actions',
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private errorHandler: ErrorHandlingService,
    private projectService: ProjectService
  ) {}

  ngOnInit(): void {
    this.initParams();
    this.startStatusCheckInterval();

    // Copy any active reports from the service
    if (this.projectService.activeReports.size > 0) {
      this.projectService.activeReports.forEach((status, reportId) => {
        this.activeReports.set(reportId, status);
      });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.statusCheckInterval) {
      clearInterval(this.statusCheckInterval);
    }
  }

  private initParams(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe({
      next: (params) => {
        this.groupId = params['groupId'];
        this.subgroupId = params['subgroupId'];
      },
    });

    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe((params) => {
        this.pageIndex = Number(params['page']) || 0;
        this.pageSize = Number(params['size']) || this.pageSizeOptions[1];
        const search = params['search'] || '';
        this.searchControl.setValue(search, { emitEvent: false });

        const groupName = params['groupName'];
        const subgroupName = params['subgroupName'];
        if (groupName && subgroupName) {
          this.breadcrumbs = [
            {
              label: 'Groups',
              url: '/groups',
              queryParams: { groupName: groupName },
            },
            {
              label: groupName,
              url: `/groups/${this.groupId}/subgroups`,
              queryParams: { groupName: groupName },
            },
            { label: subgroupName },
          ];
        }

        this.loadProjects(search);
      });
  }

  private startStatusCheckInterval() {
    this.statusCheckInterval = setInterval(() => {
      this.activeReports.forEach((status, reportId) => {
        if (status.status !== 'COMPLETED' && status.status !== 'FAILED') {
          this.projectService.getReportStatus(reportId).subscribe({
            next: (updatedStatus) => {
              this.activeReports.set(reportId, updatedStatus);

              // Also update the service's copy
              this.projectService.activeReports.set(reportId, updatedStatus);
            },
          });
        }
      });
    }, 5000); // Check every 5 seconds
  }

  onSearch(): void {
    this.pageIndex = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    this.updateUrlAndLoadProjects(this.searchControl.value || '');
  }

  onSearchKeyup(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.onSearch();
    }
  }

  handlePageEvent(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.updateUrlAndLoadProjects(this.searchControl.value || '');
  }

  private updateUrlAndLoadProjects(search: string): void {
    const queryParams: { [key: string]: any } = {
      page: this.pageIndex,
      size: this.pageSize,
    };

    if (search?.trim()) {
      queryParams['search'] = search.trim();
    }

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        // Preserve the breadcrumb params
        ...queryParams,
        groupName: this.route.snapshot.queryParams['groupName'],
        subgroupName: this.route.snapshot.queryParams['subgroupName'],
      },
    });
  }

  private loadProjects(search: string = ''): void {
    if (!this.subgroupId) {
      this.error = 'Subgroup ID is not set.';
      return;
    }

    this.isLoading = true;
    this.error = '';

    this.projectService
      .getProjects(this.subgroupId, this.pageIndex, this.pageSize, search)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.isLoading = false;
        })
      )
      .subscribe({
        next: (response) => {
          if (response.items) {
            this.dataSource = new MatTableDataSource(response.items);
            this.totalItems = response.totalItems;

            console.log(this.dataSource);

            if (this.paginator) {
              this.paginator.pageIndex = this.pageIndex;
              this.paginator.pageSize = this.pageSize;
            }
          } else {
            this.dataSource = new MatTableDataSource();
            this.totalItems = 0;
          }
        },
        error: (err) => {
          console.error('Error loading projects:', err);
          this.error = this.errorHandler.getErrorMessage(
            err,
            'Error loading projects'
          );
        },
      });
  }

  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
      return;
    }
    this.selection.select(...this.dataSource.data);
  }

  checkboxLabel(row?: Project): string {
    if (!row) {
      return `${this.isAllSelected() ? 'deselect' : 'select'} all`;
    }
    return `${this.selection.isSelected(row) ? 'deselect' : 'select'} row ${
      row.name
    }`;
  }

  onViewBranches(project: Project): void {
    this.router.navigate(
      [
        '/groups',
        this.groupId,
        'subgroups',
        this.subgroupId,
        'projects',
        project.id,
        'branches',
      ],
      {
        queryParams: {
          groupName: this.route.snapshot.queryParams['groupName'],
          subgroupName: this.route.snapshot.queryParams['subgroupName'],
          projectName: project.name,
          defaultBranch: project.defaultBranch,
        },
      }
    );
  }

  generateSelectedReport() {
    console.log('Generate report clicked');
    const selectedProjects = this.selection.selected;
    console.log('Selected projects:', selectedProjects);

    if (selectedProjects.length === 0) {
      this.error = 'Please select at least one project.';
      return;
    }

    this.generatingReport = true;
    this.error = '';

    this.projectService.generateReport(selectedProjects).subscribe({
      next: (reportId) => {
        console.log('Report generation started with ID:', reportId);

        const initialStatus: ReportStatus = {
          reportId,
          status: 'PENDING',
          startedAt: new Date().toISOString(),
          progress: 0,
          currentStep: 'Initializing',
        };

        this.activeReports.set(reportId, initialStatus);
        this.projectService.activeReports.set(reportId, initialStatus);

        console.log('Report generation started');
      },
      error: (err) => {
        console.error('Report generation error:', err);
        this.error = this.errorHandler.getErrorMessage(
          err,
          'Error initiating report generation'
        );
        this.generatingReport = false;
      },
      complete: () => {
        this.generatingReport = false;
      },
    });
  }
}
