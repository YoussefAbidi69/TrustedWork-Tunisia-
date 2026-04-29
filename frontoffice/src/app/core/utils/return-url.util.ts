/**
 * Prevents open redirects: only same-app community/post URLs may be used after login.
 */
export function isSafeCommunityReturnUrl(url: string): boolean {
  if (!url || !url.startsWith('/') || url.startsWith('//')) {
    return false;
  }
  const prefixes = ['/posts', '/communities', '/community'];
  return prefixes.some((p) => url === p || url.startsWith(p + '/') || url.startsWith(p + '?'));
}
