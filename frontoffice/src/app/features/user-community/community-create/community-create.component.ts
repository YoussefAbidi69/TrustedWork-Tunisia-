import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CommunityService } from '../../../core/services/community.service';

@Component({
  selector: 'app-community-create',
  templateUrl: './community-create.component.html',
  styleUrls: ['./community-create.component.css']
})
export class CommunityCreateComponent implements OnInit {
  submitted = false;
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private communityService: CommunityService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  ngOnInit(): void {}

  get f() {
    return this.form.controls;
  }

  cancel(): void {
    this.router.navigate(['../'], { relativeTo: this.route });
  }

  onSubmit(): void {
    this.submitted = true;
    if (this.form.invalid) return;
    const user = this.authService.getCurrentAuthUser();
    const userId = user?.userId;
    if (userId == null) return;
    const { name, description } = this.form.getRawValue();
    this.communityService
      .create({ name: name!, description: description!, createdBy: userId })
      .subscribe({
        next: () => this.router.navigate(['../browse'], { relativeTo: this.route }),
        error: () => {
          this.submitted = true;
        }
      });
  }
}
