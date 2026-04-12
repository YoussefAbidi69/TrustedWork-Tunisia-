import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FreelanceJobsComponent } from './freelance-jobs.component';

describe('FreelanceJobsComponent', () => {
  let component: FreelanceJobsComponent;
  let fixture: ComponentFixture<FreelanceJobsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [FreelanceJobsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FreelanceJobsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
