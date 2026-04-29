import { EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle, StringSelectMenuBuilder } from 'discord.js';
import { C } from '../utils/colors.js';
import { config } from '../config/index.js';

export const name = 'guildMemberAdd';

export async function execute(member) {
  if (member.user.bot) return;

  const embed = new EmbedBuilder()
    .setColor(C.primary)
    .setTitle('👋 Welcome to TrustedWork Community!')
    .setDescription(`Hi ${member.user.username}, we're excited to have you here! This server is your hub for connecting with peers, sharing knowledge, and leveling up your skills.`)
    .addFields(
      { name: '🎯 What to do next?', value: 'Take a quick 3-step tour to set up your profile and discover the best content tailored to your interests.' }
    )
    .setThumbnail(member.guild.iconURL({ size: 128 }))
    .setFooter({ text: 'TrustedWork Community Team' });

  const row = new ActionRowBuilder().addComponents(
    new ButtonBuilder()
      .setCustomId('tour_start')
      .setLabel('🗺️ Take the Tour')
      .setStyle(ButtonStyle.Primary),
    new ButtonBuilder()
      .setLabel('💻 Open Web App')
      .setStyle(ButtonStyle.Link)
      .setURL(config.appBaseUrl)
  );

  try {
    await member.send({ embeds: [embed], components: [row] });
  } catch (err) {
    console.log(`[guildMemberAdd] Could not DM user ${member.user.tag}`);
  }
}
