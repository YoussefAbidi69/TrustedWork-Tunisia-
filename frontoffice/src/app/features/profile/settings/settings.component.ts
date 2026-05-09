import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, UserProfileResponse } from '../../../core/services/user.service';

interface SettingsStat {
  label: string;
  value: string;
}

interface PreferenceToggle {
  id: string;
  title: string;
  description: string;
  enabled: boolean;
}

interface SecurityItem {
  label: string;
  value: string;
  status: string;
}

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent implements OnInit {
  readonly stats: SettingsStat[] = [
    { label: 'Profile completion', value: '92%' },
    { label: 'Security level', value: 'Advanced' },
    { label: 'Visibility score', value: 'High' },
    { label: 'Active preferences', value: '12' }
  ];

  readonly notificationPreferences: PreferenceToggle[] = [
    {
      id: 'notif-1',
      title: 'New opportunities alerts',
      description: 'Receive premium freelance opportunities matching your profile.',
      enabled: true
    },
    {
      id: 'notif-2',
      title: 'Application status updates',
      description: 'Get notified when a client reviews or updates your application.',
      enabled: true
    },
    {
      id: 'notif-3',
      title: 'Messages and conversations',
      description: 'Receive instant alerts for new unread conversations.',
      enabled: true
    },
    {
      id: 'notif-4',
      title: 'Marketing insights',
      description: 'Occasional platform tips, growth updates and product news.',
      enabled: false
    }
  ];

  readonly privacyPreferences: PreferenceToggle[] = [
    {
      id: 'privacy-1',
      title: 'Public profile visibility',
      description: 'Allow your public profile to appear in talent discovery results.',
      enabled: true
    },
    {
      id: 'privacy-2',
      title: 'Show certifications publicly',
      description: 'Display verified certifications on your premium profile.',
      enabled: true
    },
    {
      id: 'privacy-3',
      title: 'Display trust indicators',
      description: 'Show trust score, badges and profile credibility markers.',
      enabled: true
    },
    {
      id: 'privacy-4',
      title: 'Private activity mode',
      description: 'Hide recent internal activity from profile visitors.',
      enabled: false
    }
  ];

  securityItems: SecurityItem[] = [
    {
      label: 'Password strength',
      value: 'Strong',
      status: 'Verified'
    },
    {
      label: 'Two-factor authentication',
      value: 'Checking...',
      status: 'Loading'
    },
    {
      label: 'Connected devices',
      value: '3 sessions',
      status: 'Monitored'
    },
    {
      label: 'Recent login review',
      value: 'Today',
      status: 'Safe'
    }
  ];

  readonly workspacePreferences: string[] = [
    'Language: English',
    'Timezone: GMT+1',
    'Theme: Premium Light',
    'Desktop experience optimized'
  ];

  loading2FA = false;
  confirming2FA = false;
  disabling2FA = false;
  loadingProfile = false;

  twoFactorEnabled = false;
  qrCodeUri = '';
  setupStepVisible = false;
  confirmationCode = '';

  twoFactorMessage = '';
  twoFactorError = '';

  currentUserCin: string | number | null = null;
  currentUserEmail = '';

  constructor(
    private authService: AuthService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.loadCurrentUserSecurityState();
  }

  togglePreference(list: PreferenceToggle[], id: string): void {
    const item = list.find((preference) => preference.id === id);
    if (item) {
      item.enabled = !item.enabled;
    }
  }

  loadCurrentUserSecurityState(): void {
    this.clearTwoFactorMessages();
    this.loadingProfile = true;
    this.updateTwoFactorSecurityItem();

    const currentUser = this.authService.getCurrentAuthUser();

    if (!currentUser?.email) {
      this.loadingProfile = false;
      this.twoFactorError = 'Unable to load connected user.';
      this.updateTwoFactorSecurityItem();
      return;
    }

    this.currentUserEmail = currentUser.email;

    this.userService.getMyProfile()
      .pipe(
        finalize(() => {
          this.loadingProfile = false;
        })
      )
      .subscribe({
        next: (profile: UserProfileResponse) => {
          this.currentUserCin = profile?.cin ?? null;
          this.twoFactorEnabled = !!profile?.twoFactorEnabled;
          this.updateTwoFactorSecurityItem();
        },
        error: (err: HttpErrorResponse) => {
          console.error('PROFILE LOAD ERROR', err);
          this.twoFactorError = this.extractErrorMessage(
            err,
            'Unable to load security settings.'
          );
          this.updateTwoFactorSecurityItem();
        }
      });
  }

  startTwoFactorSetup(): void {
    this.clearTwoFactorMessages();

    if (!this.currentUserCin) {
      this.twoFactorError = 'User CIN not found.';
      return;
    }

    this.loading2FA = true;

    this.userService.setupTwoFactor(this.currentUserCin)
      .pipe(
        finalize(() => {
          this.loading2FA = false;
        })
      )
      .subscribe({
        next: (res) => {
          this.qrCodeUri =
            res?.qrCodeUri ||
            res?.otpauthUrl ||
            res?.otpauthUri ||
            '';

          this.setupStepVisible = true;
          this.twoFactorMessage =
            res?.message ||
            'Scan the QR code or copy the secret key into Google Authenticator.';
        },
        error: (err: HttpErrorResponse) => {
          console.error('SETUP 2FA ERROR', err);
          this.twoFactorError = this.extractOtpSetupError(err);
        }
      });
  }

  confirmTwoFactor(): void {
    this.clearTwoFactorMessages();

    if (!this.currentUserCin) {
      this.twoFactorError = 'User CIN not found.';
      return;
    }

    const normalizedCode = this.confirmationCode.trim();

    if (!/^\d{6}$/.test(normalizedCode)) {
      this.twoFactorError = 'Enter a valid 6-digit code.';
      return;
    }

    this.confirming2FA = true;

    this.userService.confirmTwoFactor(this.currentUserCin, normalizedCode)
      .pipe(
        finalize(() => {
          this.confirming2FA = false;
        })
      )
      .subscribe({
        next: () => {
          this.twoFactorEnabled = true;
          this.setupStepVisible = false;
          this.qrCodeUri = '';
          this.confirmationCode = '';
          this.twoFactorMessage = 'Two-factor authentication enabled successfully.';
          this.updateTwoFactorSecurityItem();
        },
        error: (err: HttpErrorResponse) => {
          console.error('CONFIRM 2FA ERROR', err);
          this.twoFactorError = this.extractOtpValidationError(err);
        }
      });
  }

  disableTwoFactor(): void {
    this.clearTwoFactorMessages();

    if (!this.currentUserCin) {
      this.twoFactorError = 'User CIN not found.';
      return;
    }

    this.disabling2FA = true;

    this.userService.disableTwoFactor(this.currentUserCin)
      .pipe(
        finalize(() => {
          this.disabling2FA = false;
        })
      )
      .subscribe({
        next: () => {
          this.twoFactorEnabled = false;
          this.setupStepVisible = false;
          this.qrCodeUri = '';
          this.confirmationCode = '';
          this.twoFactorMessage = 'Two-factor authentication disabled successfully.';
          this.updateTwoFactorSecurityItem();
        },
        error: (err: HttpErrorResponse) => {
          console.error('DISABLE 2FA ERROR', err);
          this.twoFactorError = this.extractErrorMessage(
            err,
            'Failed to disable 2FA.'
          );
        }
      });
  }

  get extractedSecret(): string {
    if (!this.qrCodeUri || !this.qrCodeUri.includes('secret=')) {
      return '';
    }

    const match = this.qrCodeUri.match(/secret=([^&]+)/);
    return match ? decodeURIComponent(match[1]) : '';
  }

  private updateTwoFactorSecurityItem(): void {
    const twoFactorItem = this.securityItems.find(
      (item) => item.label === 'Two-factor authentication'
    );

    if (!twoFactorItem) {
      return;
    }

    if (this.loadingProfile) {
      twoFactorItem.value = 'Checking...';
      twoFactorItem.status = 'Loading';
      return;
    }

    if (this.twoFactorEnabled) {
      twoFactorItem.value = 'Enabled';
      twoFactorItem.status = 'Active';
    } else {
      twoFactorItem.value = 'Disabled';
      twoFactorItem.status = 'Inactive';
    }
  }

  private clearTwoFactorMessages(): void {
    this.twoFactorMessage = '';
    this.twoFactorError = '';
  }

  private extractOtpSetupError(error: HttpErrorResponse): string {
    return this.extractErrorMessage(error, 'Failed to initialize 2FA.');
  }

  private extractOtpValidationError(error: HttpErrorResponse): string {
    const message = this.extractErrorMessage(error, '');
    const normalized = message.toLowerCase();

    if (
      normalized.includes('expired') ||
      normalized.includes('expire')
    ) {
      return 'Invalid or expired code.';
    }

    if (
      normalized.includes('invalid') ||
      normalized.includes('incorrect') ||
      normalized.includes('wrong')
    ) {
      return 'Invalid or expired code.';
    }

    return message || 'Invalid or expired code.';
  }

  private extractErrorMessage(error: HttpErrorResponse, fallback: string): string {
    return (
      error?.error?.message ||
      error?.error?.error ||
      error?.error?.details ||
      fallback
    );
  }
}