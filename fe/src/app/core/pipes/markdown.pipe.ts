import { Pipe, PipeTransform } from '@angular/core';
import { marked } from 'marked';

@Pipe({
  name: 'markdown',
  standalone: true,
})
export class MarkdownPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) {
      return '';
    }
    // Force sync mode each time
    const result = marked.parse(value, { async: false });
    // Marked's type definition says (string | Promise<string>),
    // so we cast to string since we've enforced sync mode.
    return result as string;
  }
}
