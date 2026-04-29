import * as api from '../services/community.service.js';
import { card, alertCard } from '../embeds/common.js';
import { C } from '../utils/colors.js';
import { truncate, postAppUrl } from '../utils/formatting.js';
import { config } from '../config/index.js';

export const name = 'trending';

export async function execute(interaction) {
  await interaction.deferReply();

  const posts = await api.listPosts({
    status: 'PUBLISHED',
    voterId: config.defaultUserId,
  });

  if (!posts?.length) {
    return interaction.editReply({
      embeds: [alertCard(interaction, C.warn, 'No posts', 'No published posts found.')],
    });
  }

  // Sort by upvote count descending (no createdAt field available)
  const sorted = [...posts].sort((a, b) => (b.upvoteCount ?? 0) - (a.upvoteCount ?? 0));
  const top5 = sorted.slice(0, 5);

  const lines = top5.map((p, i) => {
    const emoji = ['🥇', '🥈', '🥉', '4️⃣', '5️⃣'][i];
    const url = postAppUrl(p.id);
    const up = p.upvoteCount ?? 0;
    const down = p.downvoteCount ?? 0;
    return `${emoji} [${truncate(p.title, 55)}](${url})\n  └ ▲ **${up}** · ▼ **${down}** · Community \`#${p.communityId}\``;
  });

  await interaction.editReply({
    embeds: [
      card(interaction, {
        color: C.trending,
        title: '🔥 Trending Posts',
        description: `**Top 5 by upvotes**\n\n${lines.join('\n\n')}`,
      }),
    ],
  });
}
