import {
  ActionRowBuilder,
  AttachmentBuilder,
  ButtonBuilder,
  ButtonStyle,
  Client,
  GatewayIntentBits,
  InteractionType
} from 'discord.js';
import { config, requireConfig } from './config.js';
import * as api from './api.js';
import {
  C,
  alertCard,
  buildPostDetailEmbed,
  card,
  formatCommunities,
  formatPostRows,
  truncate
} from './theme.js';
import { isHttpUrl, resolveCourseFileUrl } from './urls.js';

requireConfig();

const client = new Client({ intents: [GatewayIntentBits.Guilds] });

function ensureMinContent(s) {
  const base = (s || '').trim() || 'Course shared from Discord.';
  return base.length >= 50 ? base : `${base}${'·'.repeat(50 - base.length)}`;
}

client.once('clientReady', () => {
  console.log(`Logged in as ${client.user?.tag}`);
  console.log(`ms-community: ${config.msCommunityBase} (user ${config.defaultUserId})`);
});

client.on('interactionCreate', async (interaction) => {
  if (interaction.type !== InteractionType.ApplicationCommand || !interaction.isChatInputCommand()) {
    return;
  }

  const uid = config.defaultUserId;

  try {
    switch (interaction.commandName) {
      case 'communities': {
        await interaction.deferReply();
        const list = await api.listCommunities();
        if (!list?.length) {
          await interaction.editReply({
            embeds: [
              alertCard(
                interaction,
                C.warn,
                'No communities',
                `The API returned an empty list. Check **MS_COMMUNITY_URL** (\`${config.msCommunityBase}\`) and that **ms-community** is running.`
              )
            ]
          });
          return;
        }
        await interaction.editReply({
          embeds: [
            card(interaction, {
              color: C.primary,
              title: `Communities · ${list.length} total`,
              description: formatCommunities(list)
            })
          ]
        });
        break;
      }

      case 'posts':
      case 'list_posts': {
        await interaction.deferReply();
        const communityId = interaction.options.getInteger('community_id');
        const posts = await api.listPosts({
          communityId: communityId ?? undefined,
          status: 'PUBLISHED',
          voterId: uid
        });
        if (!posts?.length) {
          await interaction.editReply({
            embeds: [
              alertCard(
                interaction,
                C.warn,
                'No published posts',
                `Nothing **PUBLISHED** yet` +
                  (communityId != null ? ` in community \`#${communityId}\`` : '') +
                  `.\n\nPublish from the web app or use **/upload_course**.`
              )
            ]
          });
          return;
        }
        const subtitle =
          communityId != null ? `Community \`#${communityId}\` · ${posts.length} shown` : `Latest · ${posts.length} shown`;
        await interaction.editReply({
          embeds: [
            card(interaction, {
              color: C.primary,
              title: 'Published feed',
              description: `_${subtitle}_\n\n${formatPostRows(posts)}`
            })
          ]
        });
        break;
      }

      case 'post': {
        await interaction.deferReply();
        const postId = interaction.options.getInteger('post_id', true);
        const p = await api.getPost(postId, uid);
        await interaction.editReply({ embeds: [buildPostDetailEmbed(interaction, p)] });
        break;
      }

      case 'vote': {
        await interaction.deferReply();
        const postId = interaction.options.getInteger('post_id', true);
        const direction = interaction.options.getString('direction', true);
        await api.vote(postId, uid, direction);
        const p = await api.getPost(postId, uid);
        await interaction.editReply({
          embeds: [
            card(interaction, {
              color: C.success,
              title: 'Vote saved',
              description:
                `Post **#${postId}** · direction **${direction}**\n\n` +
                `▲ **${p.upvoteCount ?? 0}** · ▼ **${p.downvoteCount ?? 0}**\n` +
                `Linked app user vote: **${p.myVote ?? 'none'}**`
            })
          ]
        });
        break;
      }

      case 'comment': {
        await interaction.deferReply();
        const postId = interaction.options.getInteger('post_id', true);
        const message = interaction.options.getString('message', true);
        await api.addComment(postId, message, uid);
        const quoted = truncate(message, 1800)
          .split('\n')
          .map((line) => (line.trim() ? `> ${line}` : ''))
          .join('\n');
        await interaction.editReply({
          embeds: [
            card(interaction, {
              color: C.success,
              title: `Comment on post #${postId}`,
              description: quoted || '*empty*'
            })
          ]
        });
        break;
      }

      case 'upload_course': {
        await interaction.deferReply();
        const communityId = interaction.options.getInteger('community_id', true);
        const title = interaction.options.getString('title', true);
        const att = interaction.options.getAttachment('file', true);
        if (!att.name?.toLowerCase().endsWith('.pdf') && att.contentType !== 'application/pdf') {
          await interaction.editReply({
            embeds: [
              alertCard(
                interaction,
                C.danger,
                'Invalid file',
                'Attach a single **.pdf** file for the course.'
              )
            ]
          });
          return;
        }
        const fileRes = await fetch(att.url);
        if (!fileRes.ok) {
          await interaction.editReply({
            embeds: [
              alertCard(interaction, C.danger, 'Download failed', 'Could not fetch the file from Discord.')
            ]
          });
          return;
        }
        const buf = Buffer.from(await fileRes.arrayBuffer());
        const { fileUrl } = await api.uploadCoursePdf(buf, att.name || 'course.pdf');
        const draft = await api.createPost({
          title,
          content: ensureMinContent(`Uploaded from Discord: ${title}`),
          type: 'COURSE',
          mediaUrl: '',
          fileUrl,
          communityId,
          createdBy: uid,
          status: 'DRAFT'
        });
        const published = await api.publishPost(draft.id);
        await interaction.editReply({
          embeds: [
            card(interaction, {
              color: C.success,
              title: 'Course published',
              description:
                `**${truncate(title, 200)}**\n\n` +
                `Post **#${published.id}** · Community **#${communityId}**\n\n` +
                `[Open course file](${fileUrl})`
            })
          ]
        });
        break;
      }

      case 'download_course': {
        await interaction.deferReply();
        const postId = interaction.options.getInteger('post_id', true);
        const attach = interaction.options.getBoolean('attach') ?? false;
        const p = await api.getPost(postId, uid);
        if (p.type !== 'COURSE') {
          await interaction.editReply({
            embeds: [
              alertCard(interaction, C.warn, 'Not a course', `Post **#${postId}** is type \`${p.type}\`, not **COURSE**.`)
            ]
          });
          return;
        }
        if (p.status !== 'PUBLISHED') {
          await interaction.editReply({
            embeds: [
              alertCard(
                interaction,
                C.warn,
                'Not published',
                'Only **PUBLISHED** courses can be downloaded through this flow.'
              )
            ]
          });
          return;
        }
        if (!p.fileUrl) {
          await interaction.editReply({
            embeds: [alertCard(interaction, C.warn, 'No file', 'This post has no **fileUrl** set.')]
          });
          return;
        }
        if (!attach) {
          const href = resolveCourseFileUrl(p.fileUrl);
          const components = [];
          if (isHttpUrl(href)) {
            components.push(
              new ActionRowBuilder().addComponents(
                new ButtonBuilder()
                  .setLabel('Open / download PDF')
                  .setStyle(ButtonStyle.Link)
                  .setURL(href)
              )
            );
          }
          const desc =
            (isHttpUrl(href)
              ? `**Button** — most reliable in Discord (embed markdown links are picky).\nCopy link:\n\`${href}\``
              : `**Could not build a valid http(s) URL.**\nAPI value:\n\`${String(p.fileUrl)}\`\nResolved attempt:\n\`${href}\``) +
            `\n\n_If you see \`localhost\`, only machines that reach your API host can open it — set **MS_COMMUNITY_URL** in the bot’s \`.env\`._` +
            `\n_Use **attach: true** to fetch via \`/api/download\` (contribution rules)._`;
          await interaction.editReply({
            embeds: [
              card(interaction, {
                color: C.course,
                title: `Course #${postId}`,
                description: desc
              })
            ],
            components
          });
          return;
        }
        try {
          const { buffer, contentType } = await api.downloadCourseFile(postId, uid);
          const max = 24 * 1024 * 1024;
          if (buffer.length > max) {
            const hrefLarge = resolveCourseFileUrl(p.fileUrl);
            const largeLink = /^https?:\/\//i.test(hrefLarge) ? `[Use direct link](${hrefLarge})` : `\`${hrefLarge}\``;
            await interaction.editReply({
              embeds: [
                alertCard(
                  interaction,
                  C.warn,
                  'File too large',
                  `**${buffer.length}** bytes — Discord limit ~24 MB.\n\n${largeLink}`
                )
              ]
            });
            return;
          }
          const attachment = new AttachmentBuilder(buffer, {
            name: `course-${postId}.pdf`,
            description: contentType
          });
          await interaction.editReply({
            embeds: [
              card(interaction, {
                color: C.course,
                title: `Course #${postId}`,
                description: 'PDF attached below.'
              })
            ],
            files: [attachment]
          });
        } catch (e) {
          const msg = e instanceof Error ? e.message : String(e);
          const hrefErr = resolveCourseFileUrl(p.fileUrl);
          const errLink = /^https?:\/\//i.test(hrefErr) ? `[Try direct link](${hrefErr})` : `\`${hrefErr}\``;
          await interaction.editReply({
            embeds: [
              alertCard(
                interaction,
                C.danger,
                'Download failed',
                `${msg}\n\n${errLink}\n_Authors can always download; others may need a contribution share._`
              )
            ]
          });
        }
        break;
      }

      default:
        await interaction.reply({
          embeds: [
            alertCard(interaction, C.muted, 'Unknown command', 'This interaction is not handled.')
          ]
        });
    }
  } catch (err) {
    console.error(`[interaction ${interaction.commandName}]`, err);
    const msg = err instanceof Error ? err.message : String(err);
    const text = alertCard(interaction, C.danger, 'Request failed', msg);
    if (interaction.deferred) {
      await interaction.editReply({ embeds: [text] }).catch(() => {});
    } else if (interaction.replied) {
      await interaction.followUp({ embeds: [text] }).catch(() => {});
    } else {
      await interaction.reply({ embeds: [text] }).catch(() => {});
    }
  }
});

client.login(config.discordToken);
