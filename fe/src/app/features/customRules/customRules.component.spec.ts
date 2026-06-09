import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { CustomRulesComponent } from './customRules.component';
import { CustomRuleService } from '../../core/services/customRule.service';

describe('CustomRulesComponent', () => {
  let component: CustomRulesComponent;
  let fixture: ComponentFixture<CustomRulesComponent>;
  let customRuleService: jasmine.SpyObj<CustomRuleService>;

  beforeEach(async () => {
    customRuleService = jasmine.createSpyObj<CustomRuleService>('CustomRuleService', [
      'getRules',
      'deleteRule',
    ]);
    customRuleService.getRules.and.returnValue(
      of({ items: [], currentPage: 1, pageSize: 10, totalPages: 0, totalItems: 0 })
    );

    await TestBed.configureTestingModule({
      imports: [CustomRulesComponent],
      providers: [
        provideRouter([]),
        { provide: CustomRuleService, useValue: customRuleService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomRulesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.error).toBe('Parent group ID is not set.');
  });
});
