import { EmbedBuilder } from 'discord.js';
import { getLeaderboard, getLevelForXp } from '../services/xp.service.js';
import { C } from '../utils/colors.js';

export const name = 'leaderboard';

export async function execute(interaction) {
  await interaction.deferReply();
  const topUsers = await getLeaderboard(10);

  if (!topUsers.length) {
    return interaction.editReply({ content: 'No users have earned XP yet.' });
  }

  const lines = await Promise.all(topUsers.map(async (u, i) => {
    const emoji = ['🥇', '🥈', '🥉', '4️⃣', '5️⃣', '6️⃣', '7️⃣', '8️⃣', '9️⃣', '🔟'][i];
    let tag = `User \`${u.id}\``;
    try {
      const discordUser = await interaction.client.users.fetch(u.id);
      if (discordUser) tag = `**${discordUser.username}**`;
    } catch { /* user might have left */ }
    
    const levelObj = getLevelForXp(u.xp);
    return `${emoji} ${tag} — Level ${levelObj.level} (${u.xp} XP)`;
  }));

  const embed = new EmbedBuilder()
    .setColor(C.primary)
    .setTitle('🏆 Community Leaderboard')
    .setDescription(lines.join('\n\n'))
    .setFooter({ text: 'Earn XP by reading, commenting, and participating!' })
    .setTimestamp(new Date());

  await interaction.editReply({ embeds: [embed] });
}
