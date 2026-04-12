import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BadgesXpComponent } from './badges-xp.component';

describe('BadgesXpComponent', () => {
  let component: BadgesXpComponent;
  let fixture: ComponentFixture<BadgesXpComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [BadgesXpComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BadgesXpComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
