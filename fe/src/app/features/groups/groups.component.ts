import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Group } from '../../core/models/group.model';
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
import { ErrorHandlingService } from '../../core/services/errorHandling.service';
import { SelectionModel } from '@angular/cdk/collections';
import { GroupService } from '../../core/services/group.service';
import { MatIconModule } from '@angular/material/icon';
import {
  Breadcrumb,
  BreadcrumbComponent,
} from '../../core/components/breadcrumb';

@Component({
  selector: 'app-groups',
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
  templateUrl: './groups.component.html',
  styleUrls: ['./groups.component.scss'],
})
export class GroupsComponent implements OnInit, OnDestroy {
  breadcrumbs: Breadcrumb[] = [{ label: 'Groups' }];

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  dataSource = new MatTableDataSource<Group>();
  pageSizeOptions = [5, 10, 15, 20];
  pageSize: number = this.pageSizeOptions[0];
  pageIndex: number = 0;
  totalItems: number = 0;

  selection = new SelectionModel<Group>(true, []);
  isLoading = false;
  error = '';
  searchControl = new FormControl('');

  displayedColumns: string[] = [
    'select',
    'name',
    'subgroupCount',
    'lastActivity',
    'actions',
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private errorHandler: ErrorHandlingService,
    private groupService: GroupService
  ) {}

  ngOnInit(): void {
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe((params) => {
        // will be called when the component is first initialized and when the query and route params change
        this.pageIndex = Number(params['page']) || 0;
        this.pageSize = Number(params['size']) || this.pageSizeOptions[0];
        const search = params['search'] || '';
        this.searchControl.setValue(search, { emitEvent: false });

        this.loadGroups(this.searchControl.value || '');
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearch(): void {
    this.pageIndex = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    this.updateUrlAndLoadGroups(this.searchControl.value || '');
  }

  onSearchKeyup(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.onSearch();
    }
  }

  handlePageEvent(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.updateUrlAndLoadGroups(this.searchControl.value || '');
  }

  private updateUrlAndLoadGroups(search: string): void {
    const queryParams: { [key: string]: any } = {
      page: this.pageIndex,
      size: this.pageSize,
    };

    if (search && search.trim() !== '') {
      queryParams['search'] = search;
    }

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
    });
  }

  private loadGroups(search: string = ''): void {
    console.log('loadGroups');
    this.isLoading = true;
    this.error = '';

    this.groupService
      .getGroups(this.pageIndex, this.pageSize, search)
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

            console.log('dataSource', this.dataSource);

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
          this.error = this.errorHandler.getErrorMessage(
            err,
            'Error loading groups'
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

  checkboxLabel(row?: Group): string {
    if (!row) {
      return `${this.isAllSelected() ? 'deselect' : 'select'} all`;
    }
    return `${this.selection.isSelected(row) ? 'deselect' : 'select'} row ${
      row.name
    }`;
  }

  onViewSubgroups(group: Group): void {
    // groupid is passed as route param and group name is passed as query param
    this.router.navigate(['/groups', group.id, 'subgroups'], {
      queryParams: { groupName: group.name },
    });
  }
}
