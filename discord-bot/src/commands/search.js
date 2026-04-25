import * as api from '../services/community.service.js';
import { card, alertCard } from '../embeds/common.js';
import { C } from '../utils/colors.js';
import { truncate, postAppUrl, courseAppUrl } from '../utils/formatting.js';
import { config } from '../config/index.js';

export const name = 'search';

export async function execute(interaction) {
  await interaction.deferReply({ ephemeral: true });
  const query = interaction.options.getString('query', true).toLowerCase();

  // Fetch all posts and courses, filter client-side (no backend search endpoint)
  const [posts, courses] = await Promise.all([
    api.listPosts({ status: 'PUBLISHED', voterId: config.defaultUserId }).catch(() => []),
    api.listCourses({ publishedOnly: true }).catch(() => []),
  ]);

  const matchedPosts = (posts || [])
    .filter((p) => p.title?.toLowerCase().includes(query) || p.content?.toLowerCase().includes(query))
    .slice(0, 5);

  const matchedCourses = (courses || [])
    .filter((c) => c.title?.toLowerCase().includes(query) || c.description?.toLowerCase().includes(query))
    .slice(0, 5);

  if (!matchedPosts.length && !matchedCourses.length) {
    return interaction.editReply({
      embeds: [alertCard(interaction, C.muted, 'No results', `No posts or courses match **"${truncate(query, 50)}"**.`)],
    });
  }

  const sections = [];

  if (matchedPosts.length) {
    const lines = matchedPosts.map((p, i) => {
      const url = postAppUrl(p.id);
      return `**${i + 1}.** [${truncate(p.title, 55)}](${url}) · ▲ ${p.upvoteCount ?? 0}`;
    });
    sections.push(`### 📝 Posts\n${lines.join('\n')}`);
  }

  if (matchedCourses.length) {
    const lines = matchedCourses.map((c, i) => {
      const url = courseAppUrl(c.id);
      const status = c.published ? '🟢' : '🟡';
      return `**${i + 1}.** ${status} [${truncate(c.title, 55)}](${url})`;
    });
    sections.push(`### 📚 Courses\n${lines.join('\n')}`);
  }

  await interaction.editReply({
    embeds: [
      card(interaction, {
        color: C.primary,
        title: `🔍 Search: "${truncate(query, 40)}"`,
        description: sections.join('\n\n'),
      }),
    ],
  });
}
