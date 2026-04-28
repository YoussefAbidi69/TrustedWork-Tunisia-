import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PinnedMessagesPanelComponent } from './pinned-messages-panel.component';

describe('PinnedMessagesPanelComponent', () => {
  let component: PinnedMessagesPanelComponent;
  let fixture: ComponentFixture<PinnedMessagesPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PinnedMessagesPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PinnedMessagesPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
