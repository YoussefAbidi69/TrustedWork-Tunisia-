export enum PostType {
  COURSE = 'COURSE',
  INFO = 'INFO'
}

export enum PostStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  REJECTED = 'REJECTED',
  HIDDEN = 'HIDDEN'
}

export enum VoteType {
  UP = 'UP',
  DOWN = 'DOWN'
}

export enum ReportStatus {
  PENDING = 'PENDING',
  REVIEWED = 'REVIEWED'
}

export enum LessonType {
  TEXT = 'TEXT',
  VIDEO = 'VIDEO',
  PDF = 'PDF',
  QUIZ = 'QUIZ',
  CODE = 'CODE',
  IMAGE = 'IMAGE'
}

export interface Community {
  id: number;
  name: string;
  description: string;
  createdBy: number;
  joined?: boolean;
  memberCount?: number;
  postCount?: number;
}

export interface Post {
  id: number;
  title: string;
  content: string;
  type: PostType;
  mediaUrl: string;
  fileUrl: string;
  createdBy: number;
  communityId: number;
  status: PostStatus;
  isAiGenerated: boolean;
  isValidated: boolean;
  reportCount: number;
  /** Set by ms-community when listing or loading a post */
  upvoteCount?: number;
  downvoteCount?: number;
  /** Current user's vote when API was called with voterId */
  myVote?: VoteType | null;
}

export interface Comment {
  id: number;
  content: string;
  postId: number;
  userId: number;
}

export interface Vote {
  id: number | null;
  postId: number;
  userId: number;
  type: VoteType | null;
}

export interface Report {
  id: number;
  postId: number;
  reportedBy: number;
  reason: string;
  description: string;
  status: ReportStatus;
}

export interface Contribution {
  id: number;
  userId: number;
  sharedCourseCount: number;
}

export interface Course {
  id: number;
  title: string;
  description: string;
  authorId: number;
  communityId?: number | null;
  published: boolean;
}

export interface Section {
  id: number;
  courseId: number;
  title: string;
  orderIndex: number;
}

export interface Lesson {
  id: number;
  sectionId: number;
  title: string;
  content: string;
  type: LessonType;
  fileUrl?: string;
  orderIndex: number;
  parsedQuiz?: any;
  quizState?: any;
}

export interface Progress {
  id?: number | null;
  userId: number;
  lessonId: number;
  completed: boolean;
  completedAt?: string | null;
}

export interface Certificate {
  id: number;
  userId: number;
  courseId: number;
  issuedAt: string;
}
