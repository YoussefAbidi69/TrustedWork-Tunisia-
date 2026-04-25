import { SlashCommandBuilder, EmbedBuilder } from 'discord.js';
import { askQuestion } from '../services/rag.service.js';
import { C } from '../utils/colors.js';
import { config } from '../config/index.js';

export const name = 'ask';
export const description = 'Ask the AI a question about our community content';

export const build = new SlashCommandBuilder()
  .setName(name)
  .setDescription(description)
  .addStringOption(o => o.setName('question').setDescription('What do you want to know?').setRequired(true));

export async function execute(interaction) {
  if (!config.openaiApiKey) {
    return interaction.reply({ content: 'AI features are not configured.', ephemeral: true });
  }

  const query = interaction.options.getString('question');
  await interaction.deferReply();

  try {
    const result = await askQuestion(query);

    const embed = new EmbedBuilder()
      .setColor(C.primary)
      .setTitle(`💬 ${query}`)
      .setDescription(result.answer)
      .setFooter({ text: 'TrustedWork AI Assistant' })
      .setTimestamp(new Date());

    if (result.sources && result.sources.length > 0) {
      const sourceLinks = result.sources.map((s, i) => `[${i + 1}] [${s.title}](${s.url})`).join('\n');
      embed.addFields({ name: '📚 Sources', value: sourceLinks });
    }

    await interaction.editReply({ embeds: [embed] });
  } catch (err) {
    console.error('[ask cmd]', err);
    await interaction.editReply({ content: 'Sorry, I encountered an error while thinking.' });
  }
}
