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
  // Discord
  discordToken: process.env.DISCORD_TOKEN || '',
  clientId: process.env.DISCORD_CLIENT_ID || '',
  guildId: process.env.DISCORD_GUILD_ID || '',

  // Backend
  msCommunityBase: base,
  api: `${base}/api`,

  // App frontend (for embed links)
  appBaseUrl: (process.env.APP_BASE_URL || 'http://localhost:4200').replace(/\/$/, ''),

  // User
  defaultUserId: Number(process.env.DEFAULT_USER_ID || '1'),

  // Theming
  embedAccent: parseEmbedColor(),

  // Automation
  feedPollIntervalMs: Number(process.env.FEED_POLL_INTERVAL_MS || String(5 * 60 * 1000)),
  digestTimezone: process.env.DIGEST_TIMEZONE || 'Europe/Paris',

  // Advanced Features
  openaiApiKey: process.env.OPENAI_API_KEY || '',
  webhookPort: Number(process.env.WEBHOOK_PORT || 3000),
  webhookSecret: process.env.WEBHOOK_SECRET || '',
};

export function requireConfig() {
  if (!config.discordToken) {
    throw new Error('Set DISCORD_TOKEN in .env');
  }
  if (!Number.isFinite(config.defaultUserId)) {
    throw new Error('Set DEFAULT_USER_ID to a numeric ms-community user id');
  }
}
