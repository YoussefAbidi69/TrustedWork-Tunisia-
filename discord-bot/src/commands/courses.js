import * as api from '../services/community.service.js';
import { alertCard } from '../embeds/common.js';
import { formatCourseRow } from '../embeds/course-card.js';
import { C } from '../utils/colors.js';
import { paginate, paginationRow } from '../utils/pagination.js';

export const name = 'courses';

export async function execute(interaction, page = 1) {
  await interaction.deferReply({ ephemeral: true });
  const communityId = interaction.options.getInteger('community_id');
  const publishedOnly = interaction.options.getBoolean('published_only') ?? true;
  const requestedPage = interaction.options.getInteger('page') ?? page;

  const courses = await api.listCourses({
    communityId: communityId ?? undefined,
    publishedOnly: publishedOnly || undefined,
  });

  if (!courses?.length) {
    return interaction.editReply({
      embeds: [
        alertCard(
          interaction,
          C.warn,
          'No courses found',
          `No courses found${communityId != null ? ` in community \`#${communityId}\`` : ''}.`
        ),
      ],
    });
  }

  const { items, totalPages, currentPage } = paginate(courses, requestedPage);
  const lines = items.map((c, i) => formatCourseRow(c, (currentPage - 1) * 5 + i + 1));

  const subtitle =
    communityId != null
      ? `Community \`#${communityId}\` · ${courses.length} total`
      : `All communities · ${courses.length} total`;

  const embed = (await import('../embeds/common.js')).card(interaction, {
    color: C.course,
    title: '📚 Courses',
    description: `_${subtitle}_\n\n${lines.join('\n\n')}`,
  });

  const components = [];
  if (totalPages > 1) {
    components.push(paginationRow('courses', currentPage, totalPages, communityId ?? ''));
  }

  await interaction.editReply({ embeds: [embed], components });
}
