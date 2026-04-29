import { EmbedBuilder } from 'discord.js';
import { C } from '../utils/colors.js';

/**
 * Bot avatar URL helper.
 */
function botIcon(client) {
  return client.user?.displayAvatarURL({ size: 128 }) ?? undefined;
}

/**
 * User avatar URL helper.
 */
function userIcon(user) {
  return user.displayAvatarURL({ size: 64 });
}

/**
 * Base card embed — used by everything.
 * @param {import('discord.js').BaseInteraction | null} interaction
 * @param {import('discord.js').Client} client
 * @param {{ color?: number, title: string, description?: string, url?: string, thumbnail?: string }} opts
 */
export function card(interaction, opts, client) {
  const c = client ?? interaction?.client;
  const e = new EmbedBuilder()
    .setColor(opts.color ?? C.primary)
    .setAuthor({
      name: 'TrustedWork · Community',
      iconURL: c ? botIcon(c) : undefined,
    })
    .setTitle(opts.title)
    .setTimestamp(new Date());

  if (interaction?.user) {
    e.setFooter({
      text: interaction.user.tag,
      iconURL: userIcon(interaction.user),
    });
  }

  if (opts.description != null) e.setDescription(opts.description);
  if (opts.url) e.setURL(opts.url);
  if (opts.thumbnail) e.setThumbnail(opts.thumbnail);
  return e;
}

/**
 * Alert card — color-coded message.
 */
export function alertCard(interaction, color, title, body) {
  return card(interaction, { color, title, description: body });
}

/**
 * Server-sent card (no interaction context, for automated posts).
 */
export function serverCard(client, opts) {
  const e = new EmbedBuilder()
    .setColor(opts.color ?? C.primary)
    .setAuthor({
      name: 'TrustedWork · Community',
      iconURL: botIcon(client),
    })
    .setTitle(opts.title)
    .setTimestamp(new Date());

  if (opts.footer) e.setFooter({ text: opts.footer });
  if (opts.description != null) e.setDescription(opts.description);
  if (opts.url) e.setURL(opts.url);
  if (opts.thumbnail) e.setThumbnail(opts.thumbnail);
  return e;
}
