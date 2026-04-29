import * as api from '../services/community.service.js';
import { alertCard } from '../embeds/common.js';
import { buildPostCard, formatPostRow } from '../embeds/post-card.js';
import { C } from '../utils/colors.js';
import { paginate, paginationRow } from '../utils/pagination.js';
import { config } from '../config/index.js';

export const name = 'posts';
export const aliases = ['list_posts'];

export async function execute(interaction, page = 1) {
  await interaction.deferReply({ ephemeral: true });
  const communityId = interaction.options.getInteger('community_id');
  const requestedPage = interaction.options.getInteger('page') ?? page;

  const posts = await api.listPosts({
    communityId: communityId ?? undefined,
    status: 'PUBLISHED',
    voterId: config.defaultUserId,
  });

  if (!posts?.length) {
    return interaction.editReply({
      embeds: [
        alertCard(
          interaction,
          C.warn,
          'No published posts',
          `Nothing **PUBLISHED** yet${communityId != null ? ` in community \`#${communityId}\`` : ''}.`
        ),
      ],
    });
  }

  const { items, totalPages, currentPage } = paginate(posts, requestedPage);
  const lines = items.map((p, i) => formatPostRow(p, (currentPage - 1) * 5 + i + 1));

  const subtitle =
    communityId != null
      ? `Community \`#${communityId}\` · ${posts.length} total`
      : `All communities · ${posts.length} total`;

  const embed = (await import('../embeds/common.js')).card(interaction, {
    color: C.primary,
    title: '📋 Published Posts',
    description: `_${subtitle}_\n\n${lines.join('\n\n')}`,
  });

  const components = [];
  if (totalPages > 1) {
    components.push(paginationRow('posts', currentPage, totalPages, communityId ?? ''));
  }

  await interaction.editReply({ embeds: [embed], components });
}
