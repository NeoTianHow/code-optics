import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
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
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { SelectionModel } from '@angular/cdk/collections';
import { MatIconModule } from '@angular/material/icon';
import { Subgroup } from '../../core/models/subgroup.model';
import { ErrorHandlingService } from '../../core/services/errorHandling.service';
import { SubgroupService } from '../../core/services/subgroup.service';

import {
  Breadcrumb,
  BreadcrumbComponent,
} from '../../core/components/breadcrumb';

@Component({
  selector: 'app-subgroups',
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
  ],
  templateUrl: './subgroups.component.html',
  styleUrls: ['./subgroups.component.scss'],
})
export class SubgroupsComponent implements OnInit, OnDestroy {
  breadcrumbs: Breadcrumb[] = [];

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  dataSource = new MatTableDataSource<Subgroup>();
  pageSizeOptions = [5, 10, 15, 20];
  pageSize: number = this.pageSizeOptions[1];
  pageIndex: number = 0;
  totalItems: number = 0;

  selection = new SelectionModel<Subgroup>(true, []);
  searchControl = new FormControl('');
  groupId?: number;
  isLoading = false;
  error = '';

  displayedColumns: string[] = [
    'select',
    'name',
    'projectCount',
    'lastActivity',
    'actions',
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private errorHandler: ErrorHandlingService,
    private subgroupService: SubgroupService
  ) {}

  ngOnInit(): void {
    this.initParams();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initParams(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe({
      next: (params) => {
        // will be called when the component is first initialized and when the "route" params change
        const groupId = params['groupId'];
        if (groupId && !isNaN(+groupId)) {
          this.groupId = +groupId;
        }
      },
    });

    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe((params) => {
        // will be called when the component is first initialized and when the query and route params change
        this.pageIndex = Number(params['page']) || 0;
        this.pageSize = Number(params['size']) || this.pageSizeOptions[1];
        const search = params['search'] || '';
        this.searchControl.setValue(search, { emitEvent: false });

        // Set breadcrumbs
        const groupName = params['groupName'];
        if (groupName) {
          this.breadcrumbs = [
            {
              label: 'Groups',
              url: '/groups',
            },
            { label: groupName },
          ];
        }

        this.loadSubgroups(search);
      });
  }

  onSearch(): void {
    this.pageIndex = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    this.updateUrlAndLoadSubgroups(this.searchControl.value || '');
  }

  onSearchKeyup(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.onSearch();
    }
  }

  handlePageEvent(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.updateUrlAndLoadSubgroups(this.searchControl.value || '');
  }

  private updateUrlAndLoadSubgroups(search: string): void {
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
      },
    });
  }

  private loadSubgroups(search: string = ''): void {
    if (!this.groupId) {
      this.error = 'Group ID is not set.';
      return;
    }

    this.isLoading = true;
    this.error = '';

    this.subgroupService
      .getSubgroups(this.groupId, this.pageIndex, this.pageSize, search)
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
          console.error('Error loading subgroups:', err);
          this.error = this.errorHandler.getErrorMessage(
            err,
            'Error loading subgroups'
          );
        },
      });
  }
  onViewProjects(subgroupId: number, name: String): void {
    this.router.navigate(
      ['/groups', this.groupId, 'subgroups', subgroupId, 'projects'],
      {
        queryParams: {
          groupName: this.route.snapshot.queryParams['groupName'],
          subgroupName: name,
        },
      }
    );
  }

  onViewCustomRules(): void {
    this.router.navigate(['/codeReviewRules'], {
      queryParams: {
        parentGroupId: this.groupId,
        groupName: this.route.snapshot.queryParams['groupName'],
      },
    });
  }

  // Selection methods
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

  checkboxLabel(row?: Subgroup): string {
    if (!row) {
      return `${this.isAllSelected() ? 'deselect' : 'select'} all`;
    }
    return `${this.selection.isSelected(row) ? 'deselect' : 'select'} row ${
      row.name
    }`;
  }
}
