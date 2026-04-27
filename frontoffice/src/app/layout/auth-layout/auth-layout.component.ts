import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subject, filter, takeUntil } from 'rxjs';
import {
  trigger,
  transition,
  style,
  animate,
  query,
  group
} from '@angular/animations';

@Component({
  selector: 'app-auth-layout',
  templateUrl: './auth-layout.component.html',
  styleUrls: ['./auth-layout.component.css'],
  animations: [
    trigger('routeAnimation', [
      transition('* <=> *', [
        query(
          ':enter, :leave',
          [
            style({
              position: 'absolute',
              width: '100%',
              left: 0,
              top: 0
            })
          ],
          { optional: true }
        ),

        group([
          query(
            ':leave',
            [
              animate(
                '260ms ease',
                style({
                  opacity: 0,
                  transform: 'translateY(20px) scale(0.98)'
                })
              )
            ],
            { optional: true }
          ),

          query(
            ':enter',
            [
              style({
                opacity: 0,
                transform: 'translateY(28px) scale(0.96)'
              }),
              animate(
                '520ms cubic-bezier(0.22, 1, 0.36, 1)',
                style({
                  opacity: 1,
                  transform: 'translateY(0) scale(1)'
                })
              )
            ],
            { optional: true }
          )
        ])
      ])
    ])
  ]
})
export class AuthLayoutComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  currentMode: 'login' | 'register' | 'forgot' | 'reset' = 'login';
  overlayActive = false;

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.updateMode(this.router.url);

    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event: any) => {
        this.updateMode(event.urlAfterRedirects || event.url);
      });
  }

  private updateMode(url: string): void {
  this.overlayActive = false;

  requestAnimationFrame(() => {
    this.overlayActive = true;

    setTimeout(() => {
      this.overlayActive = false;
    }, 800);
  });

  if (url.includes('register')) {
    this.currentMode = 'register';
  } else if (url.includes('forgot-password')) {
    this.currentMode = 'forgot';
  } else if (url.includes('reset-password')) {
    this.currentMode = 'reset';
  } else {
    this.currentMode = 'login';
  }
}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}