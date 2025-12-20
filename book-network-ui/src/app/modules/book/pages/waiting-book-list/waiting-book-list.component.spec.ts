import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WaitingBookListComponent } from './waiting-book-list.component';

describe('WaitingBookListComponent', () => {
  let component: WaitingBookListComponent;
  let fixture: ComponentFixture<WaitingBookListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [WaitingBookListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(WaitingBookListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
