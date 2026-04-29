import { ActionRowBuilder, ButtonBuilder, ButtonStyle, EmbedBuilder } from 'discord.js';
import { card, serverCard } from './common.js';
import { courseStatusColor, communityColor, C } from '../utils/colors.js';
import { truncate, courseAppUrl } from '../utils/formatting.js';

/**
 * Build a rich course card embed.
 * @param {import('discord.js').BaseInteraction} interaction
 * @param {object} course CourseResponse from backend
 * @param {object} [extra] Optional extra data (sectionCount, etc.)
 * @returns {{ embed: EmbedBuilder, row: ActionRowBuilder }}
 */
export function buildCourseCard(interaction, course, extra = {}) {
  const color = courseStatusColor(course.published);
  const appUrl = courseAppUrl(course.id);
  const desc = truncate(course.description || '', 300);
  const bodyText = desc || '*No description provided*';

  const embed = card(interaction, {
    color,
    title: `📚 ${truncate(course.title, 200)}`,
    description: bodyText,
    url: appUrl,
  });

  embed.addFields(
    {
      name: '📖 Status',
      value: course.published ? '🟢 Published' : '🟡 Draft',
      inline: true,
    },
    {
      name: '🏠 Community',
      value: course.communityId ? `\`#${course.communityId}\`` : '—',
      inline: true,
    },
    {
      name: '👤 Author',
      value: `\`#${course.authorId}\``,
      inline: true,
    }
  );

  if (extra.sectionCount != null) {
    embed.addFields({
      name: '📋 Sections',
      value: `${extra.sectionCount} section${extra.sectionCount !== 1 ? 's' : ''}`,
      inline: true,
    });
  }

  embed.setFooter({
    text: `Course #${course.id}`,
    iconURL: interaction.user?.displayAvatarURL({ size: 64 }),
  });

  const row = new ActionRowBuilder().addComponents(
    new ButtonBuilder()
      .setLabel('▶️ View Course')
      .setStyle(ButtonStyle.Link)
      .setURL(appUrl),
    new ButtonBuilder()
      .setLabel('📋 Syllabus')
      .setStyle(ButtonStyle.Secondary)
      .setCustomId(`syllabus_course_${course.id}`)
  );

  return { embed, row };
}

/**
 * Build a server-sent course card (for auto-sync).
 */
export function buildServerCourseCard(client, course) {
  const color = courseStatusColor(course.published);
  const appUrl = courseAppUrl(course.id);
  const desc = truncate(course.description || '', 300);

  const embed = serverCard(client, {
    color,
    title: `📚 ${truncate(course.title, 200)}`,
    description: desc || '*No description provided*',
    url: appUrl,
    footer: `Course #${course.id} · Author #${course.authorId}`,
  });

  embed.addFields(
    {
      name: '📖 Status',
      value: course.published ? '🟢 Published' : '🟡 Draft',
      inline: true,
    },
    {
      name: '🏠 Community',
      value: course.communityId ? `\`#${course.communityId}\`` : '—',
      inline: true,
    }
  );

  const row = new ActionRowBuilder().addComponents(
    new ButtonBuilder()
      .setLabel('▶️ View Course')
      .setStyle(ButtonStyle.Link)
      .setURL(appUrl)
  );

  return { embed, row };
}

/**
 * Compact course row for list views.
 */
export function formatCourseRow(c, index) {
  const appUrl = courseAppUrl(c.id);
  const status = c.published ? '🟢' : '🟡';
  return `**${index}.** ${status} [${truncate(c.title, 60)}](${appUrl})\n└ Author \`#${c.authorId}\` · Community \`#${c.communityId ?? '—'}\``;
}
