import { EmbedBuilder, StringSelectMenuBuilder, ActionRowBuilder, SlashCommandBuilder } from 'discord.js';
import { getDb } from '../services/db.service.js';
import { C } from '../utils/colors.js';
import * as api from '../services/community.service.js';

export const name = 'subscribe';

export const build = new SlashCommandBuilder()
  .setName(name)
  .setDescription('Subscribe to categories to get DM notifications for new content')
  .addSubcommand(sub => sub.setName('categories').setDescription('Subscribe to content categories'));

export async function execute(interaction) {
  await interaction.deferReply({ ephemeral: true });

  const communities = await api.listCommunities();
  if (!communities || communities.length === 0) {
    return interaction.editReply({ content: 'No categories available right now.' });
  }

  const db = await getDb();
  const currentSubs = await db.all('SELECT target_id FROM subscriptions WHERE user_id = ? AND target_type = ?', [interaction.user.id, 'category']);
  const subbedIds = currentSubs.map(s => s.target_id);

  const options = communities.map(c => ({
    label: c.name,
    description: c.description ? c.description.slice(0, 100) : 'Community category',
    value: String(c.id),
    default: subbedIds.includes(String(c.id))
  }));

  const selectMenu = new StringSelectMenuBuilder()
    .setCustomId('subscribe_categories')
    .setPlaceholder('Select categories to subscribe to')
    .setMinValues(0)
    .setMaxValues(options.length)
    .addOptions(options);

  const row = new ActionRowBuilder().addComponents(selectMenu);

  const embed = new EmbedBuilder()
    .setColor(C.primary)
    .setTitle('🔔 Content Subscriptions')
    .setDescription('Select the categories you want to follow. You will receive a DM when new posts or courses are published in these categories.')
    .setFooter({ text: 'You can update this at any time.' });

  await interaction.editReply({ embeds: [embed], components: [row] });
}
