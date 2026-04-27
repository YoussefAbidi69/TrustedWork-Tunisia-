import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'timeAgo', standalone: true })
export class TimeAgoPipe implements PipeTransform {
  transform(iso: string | null | undefined): string {
    if (!iso) {
      return '—';
    }
    const t = new Date(iso).getTime();
    if (Number.isNaN(t)) {
      return '—';
    }
    const sec = Math.max(0, Math.floor((Date.now() - t) / 1000));
    if (sec < 60) {
      return 'just now';
    }
    const min = Math.floor(sec / 60);
    if (min < 60) {
      return `${min} min ago`;
    }
    const h = Math.floor(min / 60);
    if (h < 48) {
      return `${h} hour${h === 1 ? '' : 's'} ago`;
    }
    const d = Math.floor(h / 24);
    return `${d} day${d === 1 ? '' : 's'} ago`;
  }
}
