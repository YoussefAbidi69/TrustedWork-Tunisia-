import { config } from './config.js';

function stripInvisible(s) {
  return s.replace(/[\u200b-\u200d\ufeff]/g, '');
}

/** Last path segment, handling / and \\ */
export function basenameOnly(raw) {
  const t = stripInvisible(String(raw ?? '').trim());
  if (!t) return '';
  const i = Math.max(t.lastIndexOf('/'), t.lastIndexOf('\\'));
  return i >= 0 ? t.slice(i + 1) : t;
}

/**
 * Turn API fileUrl values into a browser-openable http(s) URL for ms-community course files.
 */
export function resolveCourseFileUrl(raw) {
  let t = stripInvisible(String(raw ?? '').trim());
  if (!t) return '';

  // Values mistakenly stored as "http://file2.pdf" (host is the filename)
  const bogusHostOnlyPdf = /^https?:\/\/([^/?#]+\.pdf)\/?$/i;
  const bogus = t.match(bogusHostOnlyPdf);
  if (bogus) {
    t = bogus[1];
  } else if (/^https?:\/\//i.test(t)) {
    return t;
  }

  const base = config.msCommunityBase.replace(/\/$/, '');

  if (t.startsWith('/')) {
    return `${base}${t}`;
  }

  // "api/course-files/x.pdf" without leading slash
  if (/^api\/course-files\//i.test(t)) {
    return `${base}/${t}`;
  }

  // Any path or relative dir: keep only safe basename
  const name = basenameOnly(t);
  if (!name) {
    return `${base}/api/course-files/${encodeURIComponent(t)}`;
  }

  if (/\.pdf$/i.test(name) && /^[a-zA-Z0-9._-]+$/.test(name)) {
    return `${base}/api/course-files/${encodeURIComponent(name)}`;
  }

  if (/^[a-zA-Z0-9._-]+$/.test(name)) {
    return `${base}/api/course-files/${encodeURIComponent(name)}`;
  }

  return `${base}/api/course-files/${encodeURIComponent(name)}`;
}

/** True if Discord will accept this string as a Link button / markdown URL */
export function isHttpUrl(s) {
  if (!s || typeof s !== 'string') return false;
  try {
    const u = new URL(s);
    return u.protocol === 'http:' || u.protocol === 'https:';
  } catch {
    return false;
  }
}
