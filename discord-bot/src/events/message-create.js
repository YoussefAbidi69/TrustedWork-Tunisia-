import { EmbedBuilder, ChannelType } from 'discord.js';
import { getDb } from '../services/db.service.js';
import { addXp, XP_RULES } from '../services/xp.service.js';
import { C } from '../utils/colors.js';
import OpenAI from 'openai';
import { config } from '../config/index.js';

let openai;
if (config.openaiApiKey) {
  openai = new OpenAI({ apiKey: config.openaiApiKey });
}

// Simple in-memory spam tracker: userId -> timestamps[]
const messageHistory = new Map();
const SPAM_LIMIT = 5;
const SPAM_WINDOW_MS = 10000; // 10 seconds

export const name = 'messageCreate';

export async function execute(message) {
  if (message.author.bot) return;

  const userId = message.author.id;
  const content = message.content.trim();
  const client = message.client;

  // ─── 1. Gamification: Comment XP ──────────────────────────────────────────
  // If in a thread that belongs to a forum or text channel, count as comment
  if (message.channel.isThread()) {
    // Basic anti-spam for XP
    if (content.length > 5) {
      await addXp(userId, 'COMMENT', XP_RULES.COMMENT, client);
    }
  }

  // ─── 2. Spam Detection ───────────────────────────────────────────────────
  if (!message.member?.permissions.has('Administrator')) {
    const now = Date.now();
    const history = messageHistory.get(userId) || [];
    // Clean old history
    const recent = history.filter(t => now - t < SPAM_WINDOW_MS);
    recent.push(now);
    messageHistory.set(userId, recent);

    if (recent.length > SPAM_LIMIT) {
      await handleModeration(message, 'Spam detection (rate limit exceeded)', 'timeout');
      return;
    }
  }

  // ─── 3. Toxicity Moderation ──────────────────────────────────────────────
  if (openai && content.length > 3) {
    // Don't block the event loop, run async
    openai.moderations.create({ input: content }).then(async (response) => {
      const result = response.results[0];
      if (result.flagged) {
        let reason = 'Violates community guidelines';
        if (result.categories.hate) reason = 'Hate speech';
        else if (result.categories['harassment']) reason = 'Harassment';
        else if (result.categories.sexual) reason = 'NSFW content';
        
        await handleModeration(message, reason, 'warn_and_delete');
      }
    }).catch(err => console.error('[moderation api]', err.message));
  }
}

async function handleModeration(message, reason, actionType) {
  try {
    const db = await getDb();
    const userId = message.author.id;
    
    await db.run('INSERT INTO mod_logs (user_id, action, reason) VALUES (?, ?, ?)', [userId, actionType, reason]);
    
    // Count previous violations
    const res = await db.get('SELECT COUNT(*) as count FROM mod_logs WHERE user_id = ?', [userId]);
    const violations = res.count;

    if (actionType === 'warn_and_delete') {
      await message.delete().catch(() => {});
      
      const embed = new EmbedBuilder()
        .setColor(C.danger)
        .setTitle('⚠️ Content Removed')
        .setDescription(`Your recent message was removed.\n**Reason:** ${reason}\n\nPlease review the community guidelines.`)
        .setFooter({ text: `Violation ${violations}/3` });
      
      await message.author.send({ embeds: [embed] }).catch(() => {});
    }

    if (actionType === 'timeout' || violations >= 3) {
      if (message.member && message.member.moderatable) {
        // Timeout for 10 minutes (or 1 hour if severe)
        const duration = violations > 3 ? 60 * 60 * 1000 : 10 * 60 * 1000;
        await message.member.timeout(duration, reason).catch(() => {});
        
        const embed = new EmbedBuilder()
          .setColor(C.danger)
          .setTitle('🚫 Timed Out')
          .setDescription(`You have been timed out for spam/violations.\n**Reason:** ${reason}`)
        await message.author.send({ embeds: [embed] }).catch(() => {});
      }
    }
  } catch (err) {
    console.error('[handleModeration]', err);
  }
}
