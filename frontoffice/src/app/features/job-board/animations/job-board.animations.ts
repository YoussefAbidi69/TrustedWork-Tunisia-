import { animate, query, stagger, style, transition, trigger } from '@angular/animations';

export const slideUpStagger = trigger('slideUpStagger', [
  transition('* => *', [
    query(
      ':enter',
      [
        style({ opacity: 0, transform: 'translateY(24px)' }),
        stagger(100, [animate('300ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))])
      ],
      { optional: true }
    )
  ])
]);

export const slideInRight = trigger('slideInRight', [
  transition(':enter', [
    style({ opacity: 0, transform: 'translateX(-24px)' }),
    animate('350ms ease-out', style({ opacity: 1, transform: 'translateX(0)' }))
  ])
]);

export const fadeIn = trigger('fadeIn', [
  transition(':enter', [
    style({ opacity: 0 }),
    animate('250ms ease-in', style({ opacity: 1 }))
  ])
]);

export const accordionExpand = trigger('accordionExpand', [
  transition(':enter', [
    style({ height: 0, opacity: 0, overflow: 'hidden' }),
    animate('250ms ease-out', style({ height: '*', opacity: 1 }))
  ]),
  transition(':leave', [animate('200ms ease-in', style({ height: 0, opacity: 0 }))])
]);

export const popIn = trigger('popIn', [
  transition(':enter', [
    style({ transform: 'scale(0)', opacity: 0 }),
    animate(
      '200ms cubic-bezier(0.34, 1.56, 0.64, 1)',
      style({ transform: 'scale(1)', opacity: 1 })
    )
  ])
]);

export const slideDownForm = trigger('slideDownForm', [
  transition(':enter', [
    style({ height: 0, opacity: 0, overflow: 'hidden' }),
    animate('300ms ease-out', style({ height: '*', opacity: 1 }))
  ]),
  transition(':leave', [animate('200ms ease-in', style({ height: 0, opacity: 0 }))])
]);
