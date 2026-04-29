import * as api from '../services/community.service.js';
import { card, alertCard } from '../embeds/common.js';
import { C } from '../utils/colors.js';
import { truncate } from '../utils/formatting.js';
import { config } from '../config/index.js';

export const name = 'upload_course';

function ensureMinContent(s) {
  const base = (s || '').trim() || 'Course shared from Discord.';
  return base.length >= 50 ? base : `${base}${'·'.repeat(50 - base.length)}`;
}

export async function execute(interaction) {
  await interaction.deferReply();
  const communityId = interaction.options.getInteger('community_id', true);
  const title = interaction.options.getString('title', true);
  const att = interaction.options.getAttachment('file', true);
  const uid = config.defaultUserId;

  if (!att.name?.toLowerCase().endsWith('.pdf') && att.contentType !== 'application/pdf') {
    return interaction.editReply({
      embeds: [alertCard(interaction, C.danger, 'Invalid file', 'Attach a single **.pdf** file for the course.')],
    });
  }

  const fileRes = await fetch(att.url);
  if (!fileRes.ok) {
    return interaction.editReply({
      embeds: [alertCard(interaction, C.danger, 'Download failed', 'Could not fetch the file from Discord.')],
    });
  }

  const buf = Buffer.from(await fileRes.arrayBuffer());
  const { fileUrl } = await api.uploadCoursePdf(buf, att.name || 'course.pdf');
  const draft = await api.createPost({
    title,
    content: ensureMinContent(`Uploaded from Discord: ${title}`),
    communityId,
    createdBy: uid,
    status: 'DRAFT',
  });
  const published = await api.publishPost(draft.id);

  await interaction.editReply({
    embeds: [
      card(interaction, {
        color: C.success,
        title: '✅ Course published',
        description:
          `**${truncate(title, 200)}**\n\n` +
          `Post **#${published.id}** · Community **#${communityId}**\n\n` +
          `[Open course file](${fileUrl})`,
      }),
    ],
  });
}
