import { ActionRowBuilder, AttachmentBuilder, ButtonBuilder, ButtonStyle } from 'discord.js';
import * as api from '../services/community.service.js';
import { card, alertCard } from '../embeds/common.js';
import { C } from '../utils/colors.js';
import { truncate, resolveCourseFileUrl, isHttpUrl } from '../utils/formatting.js';
import { config } from '../config/index.js';

export const name = 'download_course';

export async function execute(interaction) {
  await interaction.deferReply();
  const postId = interaction.options.getInteger('post_id', true);
  const attach = interaction.options.getBoolean('attach') ?? false;
  const uid = config.defaultUserId;
  const p = await api.getPost(postId, uid);

  if (p.status !== 'PUBLISHED') {
    return interaction.editReply({
      embeds: [
        alertCard(interaction, C.warn, 'Not published', 'Only **PUBLISHED** posts can be downloaded.'),
      ],
    });
  }

  // For the download flow we use the legacy fileUrl approach
  // since posts with type COURSE have fileUrl set
  if (!attach) {
    const href = resolveCourseFileUrl(p.fileUrl || '');
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
    const desc = isHttpUrl(href)
      ? `Click the button below to open the PDF.\nCopy link:\n\`${href}\``
      : `**Could not build a valid http(s) URL.**\nAPI value:\n\`${String(p.fileUrl)}\``;

    return interaction.editReply({
      embeds: [card(interaction, { color: C.course, title: `📥 Course #${postId}`, description: desc })],
      components,
    });
  }

  try {
    const { buffer, contentType } = await api.downloadCourseFile(postId, uid);
    const max = 24 * 1024 * 1024;
    if (buffer.length > max) {
      const hrefLarge = resolveCourseFileUrl(p.fileUrl || '');
      const largeLink = isHttpUrl(hrefLarge) ? `[Use direct link](${hrefLarge})` : `\`${hrefLarge}\``;
      return interaction.editReply({
        embeds: [
          alertCard(
            interaction,
            C.warn,
            'File too large',
            `**${buffer.length}** bytes — Discord limit ~24 MB.\n\n${largeLink}`
          ),
        ],
      });
    }

    const attachment = new AttachmentBuilder(buffer, {
      name: `course-${postId}.pdf`,
      description: contentType,
    });
    await interaction.editReply({
      embeds: [card(interaction, { color: C.course, title: `📥 Course #${postId}`, description: 'PDF attached below.' })],
      files: [attachment],
    });
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    const hrefErr = resolveCourseFileUrl(p.fileUrl || '');
    const errLink = isHttpUrl(hrefErr) ? `[Try direct link](${hrefErr})` : `\`${hrefErr}\``;
    await interaction.editReply({
      embeds: [
        alertCard(
          interaction,
          C.danger,
          'Download failed',
          `${msg}\n\n${errLink}\n_Authors can always download; others may need a contribution share._`
        ),
      ],
    });
  }
}
