import { config } from '../config/index.js';
import { startFeedSync } from '../schedulers/feed-sync.js';
import { startDigests } from '../schedulers/digests.js';
import { startWebhookServer } from '../server.js';

export const name = 'ready';
export const once = true;

/**
 * @param {import('discord.js').Client} client
 */
export function execute(client) {
  console.log(`✅ Logged in as ${client.user?.tag}`);
  console.log(`   ms-community: ${config.msCommunityBase} (user ${config.defaultUserId})`);
  console.log(`   App URL: ${config.appBaseUrl}`);

  // Start automated schedulers
  // Start automated schedulers and webhook receiver
  startFeedSync(client);
  startDigests(client);
  startWebhookServer(client);

  console.log(`   ⏱️  Feed sync, digests, and webhook schedulers started`);
}
