import { PostStatus, VoteType } from '../../../core/models/community.model';

export type CommunityFeedItemType = 'POST' | 'COURSE';

export interface CommunityFeedBaseItem {
  id: number;
  type: CommunityFeedItemType;
  title: string;
  communityId: number;
  createdBy?: number;
}

export interface CommunityFeedPostItem extends CommunityFeedBaseItem {
  type: 'POST';
  content: string;
  status?: PostStatus;
  reportCount?: number;
  comments?: unknown[];
  votes?: unknown[];
  upvoteCount?: number;
  downvoteCount?: number;
  myVote?: VoteType | null;
}

export interface CommunityFeedCourseItem extends CommunityFeedBaseItem {
  type: 'COURSE';
  description: string;
  published: boolean;
  canDownload?: boolean;
}

export type CommunityFeedItem = CommunityFeedPostItem | CommunityFeedCourseItem;

export function isCommunityFeedPostItem(item: CommunityFeedItem): item is CommunityFeedPostItem {
  return item.type === 'POST';
}

export function isCommunityFeedCourseItem(item: CommunityFeedItem): item is CommunityFeedCourseItem {
  return item.type === 'COURSE';
}

export function normalizeCommunityFeed(
  rawItems: unknown[],
  fallbackCommunityId?: number | null
): CommunityFeedItem[] {
  return (rawItems || [])
    .map((raw) => normalizeCommunityFeedItem(raw, fallbackCommunityId))
    .filter((item): item is CommunityFeedItem => item != null);
}

export function normalizeCommunityFeedItem(
  rawItem: unknown,
  fallbackCommunityId?: number | null
): CommunityFeedItem | null {
  if (!rawItem || typeof rawItem !== 'object') {
    return null;
  }

  const raw = rawItem as Record<string, unknown>;
  const id = toNumber(raw['id']);
  if (id == null) {
    return null;
  }

  const type = normalizeType(raw['type']);
  const communityMeta = raw['community'] as Record<string, unknown> | undefined;
  const communityId =
    toNumber(raw['communityId']) ??
    toNumber(raw['community']) ??
    toNumber(communityMeta?.['id']) ??
    fallbackCommunityId ??
    0;
  const createdBy = toNumber(raw['createdBy']) ?? toNumber(raw['authorId']);
  const title = toString(raw['title']) || `Item #${id}`;

  if (type === 'COURSE') {
    return {
      id,
      type: 'COURSE',
      title,
      description: toString(raw['description']) || toString(raw['content']),
      published: toBoolean(raw['published']) ?? isPublishedStatus(raw['status']),
      canDownload:
        toBoolean(raw['canDownload']) ??
        toBoolean(raw['downloadable']) ??
        toBoolean(raw['canBeDownloaded']),
      communityId,
      createdBy: createdBy ?? undefined
    };
  }

  const votes = asArray(raw['votes']);

  return {
    id,
    type: 'POST',
    title,
    content: toString(raw['content']) || toString(raw['description']),
    status: normalizeStatus(raw['status']),
    reportCount: toNumber(raw['reportCount']) ?? 0,
    comments: asArray(raw['comments']),
    votes,
    upvoteCount: toNumber(raw['upvoteCount']) ?? countVotes(votes, 'UP'),
    downvoteCount: toNumber(raw['downvoteCount']) ?? countVotes(votes, 'DOWN'),
    myVote: normalizeVoteType(raw['myVote']),
    communityId,
    createdBy: createdBy ?? 0
  };
}

function normalizeType(value: unknown): CommunityFeedItemType {
  const t = typeof value === 'string' ? value.toUpperCase() : '';
  return t === 'COURSE' ? 'COURSE' : 'POST';
}

function normalizeStatus(value: unknown): PostStatus | undefined {
  const status = typeof value === 'string' ? value.toUpperCase() : '';
  if (status === PostStatus.DRAFT || status === PostStatus.PUBLISHED || status === PostStatus.HIDDEN || status === PostStatus.REJECTED) {
    return status as PostStatus;
  }
  return undefined;
}

function normalizeVoteType(value: unknown): VoteType | null {
  const vote = typeof value === 'string' ? value.toUpperCase() : '';
  if (vote === VoteType.UP || vote === VoteType.DOWN) {
    return vote as VoteType;
  }
  return null;
}

function isPublishedStatus(value: unknown): boolean {
  return typeof value === 'string' && value.toUpperCase() === PostStatus.PUBLISHED;
}

function asArray(value: unknown): unknown[] | undefined {
  return Array.isArray(value) ? value : undefined;
}

function countVotes(votes: unknown[] | undefined, wantedType: 'UP' | 'DOWN'): number {
  if (!votes || votes.length === 0) {
    return 0;
  }
  return votes.filter((vote) => {
    if (!vote || typeof vote !== 'object') {
      return false;
    }
    const value = (vote as Record<string, unknown>)['type'];
    return typeof value === 'string' && value.toUpperCase() === wantedType;
  }).length;
}

function toString(value: unknown): string {
  return typeof value === 'string' ? value : '';
}

function toBoolean(value: unknown): boolean | undefined {
  return typeof value === 'boolean' ? value : undefined;
}

function toNumber(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === 'string' && value.trim().length > 0) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}