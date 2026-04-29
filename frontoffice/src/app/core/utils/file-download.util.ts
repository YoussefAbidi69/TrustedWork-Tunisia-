/** Triggers a browser download for a Blob (e.g. course file from ms-community). */
export function triggerBlobDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

/** Prefer .pdf from blob type or original post file URL (server often uses octet-stream). */
export function courseDownloadFilename(postId: number, blob: Blob, fileUrl?: string | null): string {
  const url = (fileUrl || '').toLowerCase();
  if (url.includes('.pdf') || (blob.type || '').includes('pdf')) {
    return `course-${postId}.pdf`;
  }
  return `course-${postId}.bin`;
}
