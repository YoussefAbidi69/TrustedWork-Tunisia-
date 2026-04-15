import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TrustPassportComponent } from './trust-passport.component';

describe('TrustPassportComponent', () => {
  let component: TrustPassportComponent;
  let fixture: ComponentFixture<TrustPassportComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TrustPassportComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TrustPassportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
