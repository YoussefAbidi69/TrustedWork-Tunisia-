import { EmbedBuilder, PermissionFlagsBits, SlashCommandBuilder } from 'discord.js';
import { getDb } from '../services/db.service.js';
import { C } from '../utils/colors.js';

export const name = 'analytics';

export const build = new SlashCommandBuilder()
  .setName(name)
  .setDescription('Show real-time community analytics (admin only)')
  .setDefaultMemberPermissions(PermissionFlagsBits.Administrator);

export async function execute(interaction) {
  await interaction.deferReply({ ephemeral: false });

  const db = await getDb();
  
  // Get discord stats
  const totalMembers = interaction.guild.memberCount;
  
  // Get gamification stats
  const { totalXp } = await db.get('SELECT SUM(xp) as totalXp FROM users') || { totalXp: 0 };
  const { totalEvents } = await db.get('SELECT COUNT(*) as totalEvents FROM xp_events') || { totalEvents: 0 };
  
  // Get moderation stats
  const { totalMods } = await db.get('SELECT COUNT(*) as totalMods FROM mod_logs') || { totalMods: 0 };

  const embed = new EmbedBuilder()
    .setColor(C.admin)
    .setTitle('📊 Real-Time Community Analytics')
    .addFields(
      { name: '👥 Members', value: `${totalMembers}`, inline: true },
      { name: '✨ Total XP Earned', value: `${totalXp || 0}`, inline: true },
      { name: '🎮 Total Events', value: `${totalEvents || 0}`, inline: true },
      { name: '🛡️ Mod Actions', value: `${totalMods || 0}`, inline: true }
    )
    .setFooter({ text: 'Powered by TrustedWork Analytics Engine' })
    .setTimestamp(new Date());

  await interaction.editReply({ embeds: [embed] });
}
