import { EmbedBuilder } from 'discord.js';
import { getUserProfile, getLevelForXp } from '../services/xp.service.js';
import { C } from '../utils/colors.js';

export const name = 'profile';

function renderXpBar(xp, currentLevel, nextLevel) {
  const currentXp = xp - currentLevel.xp;
  const targetXp = (nextLevel ? nextLevel.xp : xp) - currentLevel.xp;
  const progress = targetXp > 0 ? currentXp / targetXp : 1;
  const blocks = Math.floor(progress * 10);
  return '█'.repeat(blocks) + '░'.repeat(10 - blocks);
}

export async function execute(interaction) {
  await interaction.deferReply();
  const targetUser = interaction.options.getUser('user') || interaction.user;
  
  const profile = await getUserProfile(targetUser.id);
  const currentLevel = getLevelForXp(profile.xp);
  
  // Find next level
  const levels = (await import('../services/xp.service.js')).LEVELS || [];
  const nextLevel = levels.find(l => l.xp > profile.xp);

  const xpBar = renderXpBar(profile.xp, currentLevel, nextLevel);
  const xpStr = nextLevel ? `${profile.xp} / ${nextLevel.xp} XP` : `${profile.xp} XP (Max Level)`;

  const embed = new EmbedBuilder()
    .setColor(C.primary)
    .setAuthor({ name: targetUser.tag, iconURL: targetUser.displayAvatarURL({ size: 64 }) })
    .setTitle(`Level ${currentLevel.level} — ${currentLevel.title}`)
    .addFields(
      { name: '🌟 Experience', value: `${xpBar} \n${xpStr}`, inline: false },
      { name: '📅 Joined Server', value: `<t:${Math.floor(new Date(profile.join_date).getTime() / 1000)}:R>`, inline: false }
    )
    .setThumbnail(targetUser.displayAvatarURL({ size: 128 }))
    .setFooter({ text: 'TrustedWork Community XP' });

  await interaction.editReply({ embeds: [embed] });
}
