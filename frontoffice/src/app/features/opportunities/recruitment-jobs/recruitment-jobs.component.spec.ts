import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RecruitmentJobsComponent } from './recruitment-jobs.component';

describe('RecruitmentJobsComponent', () => {
  let component: RecruitmentJobsComponent;
  let fixture: ComponentFixture<RecruitmentJobsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [RecruitmentJobsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RecruitmentJobsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
