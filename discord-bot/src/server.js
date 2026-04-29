import express from 'express';
import { config } from './config/index.js';
import { getDb } from './services/db.service.js';
import { buildServerPostCard } from './embeds/post-card.js';
import { buildServerCourseCard } from './embeds/course-card.js';
import { getGuildConfig } from './config/guild-store.js';

export function startWebhookServer(client) {
  const app = express();
  app.use(express.json());

  // Simple auth middleware
  app.use((req, res, next) => {
    const auth = req.headers['authorization'];
    if (config.webhookSecret && auth !== `Bearer ${config.webhookSecret}`) {
      return res.status(401).json({ error: 'Unauthorized' });
    }
    next();
  });

  app.post('/webhook', async (req, res) => {
    const { event, payload } = req.body;
    
    if (!event || !payload) {
      return res.status(400).json({ error: 'Missing event or payload' });
    }

    try {
      if (event === 'post.published') {
        await handlePostPublished(client, payload);
      } else if (event === 'course.published') {
        await handleCoursePublished(client, payload);
      } else if (event === 'verify') {
        await handleVerify(payload);
      }
      
      res.status(200).json({ success: true });
    } catch (err) {
      console.error('[webhook]', err);
      res.status(500).json({ error: 'Internal Server Error' });
    }
  });

  const port = config.webhookPort;
  app.listen(port, () => {
    console.log(`   🌐 Webhook receiver listening on port ${port}`);
  });
}

async function handlePostPublished(client, post) {
  // Same logic as feed-sync.js, but triggered instantly
  for (const [guildId, guild] of client.guilds.cache) {
    const guildConfig = getGuildConfig(guildId);
    if (!guildConfig) continue;

    const { embed, row } = buildServerPostCard(client, post);
    const communityKey = String(post.communityId);
    const catConfig = guildConfig.categories?.[communityKey];

    // Post to community channel
    if (catConfig?.postsChannelId) {
      try {
        const ch = await guild.channels.fetch(catConfig.postsChannelId);
        if (ch) await ch.send({ embeds: [embed], components: [row] });
      } catch { /* ignore */ }
    }

    // Post to #feed
    if (guildConfig.feedChannelId) {
      try {
        const feedCh = await guild.channels.fetch(guildConfig.feedChannelId);
        if (feedCh) await feedCh.send({ embeds: [embed], components: [row] });
      } catch { /* ignore */ }
    }
  }
}

async function handleCoursePublished(client, course) {
  for (const [guildId, guild] of client.guilds.cache) {
    const guildConfig = getGuildConfig(guildId);
    if (!guildConfig) continue;

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
  }
}

async function handleVerify({ discordId, appUserId }) {
  const db = await getDb();
  await db.run('INSERT OR IGNORE INTO users (id) VALUES (?)', [discordId]);
  await db.run('UPDATE users SET app_user_id = ? WHERE id = ?', [appUserId, discordId]);
}
