# TrustedWork Community Discord Bot — Setup Guide

## Prerequisites

- **Node.js 20+** installed
- **Discord Bot** created in the [Discord Developer Portal](https://discord.com/developers/applications)
- **ms-community** backend running (Spring Boot, port 8084)

## 1. Configure Environment

Copy `.env.example` to `.env` and fill in:

```env
DISCORD_TOKEN=<your bot token>
DISCORD_CLIENT_ID=<your application ID>
DISCORD_GUILD_ID=<your test server ID>     # optional, for faster dev
MS_COMMUNITY_URL=http://localhost:8084
APP_BASE_URL=http://localhost:4200
DEFAULT_USER_ID=1
OPENAI_API_KEY=<your_openai_key_for_rag_and_moderation>
WEBHOOK_PORT=3000
WEBHOOK_SECRET=your_super_secret_webhook_token
```

## 2. Install Dependencies

```bash
npm install
```

> **New dependency**: `node-cron` was added for scheduled digests and feed sync.

## 3. Register Slash Commands

```bash
npm run register-commands
```

This registers all **14 slash commands** with Discord. If `DISCORD_GUILD_ID` is set, commands appear instantly in that server. Otherwise, global commands take up to 1 hour.

## 4. Start the Bot

```bash
npm start
```

The bot will log in, start feed sync polling (every 5 min), and schedule daily/weekly digests.

## 5. Run `/setup_server`

In your Discord server, use the `/setup_server` command (requires **Administrator** permission). This will:

1. Fetch all communities from the backend
2. Create a **#welcome** channel with an informational embed
3. Create a **#feed** channel for the global activity feed
4. For each community, create:
   - A Discord **Category** named after the community
   - **#posts** channel inside it
   - **#courses** channel inside it
5. Set permissions: bot can post, members are read-only
6. Save the channel mapping to `data/guild-config.json`

## 6. Force Sync Content

Use `/sync` (admin only) to immediately fetch all content from the backend and post it to the mapped channels. The bot automatically deduplicates — content already posted won't be re-posted.

---

## Slash Commands Reference

| Command | Description |
|---------|-------------|
| `/communities` | List all communities |
| `/posts [community_id] [page]` | Browse published posts (paginated) |
| `/list_posts [community_id] [page]` | Alias for `/posts` |
| `/post <post_id>` | Show a specific post with rich card |
| `/courses [community_id] [published_only] [page]` | Browse courses (paginated) |
| `/course <course_id>` | Show a specific course card |
| `/vote <post_id> <direction>` | Upvote or downvote a post |
| `/comment <post_id> <message>` | Add a comment to a post |
| `/upload_course <community_id> <title> <file>` | Upload a PDF course |
| `/download_course <post_id> [attach]` | Download a course PDF |
| `/search <query>` | Search posts and courses |
| `/trending` | Show top 5 trending posts |
| `/new_courses` | Show 5 latest published courses |
| `/setup_server` | Auto-create server channels (admin) |
| `/sync` | Force content sync (admin) |
| `/ask <query>` | Ask the AI assistant a question |
| `/profile [user]` | Show gamification XP profile |
| `/leaderboard` | Show top members by XP |
| `/subscribe categories` | Subscribe to notification DMs |
| `/analytics` | View community analytics (admin) |

## Webhooks (Real-Time Sync)

The bot runs an internal Express server on `WEBHOOK_PORT` (default 3000). The `ms-community` backend should send `POST` requests to `http://<bot-ip>:3000/webhook` with a JSON body:
```json
{
  "event": "post.published",
  "payload": { ...post object... }
}
```
Supported events: `post.published`, `course.published`, `verify`.

## Automation

- **Feed Sync**: Every 5 minutes (configurable via `FEED_POLL_INTERVAL_MS`), the bot polls the backend for new posts/courses and auto-posts them to the correct channels.
- **Daily Digest**: Every day at 9:00 AM, a "Top 5 Posts" embed is posted to #feed.
- **Weekly Highlights**: Every Monday at 9:00 AM, a summary of top posts + new courses is posted to #feed.

## Bot Permissions

The bot needs these Discord permissions:
- `Send Messages`
- `Embed Links`
- `Manage Channels` (for `/setup_server`)
- `Create Public Threads` (for the Discuss button)
- `Read Message History`

## Architecture

```
discord-bot/src/
├── commands/         # One file per slash command (14 total)
├── events/           # ready.js, interaction-create.js
├── services/         # community.service.js (all backend API calls)
├── embeds/           # post-card.js, course-card.js, digest.js, common.js
├── schedulers/       # feed-sync.js, digests.js (cron jobs)
├── utils/            # pagination.js, formatting.js, colors.js, rate-limiter.js
├── config/           # index.js (env config), guild-store.js (persistent storage)
└── index.js          # Entry point (~25 lines)
```
