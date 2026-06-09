import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SubgroupsComponent } from './subgroups.component';

describe('SubgroupsComponent', () => {
  let component: SubgroupsComponent;
  let fixture: ComponentFixture<SubgroupsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubgroupsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SubgroupsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
