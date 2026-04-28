import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AttachmentPreviewStripComponent } from './attachment-preview-strip.component';

describe('AttachmentPreviewStripComponent', () => {
  let component: AttachmentPreviewStripComponent;
  let fixture: ComponentFixture<AttachmentPreviewStripComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AttachmentPreviewStripComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AttachmentPreviewStripComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
