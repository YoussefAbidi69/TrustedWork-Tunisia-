import { Component, OnInit } from '@angular/core';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { CommunityService } from '../../../core/services/community.service';
import { Community } from '../../../core/models/community.model';
import { CommunityAiService } from '../services/community-ai.service';
import { CourseQualityService, QualityPrediction } from '../../../core/services/course-quality.service';
import { CourseBlockType, CourseBuilderService, CourseCreatePayload } from '../services/course-builder.service';

interface QuizOptionDraft {
  id: number;
  text: string;
  correct: boolean;
}

interface CourseBlockDraft {
  id: number;
  title: string;
  type: CourseBlockType;
  content: string;
  fileUrl: string;
  orderIndex: number;
  /** Local-only quiz state, serialized into content for QUIZ blocks. */
  quizQuestion?: string;
  quizOptions?: QuizOptionDraft[];
}

interface CourseSectionDraft {
  id: number;
  title: string;
  orderIndex: number;
  blocks: CourseBlockDraft[];
}

type AiSuggestionType = 'SECTION_TITLE' | 'BLOCK_CONTENT' | 'BLOCK_SUMMARY' | 'BLOCK_REWRITE';
type QualityLevel = 'positive' | 'neutral' | 'negative';

@Component({
  selector: 'app-course-create',
  templateUrl: './course-create.component.html',
  styleUrls: ['./course-create.component.css']
})
export class CourseCreateComponent implements OnInit {
  title = '';
  description = '';
  communityId: number | null = null;
  communities: Community[] = [];

  sections: CourseSectionDraft[] = [];
  selectedSectionId: number | null = null;
  selectedBlockId: number | null = null;

  saving = false;
  saveError = '';

  aiBusy = false;
  aiError = '';
  aiSuggestion = '';
  aiSuggestionType: AiSuggestionType | null = null;

  qualityLoading = false;
  qualityPrediction: QualityPrediction | null = null;
  qualityUnavailable = false;
  qualityValidationMessage = '';
  private qualityRequestVersion = 0;
  publishReviewVisible = false;
  publishReviewLoading = false;

  private idCounter = 0;

  readonly blockTypes: CourseBlockType[] = ['TEXT', 'CODE', 'VIDEO', 'PDF', 'IMAGE', 'QUIZ'];

  constructor(
    public route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private communityService: CommunityService,
    private aiService: CommunityAiService,
    private courseQualityService: CourseQualityService,
    private courseBuilderService: CourseBuilderService
  ) {}

  ngOnInit(): void {
    const queryCommunity = this.route.snapshot.queryParamMap.get('communityId');
    const parsedCommunity = queryCommunity ? Number(queryCommunity) : NaN;
    if (Number.isFinite(parsedCommunity)) {
      this.communityId = parsedCommunity;
    }

    this.communityService.getAll().subscribe({
      next: (items) => {
        this.communities = items || [];
        if (this.communityId == null && this.communities.length > 0) {
          this.communityId = this.communities[0].id;
        }
      },
      error: () => {
        this.communities = [];
      }
    });

    if (this.sections.length === 0) {
      this.addSection();
    }

  }
  get selectedSection(): CourseSectionDraft | null {
    if (this.selectedSectionId == null) {
      return null;
    }
    return this.sections.find((section) => section.id === this.selectedSectionId) ?? null;
  }

  get selectedBlock(): CourseBlockDraft | null {
    const section = this.selectedSection;
    if (!section || this.selectedBlockId == null) {
      return null;
    }
    return section.blocks.find((block) => block.id === this.selectedBlockId) ?? null;
  }

  get qualityScore(): number {
    return this.qualityPrediction?.score ?? 0;
  }

  get qualityTone(): QualityLevel {
    if (this.qualityScore >= 65) {
      return 'positive';
    }
    if (this.qualityScore >= 35) {
      return 'neutral';
    }
    return 'negative';
  }

  get totalBlockCount(): number {
    return this.sections.reduce((count, section) => count + section.blocks.length, 0);
  }

  get descriptionWordCount(): number {
    return this.wordCount(this.description);
  }

  addSection(): void {
    const section: CourseSectionDraft = {
      id: this.nextId(),
      title: `Section ${this.sections.length + 1}`,
      orderIndex: this.sections.length,
      blocks: [this.createDefaultBlock()]
    };
    this.sections.push(section);
    this.reindexSections();
    this.selectSection(section.id);
    this.selectBlock(section.blocks[0].id);
  }

  removeSection(sectionId: number): void {
    const index = this.sections.findIndex((section) => section.id === sectionId);
    if (index < 0) {
      return;
    }
    this.sections.splice(index, 1);
    if (this.sections.length === 0) {
      this.addSection();
      return;
    }
    this.reindexSections();
    this.selectSection(this.sections[0].id);
    this.selectBlock(this.sections[0].blocks[0]?.id ?? null);
  }

  addBlock(sectionId: number): void {
    const section = this.sections.find((item) => item.id === sectionId);
    if (!section) {
      return;
    }
    const block: CourseBlockDraft = {
      ...this.createDefaultBlock(),
      title: `Block ${section.blocks.length + 1}`
    };
    section.blocks.push(block);
    this.reindexBlocks(section);
    this.selectSection(section.id);
    this.selectBlock(block.id);
  }

  removeBlock(sectionId: number, blockId: number): void {
    const section = this.sections.find((item) => item.id === sectionId);
    if (!section) {
      return;
    }
    const index = section.blocks.findIndex((block) => block.id === blockId);
    if (index < 0) {
      return;
    }
    section.blocks.splice(index, 1);
    if (section.blocks.length === 0) {
      section.blocks.push(this.createDefaultBlock());
    }
    this.reindexBlocks(section);
    this.selectSection(section.id);
    this.selectBlock(section.blocks[0].id);
  }

  selectSection(sectionId: number): void {
    this.selectedSectionId = sectionId;
    const section = this.sections.find((item) => item.id === sectionId);
    if (!section || section.blocks.length === 0) {
      this.selectedBlockId = null;
      return;
    }
    const blockStillExists = section.blocks.some((block) => block.id === this.selectedBlockId);
    if (!blockStillExists) {
      this.selectedBlockId = section.blocks[0].id;
    }
  }

  selectBlock(blockId: number | null): void {
    this.selectedBlockId = blockId;
  }

  dropSection(event: CdkDragDrop<CourseSectionDraft[]>): void {
    if (event.previousIndex === event.currentIndex) {
      return;
    }
    moveItemInArray(this.sections, event.previousIndex, event.currentIndex);
    this.reindexSections();
  }

  dropBlock(section: CourseSectionDraft, event: CdkDragDrop<CourseBlockDraft[]>): void {
    if (event.previousContainer !== event.container) {
      return;
    }
    if (event.previousIndex === event.currentIndex) {
      return;
    }
    moveItemInArray(section.blocks, event.previousIndex, event.currentIndex);
    this.reindexBlocks(section);
  }

  async onSuggestSection(): Promise<void> {
    if (!this.title.trim()) {
      this.aiError = 'Set a course title before asking AI for section suggestions.';
      return;
    }
    this.aiBusy = true;
    this.aiError = '';
    this.aiSuggestion = '';
    this.aiSuggestionType = null;
    this.aiService.generateCourseOutline(this.title.trim(), 'intermediate').subscribe({
      next: (outline) => {
        this.aiBusy = false;
        const suggestion = (outline.sections?.[0]?.title || '').trim();
        if (!suggestion) {
          this.aiError = 'AI did not return a section title suggestion.';
          return;
        }
        this.aiSuggestion = suggestion;
        this.aiSuggestionType = 'SECTION_TITLE';
      },
      error: () => {
        this.aiBusy = false;
        this.aiError = 'Could not get AI section suggestion.';
      }
    });
  }

  onSuggestBlock(): void {
    const section = this.selectedSection;
    const block = this.selectedBlock;
    if (!section || !block) {
      this.aiError = 'Select a block first.';
      return;
    }
    this.aiBusy = true;
    this.aiError = '';
    this.aiSuggestion = '';
    this.aiSuggestionType = null;

    const prompt = `Generate concise ${block.type} learning content for section "${section.title}" in course "${this.title || 'Untitled course'}".`;
    this.aiService.tutorAnswer(`${this.description}\n${block.content}`, prompt).subscribe({
      next: (answer) => {
        this.aiBusy = false;
        const suggestion = (answer || '').trim();
        if (!suggestion) {
          this.aiError = 'AI did not return block content.';
          return;
        }
        this.aiSuggestion = suggestion;
        this.aiSuggestionType = 'BLOCK_CONTENT';
      },
      error: () => {
        this.aiBusy = false;
        this.aiError = 'Could not get AI block suggestion.';
      }
    });
  }

  onSummarizeBlock(): void {
    const block = this.selectedBlock;
    if (!block || !block.content.trim()) {
      this.aiError = 'Select a block with content first.';
      return;
    }
    this.aiBusy = true;
    this.aiError = '';
    this.aiSuggestion = '';
    this.aiSuggestionType = null;
    this.aiService.summarizeLesson(block.content).subscribe({
      next: (summary) => {
        this.aiBusy = false;
        this.aiSuggestion = (summary || '').trim();
        this.aiSuggestionType = 'BLOCK_SUMMARY';
      },
      error: () => {
        this.aiBusy = false;
        this.aiError = 'Could not summarize block content.';
      }
    });
  }

  onRewriteBlock(): void {
    const block = this.selectedBlock;
    if (!block || !block.content.trim()) {
      this.aiError = 'Select a block with content first.';
      return;
    }
    this.aiBusy = true;
    this.aiError = '';
    this.aiSuggestion = '';
    this.aiSuggestionType = null;
    this.aiService
      .tutorAnswer(block.content, 'Rewrite this learning content to be clearer and more structured.')
      .subscribe({
        next: (rewritten) => {
          this.aiBusy = false;
          this.aiSuggestion = (rewritten || '').trim();
          this.aiSuggestionType = 'BLOCK_REWRITE';
        },
        error: () => {
          this.aiBusy = false;
          this.aiError = 'Could not rewrite block content.';
        }
      });
  }

  applyAiSuggestion(): void {
    if (!this.aiSuggestion || !this.aiSuggestionType) {
      return;
    }
    if (this.aiSuggestionType === 'SECTION_TITLE') {
      let targetSection = this.selectedSection;
      if (!targetSection) {
        this.addSection();
        targetSection = this.selectedSection;
      }
      if (targetSection) {
        targetSection.title = this.aiSuggestion;
      }
      return;
    }

    if (this.selectedBlock) {
      this.selectedBlock.content = this.aiSuggestion;
    }
  }

  clearAiSuggestion(): void {
    this.aiSuggestion = '';
    this.aiSuggestionType = null;
    this.aiError = '';
  }

  async saveCourse(isPublish: boolean): Promise<void> {
    this.saveError = '';
    const uid = this.authService.getCurrentAuthUser()?.userId;
    if (uid == null) {
      this.saveError = 'You must be signed in.';
      return;
    }
    if (!this.title.trim()) {
      this.saveError = 'Course title is required.';
      return;
    }
    if (this.communityId == null) {
      this.saveError = 'Please choose a community.';
      return;
    }

    this.reindexSections();
    this.sections.forEach((section) => this.reindexBlocks(section));

    this.saving = true;
    try {
      const payload: CourseCreatePayload = {
        title: this.title.trim(),
        description: this.description.trim(),
        communityId: this.communityId,
        published: isPublish,
        authorId: uid
      };

      const createdCourse = await firstValueFrom(this.courseBuilderService.createCourse(payload));

      for (const section of this.sections) {
        const createdSection = await firstValueFrom(
          this.courseBuilderService.createSection(createdCourse.id, {
            title: section.title.trim() || 'Section',
            orderIndex: section.orderIndex
          })
        );

        for (const block of section.blocks) {
          await firstValueFrom(
            this.courseBuilderService.createBlock(createdSection.id, {
              title: block.title.trim() || 'Block',
              type: block.type,
              content: block.content,
              fileUrl: block.fileUrl?.trim() || '',
              orderIndex: block.orderIndex
            })
          );
        }
      }

      await this.router.navigate(['/community', 'course', String(createdCourse.id)]);
    } catch {
      this.saveError = 'Could not save the course structure. Please check backend endpoints.';
    } finally {
      this.saving = false;
    }
  }

  cancel(): void {
    void this.router.navigate(['/community']);
  }

  async openPublishReview(): Promise<void> {
    this.publishReviewVisible = true;
    this.publishReviewLoading = false;

    const title = this.title.trim();
    const description = this.description.trim();
    if (this.wordCount(title) < 3 || this.wordCount(description) < 5) {
      this.qualityValidationMessage =
        this.wordCount(title) < 3 ? 'Please enter a title first' : 'Please enter a description first';
      this.qualityPrediction = null;
      this.qualityUnavailable = false;
      return;
    }

    this.publishReviewLoading = true;
    this.qualityLoading = true;
    this.qualityUnavailable = false;
    this.qualityPrediction = null;
    const requestVersion = ++this.qualityRequestVersion;

    try {
      const prediction = await firstValueFrom(this.courseQualityService.predictCourseQuality(title, description));
      if (requestVersion !== this.qualityRequestVersion) {
        return;
      }
      if (prediction.available === false || prediction.score == null) {
        this.qualityUnavailable = true;
        this.qualityPrediction = null;
      } else {
        this.qualityUnavailable = false;
        this.qualityPrediction = prediction;
      }
    } catch {
      if (requestVersion !== this.qualityRequestVersion) {
        return;
      }
      this.qualityUnavailable = true;
      this.qualityPrediction = null;
    } finally {
      if (requestVersion === this.qualityRequestVersion) {
        this.publishReviewLoading = false;
        this.qualityLoading = false;
      }
    }
  }

  closePublishReview(): void {
    this.publishReviewVisible = false;
  }

  async confirmPublishFromReview(): Promise<void> {
    await this.saveCourse(true);
  }

  onTitleChanged(value: string): void {
    this.title = value ?? '';
    this.resetQualityAnalysisState();
  }

  onDescriptionChanged(value: string): void {
    this.description = value ?? '';
    this.resetQualityAnalysisState();
  }

  onAnalyseQuality(): void {
    this.qualityValidationMessage = '';
    const title = this.title.trim();
    const description = this.description.trim();

    if (this.wordCount(title) < 3) {
      this.qualityValidationMessage = 'Please enter a title first';
      return;
    }
    if (this.wordCount(description) < 5) {
      this.qualityValidationMessage = 'Please enter a description first';
      return;
    }

    this.qualityLoading = true;
    this.qualityPrediction = null;
    this.qualityUnavailable = false;
    const requestVersion = ++this.qualityRequestVersion;

    this.courseQualityService.predictCourseQuality(title, description).subscribe({
      next: (prediction) => {
        if (requestVersion !== this.qualityRequestVersion) {
          return;
        }
        this.qualityLoading = false;
        if (prediction.available === false || prediction.score == null) {
          this.qualityUnavailable = true;
          this.qualityPrediction = null;
          return;
        }
        this.qualityPrediction = prediction;
        this.qualityUnavailable = false;
      },
      error: () => {
        if (requestVersion !== this.qualityRequestVersion) {
          return;
        }
        this.qualityLoading = false;
        this.qualityUnavailable = true;
        this.qualityPrediction = null;
      }
    });
  }

  private wordCount(text: string): number {
    const value = text.trim();
    return value ? value.split(/\s+/).filter(Boolean).length : 0;
  }

  private resetQualityAnalysisState(): void {
    this.qualityRequestVersion += 1;
    this.qualityLoading = false;
    this.publishReviewLoading = false;
    this.publishReviewVisible = false;
    this.qualityValidationMessage = '';
    if (this.qualityPrediction || this.qualityUnavailable) {
      this.qualityPrediction = null;
      this.qualityUnavailable = false;
    }
  }

  trackBySectionId(_index: number, section: CourseSectionDraft): number {
    return section.id;
  }

  trackByBlockId(_index: number, block: CourseBlockDraft): number {
    return block.id;
  }

  private createDefaultBlock(): CourseBlockDraft {
    return {
      id: this.nextId(),
      title: 'New block',
      type: 'TEXT',
      content: '',
      fileUrl: '',
      orderIndex: 0
    };
  }

  onBlockTypeChange(block: CourseBlockDraft): void {
    if (block.type === 'QUIZ') {
      this.ensureQuizState(block);
      this.syncQuizToContent(block);
    }
  }

  onQuizQuestionChange(block: CourseBlockDraft, value: string): void {
    block.quizQuestion = value;
    this.syncQuizToContent(block);
  }

  onQuizOptionTextChange(block: CourseBlockDraft, option: QuizOptionDraft, value: string): void {
    option.text = value;
    this.syncQuizToContent(block);
  }

  onQuizOptionToggleCorrect(block: CourseBlockDraft, option: QuizOptionDraft): void {
    this.ensureQuizState(block);
    if (!block.quizOptions) {
      return;
    }
    // Single-correct UX: behave like radio buttons
    block.quizOptions.forEach((opt) => {
      opt.correct = opt.id === option.id;
    });
    this.syncQuizToContent(block);
  }

  addQuizOption(block: CourseBlockDraft): void {
    this.ensureQuizState(block);
    if (!block.quizOptions) {
      block.quizOptions = [];
    }
    const option: QuizOptionDraft = {
      id: this.nextId(),
      text: 'New option',
      correct: block.quizOptions.length === 0
    };
    block.quizOptions.push(option);
    this.syncQuizToContent(block);
  }

  removeQuizOption(block: CourseBlockDraft, optionId: number): void {
    if (!block.quizOptions) {
      return;
    }
    block.quizOptions = block.quizOptions.filter((opt) => opt.id !== optionId);
    if (block.quizOptions.length === 0) {
      // Keep at least one empty option for UX clarity
      block.quizOptions.push({
        id: this.nextId(),
        text: '',
        correct: true
      });
    } else if (!block.quizOptions.some((opt) => opt.correct)) {
      // Ensure at least one option is marked correct
      block.quizOptions[0].correct = true;
    }
    this.syncQuizToContent(block);
  }

  private reindexSections(): void {
    this.sections.forEach((section, index) => {
      section.orderIndex = index;
    });
  }

  private reindexBlocks(section: CourseSectionDraft): void {
    section.blocks.forEach((block, index) => {
      block.orderIndex = index;
    });
  }

  private nextId(): number {
    this.idCounter += 1;
    return this.idCounter;
  }

  private ensureQuizState(block: CourseBlockDraft): void {
    if (block.quizQuestion != null && block.quizOptions != null) {
      return;
    }
    // Try to parse existing JSON content first
    if (block.content && block.content.trim().startsWith('{')) {
      try {
        const parsed = JSON.parse(block.content);
        if (parsed && typeof parsed === 'object') {
          const question = String(parsed.question ?? '').trim();
          const rawOptions: Array<{ text?: string; correct?: boolean }> = Array.isArray(parsed.options)
            ? parsed.options
            : [];
          const quizOptions: QuizOptionDraft[] = rawOptions.map((opt, index) => ({
            id: this.nextId(),
            text: String(opt.text ?? `Option ${index + 1}`),
            correct: Boolean(opt.correct)
          }));
          block.quizQuestion = question || block.title || 'Question';
          block.quizOptions = quizOptions.length
            ? quizOptions
            : [
                {
                  id: this.nextId(),
                  text: 'Option 1',
                  correct: true
                }
              ];
          return;
        }
      } catch {
        // Fall back to defaults if JSON parsing fails
      }
    }

    // Initialize from plain content (treat as question) or defaults
    block.quizQuestion = block.content.trim() || block.title || 'Question';
    block.quizOptions = [
      {
        id: this.nextId(),
        text: 'Option 1',
        correct: true
      },
      {
        id: this.nextId(),
        text: 'Option 2',
        correct: false
      }
    ];
  }

  private syncQuizToContent(block: CourseBlockDraft): void {
    if (block.type !== 'QUIZ') {
      return;
    }
    this.ensureQuizState(block);
    const payload = {
      question: (block.quizQuestion || '').trim(),
      options: (block.quizOptions || []).map((opt) => ({
        text: opt.text,
        correct: opt.correct
      }))
    };
    block.content = JSON.stringify(payload);
  }
}
