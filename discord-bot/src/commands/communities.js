import * as api from '../services/community.service.js';
import { card, alertCard } from '../embeds/common.js';
import { C } from '../utils/colors.js';
import { truncate } from '../utils/formatting.js';

export const name = 'communities';

export async function execute(interaction) {
  await interaction.deferReply();
  const list = await api.listCommunities();

  if (!list?.length) {
    return interaction.editReply({
      embeds: [
        alertCard(
          interaction,
          C.warn,
          'No communities',
          'The API returned an empty list. Check that **ms-community** is running.'
        ),
      ],
    });
  }

  const description = list
    .map((c) => {
      const desc = c.description ? `\n*${truncate(c.description, 140)}*` : '';
      return `**${truncate(c.name, 90)}** · \`#${c.id}\`${desc}`;
    })
    .join('\n\n');

  await interaction.editReply({
    embeds: [
      card(interaction, {
        color: C.primary,
        title: `Communities · ${list.length} total`,
        description,
      }),
    ],
  });
}
