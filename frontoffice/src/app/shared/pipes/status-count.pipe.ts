import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'statusCount' })
export class StatusCountPipe implements PipeTransform {
  transform(items: any[], status: string): number {
    if (!items) return 0;
    return items.filter(i => i.status === status).length;
  }
}