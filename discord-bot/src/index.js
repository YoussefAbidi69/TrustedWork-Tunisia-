import { Client, GatewayIntentBits } from 'discord.js';
import { config, requireConfig } from './config/index.js';
import * as readyEvent from './events/ready.js';
import * as interactionEvent from './events/interaction-create.js';
import * as messageCreateEvent from './events/message-create.js';
import * as guildMemberAddEvent from './events/guild-member-add.js';

// ─── Validate environment ────────────────────────────────────────────────────
requireConfig();

// ─── Create client ───────────────────────────────────────────────────────────
const client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildMessages,
    GatewayIntentBits.MessageContent,
    GatewayIntentBits.GuildMembers,
  ],
});

// ─── Register events ─────────────────────────────────────────────────────────
client.once(readyEvent.name, (...args) => readyEvent.execute(...args));
client.on(interactionEvent.name, (...args) => interactionEvent.execute(...args));
client.on(messageCreateEvent.name, (...args) => messageCreateEvent.execute(...args));
client.on(guildMemberAddEvent.name, (...args) => guildMemberAddEvent.execute(...args));

// ─── Login ───────────────────────────────────────────────────────────────────
client.login(config.discordToken);
