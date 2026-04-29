import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const DATA_DIR = join(__dirname, '..', '..', 'data');
const GUILD_FILE = join(DATA_DIR, 'guild-config.json');
const POSTED_FILE = join(DATA_DIR, 'posted-ids.json');

function ensureDataDir() {
  if (!existsSync(DATA_DIR)) {
    mkdirSync(DATA_DIR, { recursive: true });
  }
}

// ─── Guild Config ────────────────────────────────────────────────────────────

function readGuildConfig() {
  ensureDataDir();
  try {
    return JSON.parse(readFileSync(GUILD_FILE, 'utf-8'));
  } catch {
    return {};
  }
}

function writeGuildConfig(data) {
  ensureDataDir();
  writeFileSync(GUILD_FILE, JSON.stringify(data, null, 2), 'utf-8');
}

/**
 * Get stored config for a guild.
 * @param {string} guildId
 * @returns {{ feedChannelId?: string, welcomeChannelId?: string, categories?: Record<string, { categoryId: string, postsChannelId: string, coursesChannelId: string }> } | null}
 */
export function getGuildConfig(guildId) {
  const all = readGuildConfig();
  return all[guildId] ?? null;
}

/**
 * Save config for a guild.
 */
export function setGuildConfig(guildId, guildData) {
  const all = readGuildConfig();
  all[guildId] = guildData;
  writeGuildConfig(all);
}

// ─── Posted IDs (dedup for feed sync) ────────────────────────────────────────

function readPostedIds() {
  ensureDataDir();
  try {
    return JSON.parse(readFileSync(POSTED_FILE, 'utf-8'));
  } catch {
    return { posts: [], courses: [] };
  }
}

function writePostedIds(data) {
  ensureDataDir();
  writeFileSync(POSTED_FILE, JSON.stringify(data, null, 2), 'utf-8');
}

export function getPostedIds() {
  return readPostedIds();
}

export function markPostAsPosted(postId) {
  const data = readPostedIds();
  if (!data.posts.includes(postId)) {
    data.posts.push(postId);
    // Keep last 500 to avoid unbounded growth
    if (data.posts.length > 500) data.posts = data.posts.slice(-500);
    writePostedIds(data);
  }
}

export function markCourseAsPosted(courseId) {
  const data = readPostedIds();
  if (!data.courses.includes(courseId)) {
    data.courses.push(courseId);
    if (data.courses.length > 500) data.courses = data.courses.slice(-500);
    writePostedIds(data);
  }
}
