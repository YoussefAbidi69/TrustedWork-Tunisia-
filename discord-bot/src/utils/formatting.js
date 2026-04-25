import { config } from '../config/index.js';

/**
 * Truncate a string to `max` characters, adding … if trimmed.
 */
export function truncate(s, max = 350) {
  if (!s) return '';
  return s.length <= max ? s : `${s.slice(0, max)}…`;
}

/**
 * Build an app URL for a post.
 */
export function postAppUrl(postId) {
  return `${config.appBaseUrl}/community/post/${postId}`;
}

/**
 * Build an app URL for a course.
 */
export function courseAppUrl(courseId) {
  return `${config.appBaseUrl}/community/course/${courseId}`;
}

/**
 * Format a vote stats line.
 */
export function statsLine(p) {
  const up = p.upvoteCount ?? 0;
  const down = p.downvoteCount ?? 0;
  return `▲ **${up}** · ▼ **${down}**`;
}

/**
 * Relative time string (simple approximation).
 */
export function relativeTime(dateStr) {
  if (!dateStr) return '';
  const now = Date.now();
  const then = new Date(dateStr).getTime();
  if (isNaN(then)) return '';
  const diffMs = now - then;
  const minutes = Math.floor(diffMs / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

/**
 * Strip invisible unicode characters.
 */
export function stripInvisible(s) {
  return s.replace(/[\u200b-\u200d\ufeff]/g, '');
}

/**
 * Last path segment, handling / and \\
 */
export function basenameOnly(raw) {
  const t = stripInvisible(String(raw ?? '').trim());
  if (!t) return '';
  const i = Math.max(t.lastIndexOf('/'), t.lastIndexOf('\\'));
  return i >= 0 ? t.slice(i + 1) : t;
}

/**
 * Resolve a fileUrl from the backend into a browser-openable URL.
 */
export function resolveCourseFileUrl(raw) {
  let t = stripInvisible(String(raw ?? '').trim());
  if (!t) return '';

  const bogusHostOnlyPdf = /^https?:\/\/([^/?#]+\.pdf)\/?$/i;
  const bogus = t.match(bogusHostOnlyPdf);
  if (bogus) {
    t = bogus[1];
  } else if (/^https?:\/\//i.test(t)) {
    return t;
  }

  const base = config.msCommunityBase.replace(/\/$/, '');

  if (t.startsWith('/')) return `${base}${t}`;
  if (/^api\/course-files\//i.test(t)) return `${base}/${t}`;

  const name = basenameOnly(t);
  if (!name) return `${base}/api/course-files/${encodeURIComponent(t)}`;
  return `${base}/api/course-files/${encodeURIComponent(name)}`;
}

/** True if string is a valid http(s) URL for Discord link buttons */
export function isHttpUrl(s) {
  if (!s || typeof s !== 'string') return false;
  try {
    const u = new URL(s);
    return u.protocol === 'http:' || u.protocol === 'https:';
  } catch {
    return false;
  }
}
