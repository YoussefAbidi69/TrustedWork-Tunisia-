import * as api from '../services/community.service.js';
import { card, alertCard } from '../embeds/common.js';
import { formatCourseRow } from '../embeds/course-card.js';
import { C } from '../utils/colors.js';

export const name = 'new_courses';

export async function execute(interaction) {
  await interaction.deferReply();

  const courses = await api.listCourses({ publishedOnly: true });

  if (!courses?.length) {
    return interaction.editReply({
      embeds: [alertCard(interaction, C.warn, 'No courses', 'No published courses found.')],
    });
  }

  // Take the last 5 (newest by ID — backend returns ordered by creation)
  const latest = [...courses].reverse().slice(0, 5);
  const lines = latest.map((c, i) => formatCourseRow(c, i + 1));

  await interaction.editReply({
    embeds: [
      card(interaction, {
        color: C.course,
        title: '🆕 Latest Courses',
        description: `**5 most recent published courses**\n\n${lines.join('\n\n')}`,
      }),
    ],
  });
}
