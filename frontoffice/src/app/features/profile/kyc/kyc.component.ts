import { Component } from '@angular/core';

interface KycStat {
  label: string;
  value: string;
  tone?: 'default' | 'accent' | 'success' | 'warning';
}

interface VerificationStep {
  id: string;
  title: string;
  description: string;
  status: 'Completed' | 'In Review' | 'Pending';
  progress: number;
}

interface RequiredDocument {
  id: string;
  title: string;
  description: string;
  fileHint: string;
  status: 'Uploaded' | 'Missing' | 'Under Review';
}

interface ComplianceItem {
  label: string;
  description: string;
  checked: boolean;
}

@Component({
  selector: 'app-kyc',
  templateUrl: './kyc.component.html',
  styleUrls: ['./kyc.component.css']
})
export class KycComponent {
  readonly stats: KycStat[] = [
    { label: 'Verification status', value: '82%', tone: 'accent' },
    { label: 'Trust boost', value: '+24%', tone: 'success' },
    { label: 'Documents uploaded', value: '3/4', tone: 'default' },
    { label: 'Review phase', value: 'In progress', tone: 'warning' }
  ];

  readonly steps: VerificationStep[] = [
    {
      id: 'step-1',
      title: 'Identity verification',
      description: 'Official identity document submitted and validated.',
      status: 'Completed',
      progress: 100
    },
    {
      id: 'step-2',
      title: 'Address confirmation',
      description: 'Proof of residence uploaded for compliance review.',
      status: 'In Review',
      progress: 76
    },
    {
      id: 'step-3',
      title: 'Professional validation',
      description: 'Profile consistency and activity legitimacy assessment.',
      status: 'In Review',
      progress: 64
    },
    {
      id: 'step-4',
      title: 'Trust passport activation',
      description: 'Final activation of premium verification markers.',
      status: 'Pending',
      progress: 18
    }
  ];

  readonly documents: RequiredDocument[] = [
    {
      id: 'doc-1',
      title: 'National ID / Passport',
      description: 'Front and back or official passport identification page.',
      fileHint: 'PDF, JPG or PNG • Max 10MB',
      status: 'Uploaded'
    },
    {
      id: 'doc-2',
      title: 'Proof of address',
      description: 'Utility bill, bank statement or official residence proof.',
      fileHint: 'Issued within last 3 months',
      status: 'Under Review'
    },
    {
      id: 'doc-3',
      title: 'Professional proof',
      description: 'Portfolio proof, company registration or service evidence.',
      fileHint: 'Optional but strongly recommended',
      status: 'Uploaded'
    },
    {
      id: 'doc-4',
      title: 'Tax / legal document',
      description: 'Freelance legal proof or local tax activity document.',
      fileHint: 'Required for premium escrow access',
      status: 'Missing'
    }
  ];

  readonly complianceChecklist: ComplianceItem[] = [
    {
      label: 'Identity matches profile information',
      description: 'Your submitted legal name must match your account identity.',
      checked: true
    },
    {
      label: 'Documents are clear and readable',
      description: 'Blurry or cropped files can delay the review process.',
      checked: true
    },
    {
      label: 'Address proof is recent',
      description: 'The document should usually be issued within the past 3 months.',
      checked: true
    },
    {
      label: 'Professional activity evidence is provided',
      description: 'Helps unlock stronger profile trust signals and premium visibility.',
      checked: false
    }
  ];

  readonly trustBenefits = [
    'Unlock stronger client trust markers',
    'Increase premium mission eligibility',
    'Improve escrow and payment credibility',
    'Strengthen public profile authority'
  ];

  getStatusClass(status: VerificationStep['status'] | RequiredDocument['status']): string {
    switch (status) {
      case 'Completed':
      case 'Uploaded':
        return 'status-badge status-badge--success';
      case 'In Review':
      case 'Under Review':
        return 'status-badge status-badge--warning';
      case 'Pending':
      case 'Missing':
        return 'status-badge status-badge--neutral';
      default:
        return 'status-badge';
    }
  }

  getStatToneClass(tone?: KycStat['tone']): string {
    switch (tone) {
      case 'accent':
        return 'stat-value stat-value--accent';
      case 'success':
        return 'stat-value stat-value--success';
      case 'warning':
        return 'stat-value stat-value--warning';
      default:
        return 'stat-value';
    }
  }
}