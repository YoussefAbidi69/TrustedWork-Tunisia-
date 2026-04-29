import cron from 'node-cron';
import * as api from '../services/community.service.js';
import { buildDailyDigest, buildWeeklyDigest } from '../embeds/digest.js';
import { config } from '../config/index.js';
import { getGuildConfig } from '../config/guild-store.js';

/**
 * Start digest schedulers.
 * - Daily at 9:00 AM: top 5 posts by upvotes
 * - Weekly on Monday at 9:00 AM: top posts + new courses
 * @param {import('discord.js').Client} client
 */
export function startDigests(client) {
  // Daily digest at 9:00 AM
  cron.schedule('0 9 * * *', async () => {
    try {
      await postDailyDigest(client);
    } catch (err) {
      console.error('[digest:daily]', err.message || err);
    }
  });

  // Weekly digest every Monday at 9:00 AM
  cron.schedule('0 9 * * 1', async () => {
    try {
      await postWeeklyDigest(client);
    } catch (err) {
      console.error('[digest:weekly]', err.message || err);
    }
  });

  console.log(`   📰 Digests scheduled: daily 9:00 AM + weekly Monday 9:00 AM`);
}

async function postDailyDigest(client) {
  const posts = await api.listPosts({ status: 'PUBLISHED', voterId: config.defaultUserId });
  if (!posts?.length) return;

  // Sort by upvotes descending, take top 5
  const sorted = [...posts].sort((a, b) => (b.upvoteCount ?? 0) - (a.upvoteCount ?? 0));
  const top5 = sorted.slice(0, 5);

  const embed = buildDailyDigest(client, top5);

  for (const [guildId, guild] of client.guilds.cache) {
    const guildConfig = getGuildConfig(guildId);
    if (!guildConfig?.feedChannelId) continue;

    try {
      const feedCh = await guild.channels.fetch(guildConfig.feedChannelId);
      if (feedCh) await feedCh.send({ embeds: [embed] });
    } catch (err) {
      console.error(`[digest:daily:${guildId}]`, err.message || err);
    }
  }
}

async function postWeeklyDigest(client) {
  const [posts, courses] = await Promise.all([
    api.listPosts({ status: 'PUBLISHED', voterId: config.defaultUserId }).catch(() => []),
    api.listCourses({ publishedOnly: true }).catch(() => []),
  ]);

  // Top posts by upvotes
  const sortedPosts = [...(posts || [])].sort((a, b) => (b.upvoteCount ?? 0) - (a.upvoteCount ?? 0));
  const topPosts = sortedPosts.slice(0, 5);

  // Newest courses (by highest ID = most recent)
  const sortedCourses = [...(courses || [])].sort((a, b) => (b.id ?? 0) - (a.id ?? 0));
  const newCourses = sortedCourses.slice(0, 5);

  const embed = buildWeeklyDigest(client, topPosts, newCourses);

  for (const [guildId, guild] of client.guilds.cache) {
    const guildConfig = getGuildConfig(guildId);
    if (!guildConfig?.feedChannelId) continue;

    try {
      const feedCh = await guild.channels.fetch(guildConfig.feedChannelId);
      if (feedCh) await feedCh.send({ embeds: [embed] });
    } catch (err) {
      console.error(`[digest:weekly:${guildId}]`, err.message || err);
    }
  }
}
