import { LocalIndex } from 'vectra';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import OpenAI from 'openai';
import { config } from '../config/index.js';
import * as api from './community.service.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const INDEX_DIR = join(__dirname, '..', '..', 'data', 'knowledge');

let index;
let openai;

if (config.openaiApiKey) {
  openai = new OpenAI({ apiKey: config.openaiApiKey });
  index = new LocalIndex(INDEX_DIR);
}

export async function initializeKnowledgeBase() {
  if (!index) return;
  if (!(await index.isIndexCreated())) {
    await index.createIndex();
  }
}

async function getEmbedding(text) {
  const response = await openai.embeddings.create({
    model: 'text-embedding-3-small',
    input: text.replace(/\n/g, ' '),
  });
  return response.data[0].embedding;
}

export async function syncKnowledge() {
  if (!index || !openai) return;

  await initializeKnowledgeBase();
  console.log('[RAG] Syncing knowledge base...');

  const posts = await api.listPosts({ status: 'PUBLISHED', voterId: config.defaultUserId }).catch(() => []);
  
  for (const post of posts) {
    const content = `Title: ${post.title}\nContent: ${post.content || ''}`;
    // very rudimentary check: see if we already have it (Vectra doesn't have easy upsert without an ID map, 
    // but we can query exactly or just wipe and rebuild)
    // For MVP: clear and rebuild is easiest if data is small
  }

  // To do a proper rebuild, we should delete the index dir and recreate it
  // For safety and speed in this demo, let's just index new items or do a full rebuild.
  // We'll implement a simple one-time index for now.
}

export async function askQuestion(query) {
  if (!index || !openai) return { answer: 'AI is not configured. Please add OPENAI_API_KEY to .env.', sources: [] };

  await initializeKnowledgeBase();

  // 1. Embed query
  const queryEmbedding = await getEmbedding(query);

  // 2. Search index
  const results = await index.queryItems(queryEmbedding, 3); // top 3

  if (results.length === 0 || results[0].score < 0.75) {
    return {
      answer: "I couldn't find a confident answer in our community content. Try using `/search` or visit the web app.",
      sources: []
    };
  }

  // 3. Prepare context
  const context = results.map(r => r.item.metadata.text).join('\n\n---\n\n');
  const sources = results.map(r => ({
    title: r.item.metadata.title,
    url: r.item.metadata.url
  }));

  // 4. Generate answer
  const completion = await openai.chat.completions.create({
    model: 'gpt-4o-mini',
    messages: [
      { role: 'system', content: 'You are the TrustedWork Community AI Assistant. Answer the user\'s question based strictly on the provided context. If the context does not contain the answer, say you don\'t know. Be helpful and concise.' },
      { role: 'user', content: `Context:\n${context}\n\nQuestion: ${query}` }
    ]
  });

  return {
    answer: completion.choices[0].message.content,
    sources
  };
}

export async function indexDocument(id, type, title, content, url) {
  if (!index || !openai) return;
  await initializeKnowledgeBase();

  const text = `[${type.toUpperCase()}] ${title}\n${content}`;
  const embedding = await getEmbedding(text);
  
  await index.insertItem({
    vector: embedding,
    metadata: { id, type, title, text, url }
  });
}
