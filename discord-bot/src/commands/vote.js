import * as api from '../services/community.service.js';
import { buildPostCard } from '../embeds/post-card.js';
import { card } from '../embeds/common.js';
import { C } from '../utils/colors.js';
import { config } from '../config/index.js';

export const name = 'vote';

export async function execute(interaction) {
  await interaction.deferReply();
  const postId = interaction.options.getInteger('post_id', true);
  const direction = interaction.options.getString('direction', true);
  const uid = config.defaultUserId;

  await api.voteOnPost(postId, uid, direction);
  const post = await api.getPost(postId, uid);

  const { embed, row } = buildPostCard(interaction, post);

  // Add a confirmation field
  embed.spliceFields(0, 0, {
    name: '✅ Vote Recorded',
    value: `You voted **${direction}** on this post.`,
    inline: false,
  });

  await interaction.editReply({ embeds: [embed], components: [row] });
}
