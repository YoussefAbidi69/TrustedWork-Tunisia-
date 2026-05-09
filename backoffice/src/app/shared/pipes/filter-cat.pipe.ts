import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'filterCat' })
export class FilterCatPipe implements PipeTransform {
  transform(items: any[], cat: string): number {
    if (!items) return 0;
    return items.filter(i => i?.categorie === cat).length;
  }
}