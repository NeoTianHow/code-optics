import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { GroupsComponent } from './groups.component';
import { GroupService } from '../../core/services/group.service';

describe('GroupsComponent', () => {
  let component: GroupsComponent;
  let fixture: ComponentFixture<GroupsComponent>;
  let groupService: jasmine.SpyObj<GroupService>;

  beforeEach(async () => {
    groupService = jasmine.createSpyObj<GroupService>('GroupService', ['getGroups']);
    groupService.getGroups.and.returnValue(
      of({ items: [], currentPage: 1, pageSize: 5, totalPages: 0, totalItems: 0 })
    );

    await TestBed.configureTestingModule({
      imports: [GroupsComponent],
      providers: [
        provideRouter([]),
        { provide: GroupService, useValue: groupService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GroupsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(groupService.getGroups).toHaveBeenCalledWith(0, 5, '');
  });
});
