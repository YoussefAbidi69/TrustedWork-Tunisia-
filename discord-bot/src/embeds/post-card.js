import { ActionRowBuilder, ButtonBuilder, ButtonStyle, EmbedBuilder } from 'discord.js';
import { card, serverCard } from './common.js';
import { communityColor, C } from '../utils/colors.js';
import { truncate, postAppUrl, statsLine } from '../utils/formatting.js';

/**
 * Build a rich post card embed (for interaction responses).
 * @param {import('discord.js').BaseInteraction} interaction
 * @param {object} post PostResponse from backend
 * @returns {{ embed: EmbedBuilder, row: ActionRowBuilder }}
 */
export function buildPostCard(interaction, post) {
  const color = communityColor(post.communityId);
  const appUrl = postAppUrl(post.id);
  const preview = truncate(post.content || '', 300);
  const bodyText = preview
    ? preview + (post.content?.length > 300 ? `\n\n[Read more →](${appUrl})` : '')
    : '*No content*';

  const embed = card(interaction, {
    color,
    title: `${truncate(post.title, 200)}`,
    description: bodyText,
    url: appUrl,
  });

  embed.addFields(
    { name: '📊 Score', value: statsLine(post), inline: true },
    { name: '🏷️ Status', value: `\`${post.status}\``, inline: true },
    { name: '🏠 Community', value: `\`#${post.communityId}\``, inline: true }
  );

  if (post.myVote) {
    embed.addFields({ name: '🗳️ Your Vote', value: `\`${post.myVote}\``, inline: true });
  }

  embed.setFooter({
    text: `Post #${post.id} · Author #${post.createdBy}`,
    iconURL: interaction.user?.displayAvatarURL({ size: 64 }),
  });

  // Action row with link buttons
  const row = new ActionRowBuilder().addComponents(
    new ButtonBuilder()
      .setLabel('👁 Read Post')
      .setStyle(ButtonStyle.Link)
      .setURL(appUrl),
    new ButtonBuilder()
      .setLabel('💬 Discuss')
      .setStyle(ButtonStyle.Primary)
      .setCustomId(`discuss_post_${post.id}`),
    new ButtonBuilder()
      .setLabel('▲ Upvote')
      .setStyle(ButtonStyle.Success)
      .setCustomId(`upvote_post_${post.id}`)
  );

  return { embed, row };
}

/**
 * Build a rich post card for server-sent messages (auto-sync, digests).
 */
export function buildServerPostCard(client, post) {
  const color = communityColor(post.communityId);
  const appUrl = postAppUrl(post.id);
  const preview = truncate(post.content || '', 300);
  const bodyText = preview
    ? preview + (post.content?.length > 300 ? `\n\n[Read more →](${appUrl})` : '')
    : '*No content*';

  const embed = serverCard(client, {
    color,
    title: `${truncate(post.title, 200)}`,
    description: bodyText,
    url: appUrl,
    footer: `Post #${post.id} · Community #${post.communityId} · Author #${post.createdBy}`,
  });

  embed.addFields(
    { name: '📊 Score', value: statsLine(post), inline: true },
    { name: '🏷️ Status', value: `\`${post.status}\``, inline: true }
  );

  const row = new ActionRowBuilder().addComponents(
    new ButtonBuilder()
      .setLabel('👁 Read Post')
      .setStyle(ButtonStyle.Link)
      .setURL(appUrl),
    new ButtonBuilder()
      .setLabel('💬 Discuss')
      .setStyle(ButtonStyle.Primary)
      .setCustomId(`discuss_post_${post.id}`)
  );

  return { embed, row };
}

/**
 * Compact post row for list views.
 */
export function formatPostRow(p, index) {
  const appUrl = postAppUrl(p.id);
  const up = p.upvoteCount ?? 0;
  const down = p.downvoteCount ?? 0;
  return `**${index}.** [${truncate(p.title, 60)}](${appUrl})\n└ ▲ ${up} · ▼ ${down} · Community \`#${p.communityId}\``;
}
