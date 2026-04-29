import { config } from '../config/index.js';

// ─── Helpers ─────────────────────────────────────────────────────────────────

/** Build full API URL */
function url(path) {
  return `${config.api}${path.startsWith('/') ? path : `/${path}`}`;
}

/** Generic JSON fetcher with error handling */
export async function fetchJson(path, options = {}) {
  const res = await fetch(url(path), {
    ...options,
    headers: {
      Accept: 'application/json',
      ...options.headers,
    },
  });
  const text = await res.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = { raw: text };
  }
  if (!res.ok) {
    const msg = data?.message || data?.error || res.statusText;
    throw new Error(`${res.status}: ${msg}`);
  }
  return data;
}

// ─── Communities ──────────────────────────────────────────────────────────────

export async function listCommunities() {
  return fetchJson('/communities');
}

export async function getCommunity(id) {
  return fetchJson(`/communities/${id}`);
}

// ─── Posts ────────────────────────────────────────────────────────────────────

/**
 * @param {{ communityId?: number, status?: string, voterId?: number }} q
 */
export async function listPosts(q = {}) {
  const p = new URLSearchParams();
  if (q.communityId != null) p.set('communityId', String(q.communityId));
  if (q.status) p.set('status', q.status);
  if (q.voterId != null) p.set('voterId', String(q.voterId));
  const qs = p.toString();
  return fetchJson(`/posts${qs ? `?${qs}` : ''}`);
}

/**
 * @param {number} id
 * @param {number} [voterId]
 */
export async function getPost(id, voterId) {
  const qs = voterId != null ? `?voterId=${voterId}` : '';
  return fetchJson(`/posts/${id}${qs}`);
}

/**
 * @param {object} payload PostRequest shape
 */
export async function createPost(payload) {
  return fetchJson('/posts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

/** @param {number} postId */
export async function publishPost(postId) {
  return fetchJson(`/posts/${postId}/publish`, { method: 'POST' });
}

// ─── Courses ─────────────────────────────────────────────────────────────────

/**
 * @param {{ communityId?: number, publishedOnly?: boolean }} q
 */
export async function listCourses(q = {}) {
  const p = new URLSearchParams();
  if (q.communityId != null) p.set('communityId', String(q.communityId));
  if (q.publishedOnly != null) p.set('publishedOnly', String(q.publishedOnly));
  const qs = p.toString();
  return fetchJson(`/courses${qs ? `?${qs}` : ''}`);
}

export async function getCourse(id) {
  return fetchJson(`/courses/${id}`);
}

/**
 * Get full course with sections and blocks for syllabus view.
 */
export async function downloadCourse(courseId) {
  return fetchJson(`/courses/${courseId}/download`);
}

// ─── Post Votes (FIXED: uses JSON body, not query params) ────────────────────

/**
 * @param {number} postId
 * @param {number} userId
 * @param {'UP'|'DOWN'} type
 */
export async function voteOnPost(postId, userId, type) {
  return fetchJson(`/votes/post/${postId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, type }),
  });
}

// ─── Course Votes ────────────────────────────────────────────────────────────

/**
 * @param {number} courseId
 * @param {number} userId
 * @param {'UP'|'DOWN'} type
 */
export async function voteOnCourse(courseId, userId, type) {
  return fetchJson(`/course-votes/course/${courseId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, type }),
  });
}

// ─── Post Comments ───────────────────────────────────────────────────────────

export async function listComments(postId) {
  return fetchJson(`/comments/post/${postId}`);
}

/**
 * @param {number} postId
 * @param {string} content
 * @param {number} userId
 */
export async function addComment(postId, content, userId) {
  return fetchJson(`/comments/post/${postId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content, userId }),
  });
}

// ─── Course Comments ─────────────────────────────────────────────────────────

export async function listCourseComments(courseId) {
  return fetchJson(`/course-comments/course/${courseId}`);
}

export async function addCourseComment(courseId, content, userId) {
  return fetchJson(`/course-comments/course/${courseId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content, userId }),
  });
}

// ─── Sections ────────────────────────────────────────────────────────────────

export async function listSections(courseId) {
  return fetchJson(`/sections/course/${courseId}`);
}

// ─── File Upload / Download ──────────────────────────────────────────────────

/**
 * @param {Buffer} pdfBuffer
 * @param {string} filename
 */
export async function uploadCoursePdf(pdfBuffer, filename) {
  const blob = new Blob([pdfBuffer], { type: 'application/pdf' });
  const form = new FormData();
  form.append('file', blob, filename.endsWith('.pdf') ? filename : `${filename}.pdf`);

  const res = await fetch(url('/course-files'), {
    method: 'POST',
    body: form,
  });
  const text = await res.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = { raw: text };
  }
  if (!res.ok) {
    const msg = data?.message || data?.error || res.statusText;
    throw new Error(`${res.status}: ${msg}`);
  }
  return data;
}

/**
 * @param {number} courseId
 * @param {number} userId
 * @returns {Promise<{ buffer: Buffer, contentType: string }>}
 */
export async function downloadCourseFile(courseId, userId) {
  const u = url(`/download/${courseId}?userId=${userId}`);
  const res = await fetch(u);
  if (!res.ok) {
    const text = await res.text();
    let msg = res.statusText;
    try {
      const j = JSON.parse(text);
      msg = j.message || j.error || msg;
    } catch {
      /* ignore */
    }
    throw new Error(`${res.status}: ${msg}`);
  }
  const arrayBuf = await res.arrayBuffer();
  const contentType = res.headers.get('content-type') || 'application/octet-stream';
  return { buffer: Buffer.from(arrayBuf), contentType };
}

// ─── Contributions ───────────────────────────────────────────────────────────

export async function getContribution(userId) {
  return fetchJson(`/contributions/users/${userId}`);
}
