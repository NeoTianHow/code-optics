import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { SubgroupsComponent } from './subgroups.component';
import { SubgroupService } from '../../core/services/subgroup.service';

describe('SubgroupsComponent', () => {
  let component: SubgroupsComponent;
  let fixture: ComponentFixture<SubgroupsComponent>;
  let subgroupService: jasmine.SpyObj<SubgroupService>;

  beforeEach(async () => {
    subgroupService = jasmine.createSpyObj<SubgroupService>('SubgroupService', ['getSubgroups']);
    subgroupService.getSubgroups.and.returnValue(
      of({ items: [], currentPage: 1, pageSize: 10, totalPages: 0, totalItems: 0 })
    );

    await TestBed.configureTestingModule({
      imports: [SubgroupsComponent],
      providers: [
        provideRouter([]),
        { provide: SubgroupService, useValue: subgroupService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SubgroupsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.error).toBe('Group ID is not set.');
  });
});
