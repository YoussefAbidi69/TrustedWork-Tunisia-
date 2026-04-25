/** Discord embed color integers */
export const C = {
  primary:   0x6366f1,  // Indigo
  success:   0x10b981,  // Emerald
  warn:      0xf59e0b,  // Amber
  danger:    0xef4444,  // Red
  muted:     0x64748b,  // Slate
  course:    0x06b6d4,  // Cyan
  digest:    0x8b5cf6,  // Violet
  trending:  0xf97316,  // Orange
};

/**
 * Map a community ID to a consistent color for embed borders.
 * Cycles through a curated palette.
 */
const PALETTE = [
  0x6366f1, 0x8b5cf6, 0x06b6d4, 0x10b981,
  0xf59e0b, 0xf97316, 0xef4444, 0xec4899,
  0x14b8a6, 0x3b82f6, 0xa855f7, 0x84cc16,
];

export function communityColor(communityId) {
  if (communityId == null) return C.primary;
  return PALETTE[Number(communityId) % PALETTE.length];
}

export function courseStatusColor(published) {
  return published ? C.success : C.warn;
}
