import 'dotenv/config';

const base = (process.env.MS_COMMUNITY_URL || 'http://localhost:8084').replace(/\/$/, '');

function parseEmbedColor() {
  const raw = process.env.DISCORD_EMBED_COLOR;
  if (!raw) return 0x6366f1;
  const hex = raw.replace(/^#/, '').trim();
  const n = parseInt(hex, 16);
  return Number.isFinite(n) ? n : 0x6366f1;
}

export const config = {
  discordToken: process.env.DISCORD_TOKEN || '',
  clientId: process.env.DISCORD_CLIENT_ID || '',
  guildId: process.env.DISCORD_GUILD_ID || '',
  msCommunityBase: base,
  api: `${base}/api`,
  defaultUserId: Number(process.env.DEFAULT_USER_ID || '1'),
  /** Left stripe color for embeds (hex in .env, e.g. 6366f1 or #7c3aed) */
  embedAccent: parseEmbedColor()
};

export function requireConfig() {
  if (!config.discordToken) {
    throw new Error('Set DISCORD_TOKEN in .env');
  }
  if (!Number.isFinite(config.defaultUserId)) {
    throw new Error('Set DEFAULT_USER_ID to a numeric ms-community user id');
  }
}
