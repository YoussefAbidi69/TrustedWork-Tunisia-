import 'dotenv/config';
import { REST, Routes, SlashCommandBuilder, PermissionFlagsBits } from 'discord.js';

const token = process.env.DISCORD_TOKEN;
const clientId = process.env.DISCORD_CLIENT_ID;
const guildId = process.env.DISCORD_GUILD_ID;

if (!token || !clientId) {
  console.error('Set DISCORD_TOKEN and DISCORD_CLIENT_ID in .env');
  process.exit(1);
}

const commands = [
  // ─── Community browsing ──────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('communities')
    .setDescription('List all communities'),

  // ─── Posts ───────────────────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('posts')
    .setDescription('Browse published posts with pagination')
    .addIntegerOption((o) =>
      o.setName('community_id').setDescription('Filter by community ID').setRequired(false)
    )
    .addIntegerOption((o) =>
      o.setName('page').setDescription('Page number (default 1)').setRequired(false).setMinValue(1)
    ),

  new SlashCommandBuilder()
    .setName('list_posts')
    .setDescription('Alias for /posts')
    .addIntegerOption((o) =>
      o.setName('community_id').setDescription('Filter by community ID').setRequired(false)
    )
    .addIntegerOption((o) =>
      o.setName('page').setDescription('Page number').setRequired(false).setMinValue(1)
    ),

  new SlashCommandBuilder()
    .setName('post')
    .setDescription('Show a specific post card')
    .addIntegerOption((o) =>
      o.setName('post_id').setDescription('Post ID').setRequired(true)
    ),

  // ─── Courses ─────────────────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('courses')
    .setDescription('Browse courses with pagination')
    .addIntegerOption((o) =>
      o.setName('community_id').setDescription('Filter by community ID').setRequired(false)
    )
    .addBooleanOption((o) =>
      o.setName('published_only').setDescription('Show only published (default true)').setRequired(false)
    )
    .addIntegerOption((o) =>
      o.setName('page').setDescription('Page number').setRequired(false).setMinValue(1)
    ),

  new SlashCommandBuilder()
    .setName('course')
    .setDescription('Show a specific course card')
    .addIntegerOption((o) =>
      o.setName('course_id').setDescription('Course ID').setRequired(true)
    ),

  // ─── Interaction ─────────────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('vote')
    .setDescription('Upvote or downvote a post')
    .addIntegerOption((o) =>
      o.setName('post_id').setDescription('Post ID').setRequired(true)
    )
    .addStringOption((o) =>
      o
        .setName('direction')
        .setDescription('Vote direction')
        .setRequired(true)
        .addChoices(
          { name: 'up', value: 'UP' },
          { name: 'down', value: 'DOWN' }
        )
    ),

  new SlashCommandBuilder()
    .setName('comment')
    .setDescription('Add a comment to a post')
    .addIntegerOption((o) =>
      o.setName('post_id').setDescription('Post ID').setRequired(true)
    )
    .addStringOption((o) =>
      o.setName('message').setDescription('Comment text').setRequired(true)
    ),

  // ─── Course management ──────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('upload_course')
    .setDescription('Upload a PDF and publish a course post')
    .addIntegerOption((o) =>
      o.setName('community_id').setDescription('Community ID').setRequired(true)
    )
    .addStringOption((o) =>
      o.setName('title').setDescription('Course title').setRequired(true)
    )
    .addAttachmentOption((o) =>
      o.setName('file').setDescription('PDF file').setRequired(true)
    ),

  new SlashCommandBuilder()
    .setName('download_course')
    .setDescription('Download a course PDF')
    .addIntegerOption((o) =>
      o.setName('post_id').setDescription('Course post ID').setRequired(true)
    )
    .addBooleanOption((o) =>
      o.setName('attach').setDescription('Attach file in Discord (max ~25 MB)').setRequired(false)
    ),

  // ─── Discovery ──────────────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('search')
    .setDescription('Search posts and courses')
    .addStringOption((o) =>
      o.setName('query').setDescription('Search keywords').setRequired(true)
    ),

  new SlashCommandBuilder()
    .setName('trending')
    .setDescription('Show top 5 trending posts by upvotes'),

  new SlashCommandBuilder()
    .setName('new_courses')
    .setDescription('Show the 5 latest published courses'),

  // ─── Admin ──────────────────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('setup_server')
    .setDescription('Auto-create channels and categories mirroring the app (admin only)')
    .setDefaultMemberPermissions(PermissionFlagsBits.Administrator),

  new SlashCommandBuilder()
    .setName('sync')
    .setDescription('Force re-sync all channels with latest content (admin only)')
    .setDefaultMemberPermissions(PermissionFlagsBits.Administrator),

  // ─── Advanced Features ────────────────────────────────────────────────
  new SlashCommandBuilder()
    .setName('ask')
    .setDescription('Ask the AI a question about our community content')
    .addStringOption(o => o.setName('question').setDescription('What do you want to know?').setRequired(true)),
  
  new SlashCommandBuilder()
    .setName('profile')
    .setDescription("Show your or another user's gamification profile")
    .addUserOption(o => o.setName('user').setDescription('User to view').setRequired(false)),

  new SlashCommandBuilder()
    .setName('leaderboard')
    .setDescription('Show top 10 members by XP'),

  new SlashCommandBuilder()
    .setName('subscribe')
    .setDescription('Subscribe to categories to get DM notifications for new content')
    .addSubcommand(sub => sub.setName('categories').setDescription('Subscribe to content categories')),

  new SlashCommandBuilder()
    .setName('analytics')
    .setDescription('Show real-time community analytics (admin only)')
    .setDefaultMemberPermissions(PermissionFlagsBits.Administrator),

].map((c) => c.toJSON());

const rest = new REST({ version: '10' }).setToken(token);

try {
  if (guildId) {
    await rest.put(Routes.applicationGuildCommands(clientId, guildId), { body: commands });
    console.log(`✅ Registered ${commands.length} guild commands for guild ${guildId}`);
  } else {
    await rest.put(Routes.applicationCommands(clientId), { body: commands });
    console.log(`✅ Registered ${commands.length} global commands (can take up to 1h to appear)`);
  }
} catch (e) {
  console.error(e);
  process.exit(1);
}
