import { ActionRowBuilder, ButtonBuilder, ButtonStyle } from 'discord.js';

const PAGE_SIZE = 5;

/**
 * Paginate an array of items.
 * @param {any[]} items
 * @param {number} page 1-indexed
 * @param {number} [pageSize]
 * @returns {{ items: any[], totalPages: number, currentPage: number }}
 */
export function paginate(items, page = 1, pageSize = PAGE_SIZE) {
  const total = items.length;
  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  const currentPage = Math.max(1, Math.min(page, totalPages));
  const start = (currentPage - 1) * pageSize;
  return {
    items: items.slice(start, start + pageSize),
    totalPages,
    currentPage,
  };
}

/**
 * Build pagination ActionRow buttons.
 * @param {string} prefix Custom ID prefix (e.g., 'posts_page')
 * @param {number} currentPage
 * @param {number} totalPages
 * @param {string} [extra] Extra data to embed in custom ID
 */
export function paginationRow(prefix, currentPage, totalPages, extra = '') {
  const suffix = extra ? `_${extra}` : '';
  const row = new ActionRowBuilder().addComponents(
    new ButtonBuilder()
      .setCustomId(`${prefix}_prev_${currentPage}${suffix}`)
      .setLabel('◀ Previous')
      .setStyle(ButtonStyle.Secondary)
      .setDisabled(currentPage <= 1),
    new ButtonBuilder()
      .setCustomId(`${prefix}_info`)
      .setLabel(`${currentPage} / ${totalPages}`)
      .setStyle(ButtonStyle.Secondary)
      .setDisabled(true),
    new ButtonBuilder()
      .setCustomId(`${prefix}_next_${currentPage}${suffix}`)
      .setLabel('Next ▶')
      .setStyle(ButtonStyle.Secondary)
      .setDisabled(currentPage >= totalPages)
  );
  return row;
}

/**
 * Parse a pagination button custom ID.
 * @param {string} customId
 * @returns {{ prefix: string, direction: 'prev'|'next', page: number } | null}
 */
export function parsePaginationId(customId) {
  const match = customId.match(/^(.+)_(prev|next)_(\d+)/);
  if (!match) return null;
  const [, prefix, direction, pageStr] = match;
  const currentPage = parseInt(pageStr, 10);
  const newPage = direction === 'prev' ? currentPage - 1 : currentPage + 1;
  return { prefix, direction, page: newPage };
}
