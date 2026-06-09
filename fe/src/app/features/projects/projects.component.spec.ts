import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ProjectsComponent } from './projects.component';
import { ProjectService } from '../../core/services/project.service';

describe('ProjectsComponent', () => {
  let component: ProjectsComponent;
  let fixture: ComponentFixture<ProjectsComponent>;
  let projectService: jasmine.SpyObj<ProjectService> & { activeReports: Map<string, unknown> };

  beforeEach(async () => {
    projectService = jasmine.createSpyObj<ProjectService>('ProjectService', [
      'getProjects',
      'getReportStatus',
    ]) as jasmine.SpyObj<ProjectService> & { activeReports: Map<string, unknown> };
    projectService.activeReports = new Map();
    projectService.getProjects.and.returnValue(
      of({ items: [], currentPage: 1, pageSize: 10, totalPages: 0, totalItems: 0 })
    );

    await TestBed.configureTestingModule({
      imports: [ProjectsComponent],
      providers: [
        provideRouter([]),
        { provide: ProjectService, useValue: projectService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.error).toBe('Subgroup ID is not set.');
  });
});
