import 'dotenv/config';
import { REST, Routes, SlashCommandBuilder } from 'discord.js';

const token = process.env.DISCORD_TOKEN;
const clientId = process.env.DISCORD_CLIENT_ID;
const guildId = process.env.DISCORD_GUILD_ID;

if (!token || !clientId) {
  console.error('Set DISCORD_TOKEN and DISCORD_CLIENT_ID in .env');
  process.exit(1);
}

const commands = [
  new SlashCommandBuilder().setName('communities').setDescription('List communities from ms-community'),
  new SlashCommandBuilder()
    .setName('posts')
    .setDescription('List published posts (optionally in one community)')
    .addIntegerOption((o) =>
      o.setName('community_id').setDescription('Filter by community id').setRequired(false)
    ),
  new SlashCommandBuilder()
    .setName('list_posts')
    .setDescription('Same as /posts — use this if another app also registers /posts')
    .addIntegerOption((o) =>
      o.setName('community_id').setDescription('Filter by community id').setRequired(false)
    ),
  new SlashCommandBuilder()
    .setName('post')
    .setDescription('Show one post')
    .addIntegerOption((o) => o.setName('post_id').setDescription('Post id').setRequired(true)),
  new SlashCommandBuilder()
    .setName('vote')
    .setDescription('Upvote / downvote / toggle (same button removes vote)')
    .addIntegerOption((o) => o.setName('post_id').setDescription('Post id').setRequired(true))
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
    .setDescription('Add a comment on a post')
    .addIntegerOption((o) => o.setName('post_id').setDescription('Post id').setRequired(true))
    .addStringOption((o) => o.setName('message').setDescription('Comment text').setRequired(true)),
  new SlashCommandBuilder()
    .setName('upload_course')
    .setDescription('Upload a PDF and publish a COURSE post (uses ms-community → FilePost)')
    .addIntegerOption((o) => o.setName('community_id').setDescription('Community id').setRequired(true))
    .addStringOption((o) => o.setName('title').setDescription('Course title').setRequired(true))
    .addAttachmentOption((o) =>
      o.setName('file').setDescription('PDF file').setRequired(true)
    ),
  new SlashCommandBuilder()
    .setName('download_course')
    .setDescription('Download course PDF for a post (or get link)')
    .addIntegerOption((o) => o.setName('post_id').setDescription('Course post id').setRequired(true))
    .addBooleanOption((o) =>
      o
        .setName('attach')
        .setDescription('Try to attach file in Discord (max ~25 MB)')
        .setRequired(false)
    )
].map((c) => c.toJSON());

const rest = new REST({ version: '10' }).setToken(token);

try {
  if (guildId) {
    await rest.put(Routes.applicationGuildCommands(clientId, guildId), { body: commands });
    console.log(`Registered ${commands.length} guild commands for guild ${guildId}`);
  } else {
    await rest.put(Routes.applicationCommands(clientId), { body: commands });
    console.log(`Registered ${commands.length} global commands (can take up to 1h to appear)`);
  }
} catch (e) {
  console.error(e);
  process.exit(1);
}
