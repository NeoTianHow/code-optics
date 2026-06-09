import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import {
  MatPaginator,
  MatPaginatorModule,
  PageEvent,
} from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { CustomRule } from '../../core/models/customRule.model';
import { CustomRuleService } from '../../core/services/customRule.service';
import { ErrorHandlingService } from '../../core/services/errorHandling.service';
import { AddCustomRuleDialogComponent } from '../../core/components/AddCustomRuleDialogComponent';
import { EditCustomRuleDialogComponent } from '../../core/components/EditCustomRuleDialogComponent';
import { MatIconModule } from '@angular/material/icon';
import {
  BreadcrumbComponent,
  Breadcrumb,
} from '../../core/components/breadcrumb';

@Component({
  selector: 'app-custom-rules',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatIconModule,
    BreadcrumbComponent,
  ],
  templateUrl: './customRules.component.html',
  styleUrls: ['./customRules.component.scss'],
})
export class CustomRulesComponent implements OnInit, OnDestroy {
  breadcrumbs: Breadcrumb[] = [{ label: 'Custom Rules' }];

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  dataSource = new MatTableDataSource<CustomRule>();
  pageSizeOptions = [5, 10, 15, 20];
  pageSize: number = this.pageSizeOptions[1];
  pageIndex: number = 0;
  totalItems: number = 0;

  isLoading = false;
  error = '';
  searchControl = new FormControl('');

  parentGroupId?: number;
  parentGroupName?: string;

  displayedColumns: string[] = [
    'description',
    'severity',
    'category',
    'enabled',
    'actions',
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private dialog: MatDialog,
    private errorHandler: ErrorHandlingService,
    private customRuleService: CustomRuleService
  ) {}

  ngOnInit(): void {
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe((params) => {
        this.pageIndex = Number(params['page']) || 0;
        this.pageSize = Number(params['size']) || this.pageSizeOptions[1];
        const search = params['search'] || '';
        this.searchControl.setValue(search, { emitEvent: false });

        // Get the parent group ID from query params if it exists
        const parentGroupId = params['parentGroupId'];
        if (parentGroupId && !isNaN(+parentGroupId)) {
          this.parentGroupId = +parentGroupId;
          this.parentGroupName = params['groupName'];

          // Update breadcrumbs
          if (this.parentGroupName) {
            this.breadcrumbs = [
              {
                label: 'Groups',
                url: '/groups',
                queryParams: { groupName: this.parentGroupName },
              },
              {
                label: this.parentGroupName,
                url: `/groups/${this.parentGroupId}/subgroups`,
                queryParams: { groupName: this.parentGroupName },
              },
              { label: 'Custom Rules' },
            ];
          }
        } else {
          this.parentGroupId = undefined;
          this.parentGroupName = undefined;
          this.breadcrumbs = [{ label: 'Custom Rules' }];
        }

        this.loadRules(this.searchControl.value || '');
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
    this.updateUrlAndLoadRules(this.searchControl.value || '');
  }

  onSearchKeyup(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.onSearch();
    }
  }

  handlePageEvent(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.updateUrlAndLoadRules(this.searchControl.value || '');
  }

  openAddDialog(): void {
    const dialogRef = this.dialog.open(AddCustomRuleDialogComponent, {
      width: '600px',
      data: { parentGroupId: this.parentGroupId },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadRules(this.searchControl.value || '');
      }
    });
  }

  editRule(rule: CustomRule): void {
    if (this.parentGroupId === undefined) {
      this.error = 'Parent group ID is not set.';
      return;
    }
    rule.parentGroupId = this.parentGroupId;
    const dialogRef = this.dialog.open(EditCustomRuleDialogComponent, {
      width: '600px',
      data: rule, // Pass the entire rule object to the dialog
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        // If dialog returns true (update was successful), refresh the rule list
        this.loadRules(this.searchControl.value || '');
      }
    });
  }

  private updateUrlAndLoadRules(search: string): void {
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

  private loadRules(search: string = ''): void {
    this.isLoading = true;
    this.error = '';

    if (this.parentGroupId === undefined) {
      this.error = 'Parent group ID is not set.';
      return;
    }

    this.customRuleService
      .getRules(this.pageIndex, this.pageSize, this.parentGroupId, search)
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
          this.error = this.errorHandler.getErrorMessage(
            err,
            'Error loading custom rules'
          );
        },
      });
  }

  deleteRule(id: number): void {
    if (confirm('Are you sure you want to delete this rule?')) {
      this.customRuleService.deleteRule(id).subscribe({
        next: () => {
          this.loadRules(this.searchControl.value || '');
        },
        error: (err) => {
          this.error = this.errorHandler.getErrorMessage(
            err,
            'Error deleting rule'
          );
        },
      });
    }
  }
}
