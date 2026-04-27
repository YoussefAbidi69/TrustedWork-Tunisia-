import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SkillStoreService {
  private readonly KEY = 'jb_skills';

  getSkills(): string[] {
    const raw = localStorage.getItem(this.KEY);
    if (!raw) {
      return [];
    }
    try {
      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed)) {
        return [];
      }
      return parsed
        .map((s) => String(s ?? '').trim())
        .filter(Boolean)
        .filter((v, i, arr) => arr.findIndex((x) => x.toLowerCase() === v.toLowerCase()) === i);
    } catch {
      return [];
    }
  }

  addSkill(skill: string): string[] {
    const v = skill.trim();
    if (!v) {
      return this.getSkills();
    }
    const next = [...this.getSkills(), v];
    const unique = next.filter(
      (x, i, arr) => arr.findIndex((y) => y.toLowerCase() === x.toLowerCase()) === i
    );
    localStorage.setItem(this.KEY, JSON.stringify(unique));
    return unique;
  }

  removeSkill(skill: string): string[] {
    const v = skill.trim().toLowerCase();
    const next = this.getSkills().filter((s) => s.toLowerCase() !== v);
    localStorage.setItem(this.KEY, JSON.stringify(next));
    return next;
  }

  setSkills(skills: string[]): string[] {
    const cleaned = (skills ?? [])
      .map((s) => String(s ?? '').trim())
      .filter(Boolean)
      .filter((v, i, arr) => arr.findIndex((x) => x.toLowerCase() === v.toLowerCase()) === i);
    localStorage.setItem(this.KEY, JSON.stringify(cleaned));
    return cleaned;
  }
}

