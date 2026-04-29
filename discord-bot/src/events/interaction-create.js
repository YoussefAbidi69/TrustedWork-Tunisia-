import { InteractionType } from 'discord.js';
import { alertCard } from '../embeds/common.js';
import { C } from '../utils/colors.js';
import * as api from '../services/community.service.js';
import { config } from '../config/index.js';
import { truncate } from '../utils/formatting.js';
import { parsePaginationId } from '../utils/pagination.js';
import { addXp, XP_RULES } from '../services/xp.service.js';

// ─── Command registry ────────────────────────────────────────────────────────

const commands = new Map();

async function loadCommands() {
  const modules = [
    'communities', 'posts', 'post', 'courses', 'course',
    'vote', 'comment', 'upload-course', 'download-course',
    'search', 'trending', 'new-courses', 'setup-server', 'sync',
    'profile', 'leaderboard', 'ask', 'subscribe', 'analytics',
  ];

  for (const modName of modules) {
    const mod = await import(`../commands/${modName}.js`);
    commands.set(mod.name, mod);
    // Register aliases
    if (mod.aliases) {
      for (const alias of mod.aliases) {
        commands.set(alias, mod);
      }
    }
  }
}

// Load commands on import
const _loaded = loadCommands();

export const name = 'interactionCreate';

/**
 * @param {import('discord.js').Interaction} interaction
 */
export async function execute(interaction) {
  await _loaded; // ensure commands are loaded

  // ─── Slash Commands ──────────────────────────────────────────────────
  if (interaction.isChatInputCommand()) {
    const cmd = commands.get(interaction.commandName);
    if (!cmd) {
      return interaction.reply({
        embeds: [alertCard(interaction, C.muted, 'Unknown command', 'This command is not registered.')],
        ephemeral: true,
      });
    }

    try {
      await cmd.execute(interaction);
    } catch (err) {
      console.error(`[cmd:${interaction.commandName}]`, err);
      const msg = err instanceof Error ? err.message : String(err);
      const embed = alertCard(interaction, C.danger, 'Request failed', msg);
      if (interaction.deferred) {
        await interaction.editReply({ embeds: [embed] }).catch(() => {});
      } else if (interaction.replied) {
        await interaction.followUp({ embeds: [embed], ephemeral: true }).catch(() => {});
      } else {
        await interaction.reply({ embeds: [embed], ephemeral: true }).catch(() => {});
      }
    }
    return;
  }

  // ─── Button Interactions ─────────────────────────────────────────────
  if (interaction.isButton()) {
    const customId = interaction.customId;

    // Upvote button: upvote_post_{postId}
    if (customId.startsWith('upvote_post_')) {
      const postId = parseInt(customId.replace('upvote_post_', ''), 10);
      if (isNaN(postId)) return;

      try {
        await interaction.deferReply({ ephemeral: true });
        await api.voteOnPost(postId, config.defaultUserId, 'UP');
        const post = await api.getPost(postId, config.defaultUserId);
        const up = post.upvoteCount ?? 0;
        const down = post.downvoteCount ?? 0;
        await addXp(interaction.user.id, 'LIKE_POST', XP_RULES.LIKE_POST, interaction.client);
        await interaction.editReply({
          embeds: [
            alertCard(interaction, C.success, '✅ Upvoted!', `Post **#${postId}** · ▲ **${up}** · ▼ **${down}**`),
          ],
        });
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        await interaction.editReply({
          embeds: [alertCard(interaction, C.danger, 'Vote failed', msg)],
        }).catch(() => {});
      }
      return;
    }

    // Discuss button: discuss_post_{postId}
    if (customId.startsWith('discuss_post_')) {
      const postId = parseInt(customId.replace('discuss_post_', ''), 10);
      if (isNaN(postId)) return;

      try {
        await interaction.deferReply({ ephemeral: true });
        const post = await api.getPost(postId, config.defaultUserId);
        const threadName = truncate(`💬 ${post.title || `Post #${postId}`}`, 100);

        // Create thread on the message that has the button
        const message = interaction.message;
        const thread = await message.startThread({
          name: threadName,
          autoArchiveDuration: 1440, // 24 hours
        });

        // Post the full content as first message in the thread
        const content = post.content || '*No content available.*';
        const chunks = [];
        for (let i = 0; i < content.length; i += 1900) {
          chunks.push(content.slice(i, i + 1900));
        }
        for (const chunk of chunks) {
          await thread.send(chunk);
        }

        await interaction.editReply({
          embeds: [
            alertCard(
              interaction,
              C.success,
              '💬 Thread Created',
              `Discussion thread created: <#${thread.id}>`
            ),
          ],
        });
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        await interaction.editReply({
          embeds: [alertCard(interaction, C.danger, 'Thread creation failed', msg)],
        }).catch(() => {});
      }
      return;
    }

    // Syllabus button: syllabus_course_{courseId}
    if (customId.startsWith('syllabus_course_')) {
      const courseId = parseInt(customId.replace('syllabus_course_', ''), 10);
      if (isNaN(courseId)) return;

      try {
        await interaction.deferReply({ ephemeral: true });
        const full = await api.downloadCourse(courseId);

        if (!full.sections?.length) {
          return interaction.editReply({
            embeds: [alertCard(interaction, C.muted, 'No syllabus', 'This course has no sections yet.')],
          });
        }

        const lines = full.sections
          .sort((a, b) => a.orderIndex - b.orderIndex)
          .map((s, i) => {
            const blockCount = s.blocks?.length ?? 0;
            return `**${i + 1}.** ${truncate(s.title, 60)} — ${blockCount} block${blockCount !== 1 ? 's' : ''}`;
          });

        await interaction.editReply({
          embeds: [
            alertCard(
              interaction,
              C.course,
              `📋 Syllabus: ${truncate(full.title, 50)}`,
              lines.join('\n') || '*Empty syllabus*'
            ),
          ],
        });
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        await interaction.editReply({
          embeds: [alertCard(interaction, C.danger, 'Syllabus failed', msg)],
        }).catch(() => {});
      }
      return;
    }

    // Pagination buttons
    const pagination = parsePaginationId(customId);
    if (pagination) {
      try {
        await interaction.deferUpdate();
        // Re-execute the original command with the new page
        const cmd = commands.get(pagination.prefix);
        if (cmd) {
          // For pagination we need to rebuild the response
          // This is handled by the individual commands accepting a page parameter
          // For now, reply with updated info
          await interaction.followUp({
            embeds: [
              alertCard(
                interaction,
                C.muted,
                'Navigation',
                `Use \`/${pagination.prefix} [page:${pagination.page}]\` to navigate.`
              ),
            ],
            ephemeral: true,
          });
        }
      } catch { /* ignore */ }
      return;
    }
  }

  // ─── Select Menus ────────────────────────────────────────────────────
  if (interaction.isStringSelectMenu()) {
    if (interaction.customId === 'subscribe_categories') {
      await interaction.deferReply({ ephemeral: true });
      try {
        const { getDb } = await import('../services/db.service.js');
        const db = await getDb();
        const userId = interaction.user.id;
        
        await db.run('DELETE FROM subscriptions WHERE user_id = ? AND target_type = ?', [userId, 'category']);
        
        for (const catId of interaction.values) {
          await db.run('INSERT INTO subscriptions (user_id, target_type, target_id) VALUES (?, ?, ?)', [userId, 'category', catId]);
        }
        
        await interaction.editReply({ content: `✅ Subscribed to ${interaction.values.length} categories!` });
      } catch (err) {
        console.error(err);
        await interaction.editReply({ content: '❌ Failed to save subscriptions.' });
      }
      return;
    }
  }
}
