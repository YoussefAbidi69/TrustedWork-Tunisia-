import { config } from './config.js';

/** @param {string} path */
function url(path) {
  return `${config.api}${path.startsWith('/') ? path : `/${path}`}`;
}

export async function fetchJson(path, options = {}) {
  const res = await fetch(url(path), {
    ...options,
    headers: {
      Accept: 'application/json',
      ...options.headers
    }
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

export async function listCommunities() {
  return fetchJson('/communities');
}

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
 * @param {Buffer} pdfBuffer
 * @param {string} filename
 */
export async function uploadCoursePdf(pdfBuffer, filename) {
  const blob = new Blob([pdfBuffer], { type: 'application/pdf' });
  const form = new FormData();
  form.append('file', blob, filename.endsWith('.pdf') ? filename : `${filename}.pdf`);

  const res = await fetch(url('/course-files'), {
    method: 'POST',
    body: form
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
 * @param {object} payload PostDTO shape
 */
export async function createPost(payload) {
  return fetchJson('/posts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
}

/** @param {number} postId */
export async function publishPost(postId) {
  return fetchJson(`/posts/${postId}/publish`, { method: 'POST' });
}

/**
 * @param {number} postId
 * @param {number} userId
 * @returns {Promise<{ buffer: Buffer, contentType: string }>}
 */
export async function downloadCourseFile(postId, userId) {
  const u = url(`/download/${postId}?userId=${userId}`);
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

/**
 * @param {number} postId
 * @param {number} userId
 * @param {'UP'|'DOWN'} type
 */
export async function vote(postId, userId, type) {
  const p = new URLSearchParams({ postId: String(postId), type, userId: String(userId) });
  return fetchJson(`/votes?${p}`, { method: 'POST' });
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
    body: JSON.stringify({ content, userId })
  });
}
