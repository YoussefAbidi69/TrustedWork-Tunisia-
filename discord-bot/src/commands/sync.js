import { PermissionFlagsBits } from 'discord.js';
import * as api from '../services/community.service.js';
import { alertCard } from '../embeds/common.js';
import { buildServerPostCard } from '../embeds/post-card.js';
import { buildServerCourseCard } from '../embeds/course-card.js';
import { C } from '../utils/colors.js';
import { config } from '../config/index.js';
import { getGuildConfig, markPostAsPosted, markCourseAsPosted, getPostedIds } from '../config/guild-store.js';

export const name = 'sync';

export async function execute(interaction) {
  if (!interaction.memberPermissions?.has(PermissionFlagsBits.Administrator)) {
    return interaction.reply({
      embeds: [alertCard(interaction, C.danger, 'Permission denied', 'You need **Administrator** permission.')],
      ephemeral: true,
    });
  }

  await interaction.deferReply();
  const guild = interaction.guild;
  if (!guild) {
    return interaction.editReply({
      embeds: [alertCard(interaction, C.danger, 'Error', 'This command can only be used in a server.')],
    });
  }

  const guildConfig = getGuildConfig(guild.id);
  if (!guildConfig) {
    return interaction.editReply({
      embeds: [
        alertCard(
          interaction,
          C.warn,
          'Not configured',
          'Run `/setup_server` first to create channels.'
        ),
      ],
    });
  }

  let postCount = 0;
  let courseCount = 0;

  try {
    // Fetch all published posts
    const posts = await api.listPosts({ status: 'PUBLISHED', voterId: config.defaultUserId });
    const posted = getPostedIds();

    for (const post of posts || []) {
      if (posted.posts.includes(post.id)) continue;

      const { embed, row } = buildServerPostCard(interaction.client, post);
      const communityKey = String(post.communityId);
      const catConfig = guildConfig.categories?.[communityKey];

      // Post to community channel
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
      postCount++;
    }

    // Fetch all published courses
    const courses = await api.listCourses({ publishedOnly: true });
    for (const course of courses || []) {
      if (posted.courses.includes(course.id)) continue;

      const { embed, row } = buildServerCourseCard(interaction.client, course);
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
      courseCount++;
    }

    await interaction.editReply({
      embeds: [
        alertCard(
          interaction,
          C.success,
          '🔄 Sync Complete',
          `Synced **${postCount}** new posts and **${courseCount}** new courses to channels.`
        ),
      ],
    });
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    await interaction.editReply({
      embeds: [alertCard(interaction, C.danger, 'Sync failed', msg)],
    });
  }
}
