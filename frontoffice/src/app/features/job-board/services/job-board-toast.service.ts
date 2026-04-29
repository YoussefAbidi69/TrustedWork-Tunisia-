import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

/** Lightweight toast bus scoped to the job-board shell (no third-party UI). */
@Injectable({ providedIn: 'root' })
export class JobBoardToastService {
  private readonly messageSubject = new Subject<string>();
  readonly messages$ = this.messageSubject.asObservable();

  show(message: string): void {
    this.messageSubject.next(message);
  }
}
