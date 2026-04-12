import { Component } from '@angular/core';

interface Skill {
  id: string;
  name: string;
  category: string;
  level: number;
  experience: string;
  projects: number;
  trending?: boolean;
}

interface SkillCategory {
  name: string;
  count: number;
}

@Component({
  selector: 'app-skills',
  templateUrl: './skills.component.html',
  styleUrls: ['./skills.component.css']
})
export class SkillsComponent {

  activeSkillId = 'skill-1';

  stats = [
    { label: 'Total skills', value: '18' },
    { label: 'Top expertise', value: 'UI/UX' },
    { label: 'Projects delivered', value: '42' },
    { label: 'Profile strength', value: '92%' }
  ];

  categories: SkillCategory[] = [
    { name: 'Design', count: 6 },
    { name: 'Frontend', count: 5 },
    { name: 'Backend', count: 3 },
    { name: 'Soft Skills', count: 4 }
  ];

  skills: Skill[] = [
    {
      id: 'skill-1',
      name: 'UI/UX Design',
      category: 'Design',
      level: 95,
      experience: '4 years',
      projects: 22,
      trending: true
    },
    {
      id: 'skill-2',
      name: 'Angular',
      category: 'Frontend',
      level: 90,
      experience: '3 years',
      projects: 18
    },
    {
      id: 'skill-3',
      name: 'Figma',
      category: 'Design',
      level: 92,
      experience: '4 years',
      projects: 25
    },
    {
      id: 'skill-4',
      name: 'Spring Boot',
      category: 'Backend',
      level: 85,
      experience: '3 years',
      projects: 14
    },
    {
      id: 'skill-5',
      name: 'Communication',
      category: 'Soft Skills',
      level: 88,
      experience: '5 years',
      projects: 40
    }
  ];

  get activeSkill(): Skill {
    return this.skills.find(s => s.id === this.activeSkillId)!;
  }

  selectSkill(id: string): void {
    this.activeSkillId = id;
  }
}