import { HttpErrorResponse } from '@angular/common/http';

/** Reads API `message` when the client used `responseType: 'blob'` (errors arrive as JSON blobs). */
export async function messageFromApiHttpError(err: HttpErrorResponse): Promise<string | undefined> {
  const body = err.error;
  if (body instanceof Blob) {
    try {
      const text = await body.text();
      const parsed = JSON.parse(text) as { message?: string };
      const m = parsed.message?.trim();
      return m || undefined;
    } catch {
      return undefined;
    }
  }
  if (body && typeof body === 'object' && 'message' in (body as object)) {
    const m = (body as { message?: unknown }).message;
    return typeof m === 'string' && m.trim() ? m.trim() : undefined;
  }
  return undefined;
}
