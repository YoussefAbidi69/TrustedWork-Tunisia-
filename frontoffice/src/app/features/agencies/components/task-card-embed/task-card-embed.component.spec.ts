import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TaskCardEmbedComponent } from './task-card-embed.component';

describe('TaskCardEmbedComponent', () => {
  let component: TaskCardEmbedComponent;
  let fixture: ComponentFixture<TaskCardEmbedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskCardEmbedComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TaskCardEmbedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
