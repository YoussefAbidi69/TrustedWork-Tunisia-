import { Component } from '@angular/core';

interface ProfileOverviewModel {
  fullName: string;
  headline: string;
  location: string;
  bio: string;
  avatar: string;
  skills: string[];
  portfolioAttached: boolean;
  certificationsAdded: boolean;
  trustPassportCompleted: boolean;
}

@Component({
  selector: 'app-profile-overview',
  templateUrl: './profile-overview.component.html',
  styleUrls: ['./profile-overview.component.css']
})
export class ProfileOverviewComponent {
  editMode = false;

  profile: ProfileOverviewModel = {
    fullName: 'Oussema Msehli',
    headline: 'Cloud Engineer & Fullstack Developer',
    location: 'Tunisia',
    bio: 'Building scalable SaaS platforms with clean architecture and cloud-native solutions.',
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80',
    skills: ['Angular', 'Spring Boot', 'Docker', 'AWS'],
    portfolioAttached: false,
    certificationsAdded: true,
    trustPassportCompleted: true
  };

  draftProfile: ProfileOverviewModel = this.cloneProfile(this.profile);

  newSkill = '';

  get completion(): number {
    const checks = [
      this.hasValue(this.profile.fullName),
      this.hasValue(this.profile.headline),
      this.hasValue(this.profile.location),
      this.hasValue(this.profile.bio),
      this.profile.skills.length > 0,
      this.profile.portfolioAttached,
      this.profile.certificationsAdded,
      this.profile.trustPassportCompleted
    ];

    const completed = checks.filter(Boolean).length;
    return Math.round((completed / checks.length) * 100);
  }

  get completionItems(): { label: string; done: boolean }[] {
    return [
      { label: 'Basic info', done: this.hasValue(this.profile.fullName) && this.hasValue(this.profile.headline) },
      { label: 'Bio', done: this.hasValue(this.profile.bio) },
      { label: 'Skills', done: this.profile.skills.length > 0 },
      { label: 'Portfolio', done: this.profile.portfolioAttached },
      { label: 'Certifications', done: this.profile.certificationsAdded },
      { label: 'Trust Passport', done: this.profile.trustPassportCompleted }
    ];
  }

  toggleEdit(): void {
    if (!this.editMode) {
      this.draftProfile = this.cloneProfile(this.profile);
      this.newSkill = '';
    }

    this.editMode = !this.editMode;
  }

  saveProfile(): void {
    const cleanedSkills = this.draftProfile.skills
      .map(skill => skill.trim())
      .filter(skill => !!skill);

    this.profile = {
      ...this.draftProfile,
      skills: [...new Set(cleanedSkills)]
    };

    this.editMode = false;
    this.newSkill = '';
  }

  cancelEdit(): void {
    this.draftProfile = this.cloneProfile(this.profile);
    this.editMode = false;
    this.newSkill = '';
  }

  addSkill(): void {
    const value = this.newSkill.trim();

    if (!value) {
      return;
    }

    const alreadyExists = this.draftProfile.skills.some(
      skill => skill.toLowerCase() === value.toLowerCase()
    );

    if (!alreadyExists) {
      this.draftProfile.skills = [...this.draftProfile.skills, value];
    }

    this.newSkill = '';
  }

  removeSkill(skillToRemove: string): void {
    this.draftProfile.skills = this.draftProfile.skills.filter(
      skill => skill !== skillToRemove
    );
  }

  onSkillInputKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.addSkill();
    }
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (!input.files || !input.files.length) {
      return;
    }

    const file = input.files[0];

    if (!file.type.startsWith('image/')) {
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      this.draftProfile.avatar = String(reader.result);
    };
    reader.readAsDataURL(file);
  }

  trackBySkill(index: number, skill: string): string {
    return `${index}-${skill}`;
  }

  private hasValue(value: string): boolean {
    return !!value && value.trim().length > 0;
  }

  private cloneProfile(profile: ProfileOverviewModel): ProfileOverviewModel {
    return {
      ...profile,
      skills: [...profile.skills]
    };
  }
}