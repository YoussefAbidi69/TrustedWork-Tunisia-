import * as api from '../services/community.service.js';
import { buildPostCard } from '../embeds/post-card.js';
import { config } from '../config/index.js';
import { addXp, XP_RULES } from '../services/xp.service.js';

export const name = 'post';

export async function execute(interaction) {
  await interaction.deferReply();
  const postId = interaction.options.getInteger('post_id', true);
  const post = await api.getPost(postId, config.defaultUserId);
  const { embed, row } = buildPostCard(interaction, post);
  
  await addXp(interaction.user.id, 'READ_POST', XP_RULES.READ_POST, interaction.client);
  
  await interaction.editReply({ embeds: [embed], components: [row] });
}
