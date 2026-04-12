import { Component } from '@angular/core';

interface RecruitmentMetric {
  label: string;
  value: string;
  helper: string;
}

interface RecruitmentFocus {
  title: string;
  text: string;
}

interface PipelineItem {
  stage: string;
  count: string;
  tone: string;
}

@Component({
  selector: 'app-recruitment-overview',
  templateUrl: './recruitment-overview.component.html',
  styleUrls: ['./recruitment-overview.component.css']
})
export class RecruitmentOverviewComponent {
  metrics: RecruitmentMetric[] = [
    {
      label: 'Open recruitment tracks',
      value: '14',
      helper: 'Long-term roles aligned with your skill profile'
    },
    {
      label: 'Shortlist potential',
      value: '89%',
      helper: 'Strong fit based on trust, skills and recent activity'
    },
    {
      label: 'Recruiter interest',
      value: '+23%',
      helper: 'Weekly increase in relevant profile views'
    }
  ];

  focusAreas: RecruitmentFocus[] = [
    {
      title: 'Product companies',
      text: 'Stable hiring demand for Angular, Spring Boot and SaaS product delivery.'
    },
    {
      title: 'Verified recruiters',
      text: 'Higher-signal opportunities with structured evaluation and stronger trust criteria.'
    },
    {
      title: 'Career transition ready',
      text: 'Designed for freelancers seeking strategic long-term positions.'
    }
  ];

  pipeline: PipelineItem[] = [
    { stage: 'Profile matched', count: '26', tone: 'tone-blue' },
    { stage: 'Shortlist ready', count: '08', tone: 'tone-green' },
    { stage: 'Interview likely', count: '03', tone: 'tone-orange' }
  ];
}