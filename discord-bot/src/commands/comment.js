import * as api from '../services/community.service.js';
import { card } from '../embeds/common.js';
import { C } from '../utils/colors.js';
import { truncate } from '../utils/formatting.js';
import { config } from '../config/index.js';

export const name = 'comment';

export async function execute(interaction) {
  await interaction.deferReply();
  const postId = interaction.options.getInteger('post_id', true);
  const message = interaction.options.getString('message', true);
  const uid = config.defaultUserId;

  await api.addComment(postId, message, uid);

  const quoted = truncate(message, 1800)
    .split('\n')
    .map((line) => (line.trim() ? `> ${line}` : ''))
    .join('\n');

  await interaction.editReply({
    embeds: [
      card(interaction, {
        color: C.success,
        title: `💬 Comment on post #${postId}`,
        description: quoted || '*empty*',
      }),
    ],
  });
}
