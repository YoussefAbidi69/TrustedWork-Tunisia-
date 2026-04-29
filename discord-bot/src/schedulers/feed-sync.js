import cron from 'node-cron';
import * as api from '../services/community.service.js';
import { buildServerPostCard } from '../embeds/post-card.js';
import { buildServerCourseCard } from '../embeds/course-card.js';
import { config } from '../config/index.js';
import { getGuildConfig, getPostedIds, markPostAsPosted, markCourseAsPosted } from '../config/guild-store.js';

/**
 * Start the feed sync polling scheduler.
 * Polls every 5 minutes (configurable via FEED_POLL_INTERVAL_MS).
 * @param {import('discord.js').Client} client
 */
export function startFeedSync(client) {
  const intervalMs = config.feedPollIntervalMs;
  const intervalMinutes = Math.max(1, Math.round(intervalMs / 60000));

  // Run every N minutes using cron
  const cronExpr = `*/${intervalMinutes} * * * *`;

  cron.schedule(cronExpr, async () => {
    try {
      await syncFeed(client);
    } catch (err) {
      console.error('[feed-sync]', err.message || err);
    }
  });

  console.log(`   📡 Feed sync scheduled: every ${intervalMinutes} min`);
}

async function syncFeed(client) {
  for (const [guildId, guild] of client.guilds.cache) {
    const guildConfig = getGuildConfig(guildId);
    if (!guildConfig) continue;

    const posted = getPostedIds();

    // ─── Sync Posts ────────────────────────────────────────────────────
    try {
      const posts = await api.listPosts({ status: 'PUBLISHED', voterId: config.defaultUserId });
      for (const post of posts || []) {
        if (posted.posts.includes(post.id)) continue;

        const { embed, row } = buildServerPostCard(client, post);
        const communityKey = String(post.communityId);
        const catConfig = guildConfig.categories?.[communityKey];

        // Post to community posts channel
        if (catConfig?.postsChannelId) {
          try {
            const ch = await guild.channels.fetch(catConfig.postsChannelId);
            if (ch) await ch.send({ embeds: [embed], components: [row] });
          } catch { /* channel may have been deleted */ }
        }

        // Post to #feed
        if (guildConfig.feedChannelId) {
          try {
            const feedCh = await guild.channels.fetch(guildConfig.feedChannelId);
            if (feedCh) await feedCh.send({ embeds: [embed], components: [row] });
          } catch { /* ignore */ }
        }

        markPostAsPosted(post.id);
      }
    } catch (err) {
      console.error('[feed-sync:posts]', err.message || err);
    }

    // ─── Sync Courses ──────────────────────────────────────────────────
    try {
      const courses = await api.listCourses({ publishedOnly: true });
      for (const course of courses || []) {
        if (posted.courses.includes(course.id)) continue;

        const { embed, row } = buildServerCourseCard(client, course);
        const communityKey = String(course.communityId);
        const catConfig = guildConfig.categories?.[communityKey];

        if (catConfig?.coursesChannelId) {
          try {
            const ch = await guild.channels.fetch(catConfig.coursesChannelId);
            if (ch) await ch.send({ embeds: [embed], components: [row] });
          } catch { /* ignore */ }
        }

        if (guildConfig.feedChannelId) {
          try {
            const feedCh = await guild.channels.fetch(guildConfig.feedChannelId);
            if (feedCh) await feedCh.send({ embeds: [embed], components: [row] });
          } catch { /* ignore */ }
        }

        markCourseAsPosted(course.id);
      }
    } catch (err) {
      console.error('[feed-sync:courses]', err.message || err);
    }
  }
}
