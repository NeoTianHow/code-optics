import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Branch } from '../../core/models/branch.model';
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
import { BranchService } from '../../core/services/branch.service';
import { MatIconModule } from '@angular/material/icon';
import { MarkdownPipe } from '../../core/pipes/markdown.pipe';
import {
  animate,
  state,
  style,
  transition,
  trigger,
} from '@angular/animations';
import {
  Breadcrumb,
  BreadcrumbComponent,
} from '../../core/components/breadcrumb';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatOption, MatSelect } from '@angular/material/select';

@Component({
  selector: 'app-branches',
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
    MarkdownPipe,
    BreadcrumbComponent,
    MatTooltipModule,
    MatSelect,
    MatOption,
  ],
  templateUrl: './branches.component.html',
  styleUrls: ['./branches.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({ height: '0px', minHeight: '0' })),
      state('expanded', style({ height: '*' })),
      transition(
        'expanded <=> collapsed',
        animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')
      ),
    ]),
  ],
})
export class BranchesComponent implements OnInit, OnDestroy {
  breadcrumbs: Breadcrumb[] = [];

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  dataSource = new MatTableDataSource<Branch>();
  pageSizeOptions = [5, 10, 15, 20];
  pageSize: number = this.pageSizeOptions[1];
  pageIndex: number = 0;
  totalItems: number = 0;

  selection = new SelectionModel<Branch>(true, []);
  searchControl = new FormControl('');
  expandedElements: Set<Branch> = new Set();
  isLoading = false;
  error = '';
  generatingBranchName: string | null = null;
  projectId?: number;
  groupId?: number;
  subgroupId?: number;
  defaultBranch?: string;

  displayedColumns: string[] = [
    'expand',
    'name',
    'compareAgainst',
    'mergeStatus',
    'activityStatus',
    'lastCommitDate',
    'actions',
  ];

  allBranches: string[] = [];

  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private errorHandler: ErrorHandlingService,
    private branchService: BranchService
  ) {}

  ngOnInit(): void {
    this.initRouteParams();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initRouteParams(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe({
      next: (params) => {
        // will be called when the component is first initialized and when the "route" params change
        const projectIdParam = params['projectId'];
        this.groupId = params['groupId'];
        this.subgroupId = params['subgroupId'];

        if (projectIdParam && !isNaN(+projectIdParam)) {
          this.projectId = +projectIdParam;
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

        const groupName = params['groupName'];
        const subgroupName = params['subgroupName'];
        const projectName = params['projectName'];
        this.defaultBranch = params['defaultBranch'];

        if (groupName && subgroupName && projectName) {
          this.breadcrumbs = [
            {
              label: 'Groups',
              url: '/groups',
              queryParams: { groupName },
            },
            {
              label: groupName,
              url: `/groups/${this.groupId}/subgroups`,
              queryParams: { groupName },
            },
            {
              label: subgroupName,
              url: `/groups/${this.groupId}/subgroups/${this.subgroupId}/projects`,
              queryParams: { groupName, subgroupName },
            },
            { label: projectName },
          ];
        }

        this.loadBranches(this.searchControl.value || '');
      });
  }

  onSearch(): void {
    this.pageIndex = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    this.updateUrlAndLoadBranches(this.searchControl.value || '');
  }

  onSearchKeyup(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.onSearch();
    }
  }

  handlePageEvent(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.updateUrlAndLoadBranches(this.searchControl.value || '');
  }

  private updateUrlAndLoadBranches(search: string): void {
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
        projectName: this.route.snapshot.queryParams['projectName'],
        defaultBranch: this.route.snapshot.queryParams['defaultBranch'],
      },
    });
  }

  private loadBranches(search: string = ''): void {
    if (!this.projectId) {
      this.error = 'Project ID is not set.';
      return;
    }

    this.isLoading = true;
    this.error = '';

    if (this.defaultBranch === undefined) {
      this.error = 'Default branch is not set.';
      return;
    }

    this.branchService
      .getBranches(
        this.projectId,
        this.pageIndex,
        this.pageSize,
        this.defaultBranch,
        search
      )
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.isLoading = false;
        })
      )
      .subscribe({
        next: (response) => {
          if (response.items) {
            // Store all branch names
            this.allBranches = response.items.map((branch) => branch.name);

            // Set default comparison branch to be used for dropdown box
            response.items.forEach((branch) => {
              if (!branch.comparingAgainst && this.defaultBranch) {
                branch.comparingAgainst = this.defaultBranch;
              }
            });

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
          console.error('Error loading branches:', err);
          this.error = this.errorHandler.getErrorMessage(
            err,
            'Error loading branches'
          );
        },
      });
  }

  onCompareChange(branch: Branch, compareBranch: string): void {
    branch.compareLoading = true;
    branch.comparingAgainst = compareBranch;

    this.branchService
      .checkBranchMergeStatus(this.projectId!, branch.name, compareBranch)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          branch.compareLoading = false;
        })
      )
      .subscribe({
        next: (response) => {
          branch.merged = response.isMerged;
          branch.needsUpdate = response.needsUpdate;

          // Update the branch summaries based on the response
          branch.detailedSummary = response.detailedSummary || '';

          // If a summary exists and we're displaying it, expand the row
          if (response.detailedSummary && !branch.needsUpdate) {
            this.expandedElements.add(branch);
          } else {
            // If there's no summary or it needs update, collapse the row
            this.expandedElements.delete(branch);
          }
        },
        error: (err) => {
          console.error('Error checking merge status:', err);
          this.error = this.errorHandler.getErrorMessage(
            err,
            'Error checking merge status'
          );
        },
      });
  }

  getAvailableBranches(currentBranch: string): string[] {
    return this.allBranches.filter((branch) => branch !== currentBranch);
  }

  onGenerateSummary(branch: Branch): void {
    if (!this.projectId) {
      this.error = 'Project ID not set.';
      return;
    }

    const comparisonBranch = branch.comparingAgainst;
    if (!comparisonBranch) {
      this.error = 'Please select a branch to compare against.';
      return;
    }

    this.generatingBranchName = branch.name;

    this.branchService
      .generateBranchSummary(
        this.projectId,
        branch.commit.id,
        comparisonBranch, // from branch (what we're comparing against)
        branch.name // to branch (current branch)
      )
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.generatingBranchName = null;
        })
      )
      .subscribe({
        next: (summaryText) => {
          branch.detailedSummary = summaryText;
          branch.needsUpdate = false;
          this.expandedElements.add(branch);
        },
        error: (err) => {
          this.error = this.errorHandler.getErrorMessage(
            err,
            'Error generating summary'
          );
        },
      });
  }

  toggleRow(element: Branch): void {
    if (this.expandedElements.has(element)) {
      this.expandedElements.delete(element);
    } else {
      this.expandedElements.add(element);
    }
  }

  isRowExpanded(element: Branch): boolean {
    return this.expandedElements.has(element);
  }

  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }
}
