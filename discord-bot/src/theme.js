import { EmbedBuilder } from 'discord.js';
import { config } from './config.js';
import { isHttpUrl, resolveCourseFileUrl } from './urls.js';

/** Discord embed color integers */
export const C = {
  primary: config.embedAccent,
  success: 0x10b981,
  warn: 0xf59e0b,
  danger: 0xef4444,
  muted: 0x64748b,
  course: 0x06b6d4
};

function botIcon(client) {
  return client.user?.displayAvatarURL({ size: 128 }) ?? undefined;
}

function userIcon(user) {
  return user.displayAvatarURL({ size: 64 });
}

/**
 * @param {import('discord.js').BaseInteraction} interaction
 * @param {{ color?: number; title: string; description?: string; url?: string; thumbnail?: string }} opts
 */
export function card(interaction, opts) {
  const e = new EmbedBuilder()
    .setColor(opts.color ?? C.primary)
    .setAuthor({
      name: 'TrustedWork · Community',
      iconURL: botIcon(interaction.client)
    })
    .setTitle(opts.title)
    .setTimestamp(new Date())
    .setFooter({
      text: interaction.user.tag,
      iconURL: userIcon(interaction.user)
    });
  if (opts.description != null) e.setDescription(opts.description);
  if (opts.url) e.setURL(opts.url);
  if (opts.thumbnail) e.setThumbnail(opts.thumbnail);
  return e;
}

/**
 * @param {import('discord.js').BaseInteraction} interaction
 * @param {number} color
 * @param {string} title
 * @param {string} body
 */
export function alertCard(interaction, color, title, body) {
  return card(interaction, { color, title, description: body });
}

export function truncate(s, max = 350) {
  if (!s) return '';
  return s.length <= max ? s : `${s.slice(0, max)}…`;
}

/** Styled list of communities */
export function formatCommunities(list) {
  return list
    .map((c) => {
      const desc = c.description ? `\n*${truncate(c.description, 140)}*` : '';
      return `**${truncate(c.name, 90)}** · \`#${c.id}\`${desc}`;
    })
    .join('\n\n');
}

/** Compact post rows */
export function formatPostRows(posts) {
  return posts
    .slice(0, 15)
    .map((p) => {
      const t = truncate(p.title, 72);
      const type = String(p.type);
      return `**#${p.id}** ${badge(type)} ${t}\n└ ${votesLine(p)}`;
    })
    .join('\n\n');
}

function badge(type) {
  const t = type.toUpperCase();
  if (t === 'COURSE') return '`COURSE`';
  if (t === 'TEXT' || t === 'POST') return '`POST`';
  return `\`${type}\``;
}

function votesLine(p) {
  return `▲ ${p.upvoteCount ?? 0}   ▼ ${p.downvoteCount ?? 0}`;
}

export function postTypeColor(type) {
  const t = String(type).toUpperCase();
  if (t === 'COURSE') return C.course;
  return C.primary;
}

/**
 * @param {import('discord.js').BaseInteraction} interaction
 * @param {object} p post DTO
 */
export function buildPostDetailEmbed(interaction, p) {
  const color = postTypeColor(p.type);
  const raw = truncate(p.content || '', 2000);
  const body = raw
    .split('\n')
    .map((line) => (line.trim() ? `> ${line}` : ''))
    .join('\n')
    .slice(0, 4096);

  const embed = card(interaction, {
    color,
    title: `#${p.id} · ${truncate(p.title, 220)}`,
    description: body.trim() ? body : '*No description*'
  });

  embed.addFields(
    { name: 'Type', value: `\`${String(p.type)}\``, inline: true },
    { name: 'Status', value: `\`${String(p.status)}\``, inline: true },
    { name: 'Community', value: `\`#${p.communityId}\``, inline: true },
    {
      name: 'Score',
      value: `▲ ${p.upvoteCount ?? 0} · ▼ ${p.downvoteCount ?? 0}`,
      inline: true
    },
    {
      name: 'Vote (linked app user)',
      value: p.myVote ? `\`${String(p.myVote)}\`` : '`—`',
      inline: true
    }
  );

  if (p.type === 'COURSE' && p.fileUrl) {
    const href = resolveCourseFileUrl(p.fileUrl);
    const value = isHttpUrl(href)
      ? `\`${href}\`\n[Open in browser](${href})`
      : `Raw: \`${String(p.fileUrl)}\`\nResolved: \`${href}\``;
    embed.addFields({
      name: 'Resource',
      value: truncate(value, 1024)
    });
  }

  return embed;
}
