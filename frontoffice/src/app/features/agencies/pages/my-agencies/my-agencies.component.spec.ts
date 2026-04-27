import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MyAgenciesComponent } from './my-agencies.component';

describe('MyAgenciesComponent', () => {
  let component: MyAgenciesComponent;
  let fixture: ComponentFixture<MyAgenciesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [MyAgenciesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MyAgenciesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
