import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { BranchesComponent } from './branches.component';
import { BranchService } from '../../core/services/branch.service';

describe('BranchesComponent', () => {
  let component: BranchesComponent;
  let fixture: ComponentFixture<BranchesComponent>;
  let branchService: jasmine.SpyObj<BranchService>;

  beforeEach(async () => {
    branchService = jasmine.createSpyObj<BranchService>('BranchService', [
      'getBranches',
      'checkBranchMergeStatus',
      'generateBranchSummary',
    ]);
    branchService.getBranches.and.returnValue(
      of({ items: [], currentPage: 1, pageSize: 10, totalPages: 0, totalItems: 0 })
    );

    await TestBed.configureTestingModule({
      imports: [BranchesComponent],
      providers: [
        provideRouter([]),
        { provide: BranchService, useValue: branchService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BranchesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.error).toBe('Project ID is not set.');
  });
});
