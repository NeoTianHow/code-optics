import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CustomRulesComponent } from './customRules.component';

describe('CustomRulesComponent', () => {
  let component: CustomRulesComponent;
  let fixture: ComponentFixture<CustomRulesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomRulesComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomRulesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
