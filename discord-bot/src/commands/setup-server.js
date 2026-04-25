import { ChannelType, PermissionFlagsBits, EmbedBuilder } from 'discord.js';
import * as api from '../services/community.service.js';
import { alertCard } from '../embeds/common.js';
import { C } from '../utils/colors.js';
import { setGuildConfig } from '../config/guild-store.js';

export const name = 'setup_server';

export async function execute(interaction) {
  // Admin check
  if (!interaction.memberPermissions?.has(PermissionFlagsBits.Administrator)) {
    return interaction.reply({
      embeds: [alertCard(interaction, C.danger, 'Permission denied', 'You need **Administrator** permission.')],
      ephemeral: true,
    });
  }

  await interaction.deferReply();
  const guild = interaction.guild;
  if (!guild) {
    return interaction.editReply({
      embeds: [alertCard(interaction, C.danger, 'Error', 'This command can only be used in a server.')],
    });
  }

  const status = [];

  try {
    // ─── 1. Fetch communities from backend ─────────────────────────────
    const communities = await api.listCommunities();
    status.push(`✅ Fetched **${communities?.length ?? 0}** communities from backend`);

    // ─── 2. Create #welcome channel ────────────────────────────────────
    let welcomeChannel;
    const existingWelcome = guild.channels.cache.find(
      (ch) => ch.name === 'welcome' && ch.type === ChannelType.GuildText
    );
    if (existingWelcome) {
      welcomeChannel = existingWelcome;
      status.push(`ℹ️ #welcome already exists — skipped`);
    } else {
      welcomeChannel = await guild.channels.create({
        name: 'welcome',
        type: ChannelType.GuildText,
        topic: 'Welcome to the TrustedWork Community mirror!',
        permissionOverwrites: [
          {
            id: guild.roles.everyone.id,
            deny: [PermissionFlagsBits.SendMessages],
            allow: [PermissionFlagsBits.ViewChannel],
          },
          {
            id: guild.members.me.id,
            allow: [PermissionFlagsBits.SendMessages, PermissionFlagsBits.EmbedLinks],
          },
        ],
      });
      // Post welcome embed
      const welcomeEmbed = new EmbedBuilder()
        .setColor(C.primary)
        .setTitle('👋 Welcome to TrustedWork Community')
        .setDescription(
          'This Discord server is a **live mirror** of the TrustedWork application.\n\n' +
          '📋 **Posts** and **Courses** from the app are automatically synced here.\n' +
          '🔔 New content appears in real-time in the appropriate channels.\n' +
          '💬 Use threads on any post card to discuss.\n\n' +
          '**Slash Commands:**\n' +
          '`/posts` — Browse posts with pagination\n' +
          '`/courses` — Browse courses\n' +
          '`/search <query>` — Search content\n' +
          '`/trending` — See what\'s hot\n' +
          '`/post <id>` — View a specific post\n' +
          '`/course <id>` — View a specific course'
        )
        .setTimestamp(new Date())
        .setFooter({ text: 'TrustedWork · Community Bot' });
      await welcomeChannel.send({ embeds: [welcomeEmbed] });
      status.push(`✅ Created **#welcome** channel`);
    }

    // ─── 3. Create #feed channel ───────────────────────────────────────
    let feedChannel;
    const existingFeed = guild.channels.cache.find(
      (ch) => ch.name === 'feed' && ch.type === ChannelType.GuildText
    );
    if (existingFeed) {
      feedChannel = existingFeed;
      status.push(`ℹ️ #feed already exists — skipped`);
    } else {
      feedChannel = await guild.channels.create({
        name: 'feed',
        type: ChannelType.GuildText,
        topic: 'Global activity feed — all new posts and courses appear here',
        permissionOverwrites: [
          {
            id: guild.roles.everyone.id,
            deny: [PermissionFlagsBits.SendMessages],
            allow: [PermissionFlagsBits.ViewChannel],
          },
          {
            id: guild.members.me.id,
            allow: [PermissionFlagsBits.SendMessages, PermissionFlagsBits.EmbedLinks],
          },
        ],
      });
      status.push(`✅ Created **#feed** channel`);
    }

    // ─── 4. Create category + channels per community ───────────────────
    const categoriesMap = {};

    for (const community of communities || []) {
      const catName = community.name || `Community ${community.id}`;

      // Check if category already exists
      const existingCat = guild.channels.cache.find(
        (ch) => ch.name.toLowerCase() === catName.toLowerCase() && ch.type === ChannelType.GuildCategory
      );

      let category;
      if (existingCat) {
        category = existingCat;
        status.push(`ℹ️ Category **${catName}** already exists — reusing`);
      } else {
        category = await guild.channels.create({
          name: catName,
          type: ChannelType.GuildCategory,
        });
        status.push(`✅ Created category **${catName}**`);
      }

      // Create #posts channel in category
      let postsChannel = guild.channels.cache.find(
        (ch) => ch.name === 'posts' && ch.parentId === category.id
      );
      if (!postsChannel) {
        postsChannel = await guild.channels.create({
          name: 'posts',
          type: ChannelType.GuildText,
          parent: category.id,
          topic: `Latest posts from ${catName}`,
          permissionOverwrites: [
            {
              id: guild.roles.everyone.id,
              deny: [PermissionFlagsBits.SendMessages],
              allow: [PermissionFlagsBits.ViewChannel],
            },
            {
              id: guild.members.me.id,
              allow: [PermissionFlagsBits.SendMessages, PermissionFlagsBits.EmbedLinks],
            },
          ],
        });
        status.push(`  ✅ Created **#posts** in ${catName}`);
      }

      // Create #courses channel in category
      let coursesChannel = guild.channels.cache.find(
        (ch) => ch.name === 'courses' && ch.parentId === category.id
      );
      if (!coursesChannel) {
        coursesChannel = await guild.channels.create({
          name: 'courses',
          type: ChannelType.GuildText,
          parent: category.id,
          topic: `Courses from ${catName}`,
          permissionOverwrites: [
            {
              id: guild.roles.everyone.id,
              deny: [PermissionFlagsBits.SendMessages],
              allow: [PermissionFlagsBits.ViewChannel],
            },
            {
              id: guild.members.me.id,
              allow: [PermissionFlagsBits.SendMessages, PermissionFlagsBits.EmbedLinks],
            },
          ],
        });
        status.push(`  ✅ Created **#courses** in ${catName}`);
      }

      categoriesMap[String(community.id)] = {
        categoryId: category.id,
        postsChannelId: postsChannel.id,
        coursesChannelId: coursesChannel.id,
      };
    }

    // ─── 5. Persist config ─────────────────────────────────────────────
    setGuildConfig(guild.id, {
      feedChannelId: feedChannel.id,
      welcomeChannelId: welcomeChannel.id,
      categories: categoriesMap,
    });
    status.push(`✅ Guild config saved to \`data/guild-config.json\``);

    await interaction.editReply({
      embeds: [
        new EmbedBuilder()
          .setColor(C.success)
          .setTitle('🏗️ Server Setup Complete')
          .setDescription(status.join('\n'))
          .setTimestamp(new Date())
          .setFooter({ text: `Guild: ${guild.name}` }),
      ],
    });
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    status.push(`❌ Error: ${msg}`);
    await interaction.editReply({
      embeds: [
        new EmbedBuilder()
          .setColor(C.danger)
          .setTitle('🏗️ Server Setup Failed')
          .setDescription(status.join('\n'))
          .setTimestamp(new Date()),
      ],
    });
  }
}
