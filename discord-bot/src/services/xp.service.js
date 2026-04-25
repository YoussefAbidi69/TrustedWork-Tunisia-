import { getDb } from './db.service.js';
import { EmbedBuilder } from 'discord.js';
import { C } from '../utils/colors.js';

export const XP_RULES = {
  READ_POST: 5,
  COMPLETE_COURSE: 100,
  LIKE_POST: 2,
  COMMENT: 10,
  SHARE: 8,
  DAILY_LOGIN: 3,
  WIN_QUIZ: 25,
  REFERRAL: 50,
};

const LEVELS = [
  { level: 1, xp: 0, title: 'Newcomer', roleName: 'Level 1: Newcomer' },
  { level: 2, xp: 100, title: 'Novice' },
  { level: 3, xp: 250, title: 'Explorer', roleName: 'Level 3: Explorer' },
  { level: 4, xp: 500, title: 'Apprentice' },
  { level: 5, xp: 1000, title: 'Scholar', roleName: 'Level 5: Scholar' },
  { level: 6, xp: 2000, title: 'Adept' },
  { level: 7, xp: 3500, title: 'Expert', roleName: 'Level 7: Expert' },
  { level: 8, xp: 5500, title: 'Master' },
  { level: 9, xp: 8000, title: 'Grandmaster' },
  { level: 10, xp: 12000, title: 'Legend', roleName: 'Level 10: Legend' },
];

export function getLevelForXp(xp) {
  let current = LEVELS[0];
  for (const l of LEVELS) {
    if (xp >= l.xp) current = l;
    else break;
  }
  return current;
}

export async function addXp(discordUserId, action, xpAmount, client) {
  const db = await getDb();
  
  // Ensure user exists
  await db.run('INSERT OR IGNORE INTO users (id) VALUES (?)', [discordUserId]);
  
  const user = await db.get('SELECT xp, level FROM users WHERE id = ?', [discordUserId]);
  const newXp = (user.xp || 0) + xpAmount;
  const newLevelObj = getLevelForXp(newXp);
  const oldLevel = user.level || 1;

  // Record event
  await db.run('INSERT INTO xp_events (user_id, action, xp_awarded) VALUES (?, ?, ?)', [discordUserId, action, xpAmount]);
  
  // Update user
  await db.run('UPDATE users SET xp = ?, level = ? WHERE id = ?', [newXp, newLevelObj.level, discordUserId]);

  if (newLevelObj.level > oldLevel && client) {
    await handleLevelUp(discordUserId, newLevelObj, client);
  }
}

async function handleLevelUp(discordUserId, newLevelObj, client) {
  try {
    const user = await client.users.fetch(discordUserId);
    if (!user) return;

    // Send DM
    const embed = new EmbedBuilder()
      .setColor(C.success)
      .setTitle('🎉 Level Up!')
      .setDescription(`Congratulations! You've reached **Level ${newLevelObj.level} (${newLevelObj.title})**!`)
      .setThumbnail(user.displayAvatarURL({ size: 128 }))
      .setTimestamp(new Date());

    if (newLevelObj.roleName) {
      embed.addFields({ name: 'New Role Unlocked', value: `\`${newLevelObj.roleName}\`` });
    }

    await user.send({ embeds: [embed] }).catch(() => {});

    // Assign Role in all mutual guilds (for simplicity, usually you'd check a specific guild)
    for (const [guildId, guild] of client.guilds.cache) {
      try {
        const member = await guild.members.fetch(discordUserId);
        if (member && newLevelObj.roleName) {
          const role = guild.roles.cache.find(r => r.name === newLevelObj.roleName);
          if (role) {
            await member.roles.add(role);
          }
        }
      } catch { /* ignore */ }
    }
  } catch (err) {
    console.error('[handleLevelUp]', err);
  }
}

export async function getLeaderboard(limit = 10) {
  const db = await getDb();
  return db.all('SELECT id, xp, level FROM users ORDER BY xp DESC LIMIT ?', [limit]);
}

export async function getUserProfile(discordUserId) {
  const db = await getDb();
  const user = await db.get('SELECT * FROM users WHERE id = ?', [discordUserId]);
  if (!user) return { xp: 0, level: 1, join_date: new Date().toISOString() };
  return user;
}
