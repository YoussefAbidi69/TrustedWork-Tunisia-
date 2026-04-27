import { animate, style, transition, trigger } from '@angular/animations';

export const accordionExpand = trigger('accordionExpand', [
  transition(':enter', [
    style({ height: 0, opacity: 0, overflow: 'hidden' }),
    animate('250ms ease-out', style({ height: '*', opacity: 1 }))
  ]),
  transition(':leave', [animate('200ms ease-in', style({ height: 0, opacity: 0 }))])
]);
