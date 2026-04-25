import * as api from '../services/community.service.js';
import { buildCourseCard } from '../embeds/course-card.js';
import { addXp, XP_RULES } from '../services/xp.service.js';

export const name = 'course';

export async function execute(interaction) {
  await interaction.deferReply();
  const courseId = interaction.options.getInteger('course_id', true);
  const course = await api.getCourse(courseId);

  // Try to get section count for extra info
  let sectionCount;
  try {
    const sections = await api.listSections(courseId);
    sectionCount = sections?.length ?? 0;
  } catch {
    sectionCount = undefined;
  }

  const { embed, row } = buildCourseCard(interaction, course, { sectionCount });
  
  await addXp(interaction.user.id, 'READ_POST', XP_RULES.READ_POST, interaction.client);
  
  await interaction.editReply({ embeds: [embed], components: [row] });
}
