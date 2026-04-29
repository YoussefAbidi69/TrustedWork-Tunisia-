import { EmbedBuilder } from 'discord.js';
import { C } from '../utils/colors.js';
import { truncate, postAppUrl, courseAppUrl } from '../utils/formatting.js';

/**
 * Build a Daily Digest embed.
 * @param {import('discord.js').Client} client
 * @param {object[]} topPosts Top 5 posts
 * @returns {EmbedBuilder}
 */
export function buildDailyDigest(client, topPosts) {
  const lines = topPosts.slice(0, 5).map((p, i) => {
    const emoji = ['🥇', '🥈', '🥉', '4️⃣', '5️⃣'][i] || `${i + 1}.`;
    const url = postAppUrl(p.id);
    const up = p.upvoteCount ?? 0;
    return `${emoji} [${truncate(p.title, 55)}](${url})\n  └ ▲ **${up}** · Community \`#${p.communityId}\``;
  });

  const embed = new EmbedBuilder()
    .setColor(C.digest)
    .setAuthor({
      name: 'TrustedWork · Community',
      iconURL: client.user?.displayAvatarURL({ size: 128 }),
    })
    .setTitle('📰 Daily Digest')
    .setDescription(
      `**Top posts from the last 24 hours**\n\n${lines.join('\n\n') || '_No posts found._'}`
    )
    .setTimestamp(new Date())
    .setFooter({ text: 'Automated daily summary' });

  return embed;
}

/**
 * Build a Weekly Highlights embed.
 * @param {import('discord.js').Client} client
 * @param {object[]} topPosts
 * @param {object[]} newCourses
 * @returns {EmbedBuilder}
 */
export function buildWeeklyDigest(client, topPosts, newCourses) {
  const postLines = topPosts.slice(0, 5).map((p, i) => {
    const emoji = ['🥇', '🥈', '🥉', '4️⃣', '5️⃣'][i] || `${i + 1}.`;
    const url = postAppUrl(p.id);
    const up = p.upvoteCount ?? 0;
    return `${emoji} [${truncate(p.title, 55)}](${url}) · ▲ **${up}**`;
  });

  const courseLines = newCourses.slice(0, 5).map((c, i) => {
    const url = courseAppUrl(c.id);
    return `**${i + 1}.** [${truncate(c.title, 55)}](${url}) · Author \`#${c.authorId}\``;
  });

  const sections = [];
  sections.push(`## 🔥 Top Posts\n${postLines.join('\n') || '_None this week._'}`);
  sections.push(`## 📚 New Courses\n${courseLines.join('\n') || '_None this week._'}`);

  const embed = new EmbedBuilder()
    .setColor(C.digest)
    .setAuthor({
      name: 'TrustedWork · Community',
      iconURL: client.user?.displayAvatarURL({ size: 128 }),
    })
    .setTitle('🌟 Weekly Highlights')
    .setDescription(sections.join('\n\n'))
    .setTimestamp(new Date())
    .setFooter({ text: 'Automated weekly summary · Every Monday' });

  return embed;
}
