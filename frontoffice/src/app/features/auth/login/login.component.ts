import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { AuthResponse } from '../../../core/models/auth.model';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  submitted = false;
  loading = false;
  hidePassword = true;
  successMessage = '';
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      rememberMe: [true]
    });
  }

  ngOnInit(): void {
  }

  get f() {
    return this.loginForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.successMessage = '';
    this.errorMessage = '';

    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;

    const payload = {
      email: this.loginForm.value.email,
      password: this.loginForm.value.password
    };

    const rememberMe = this.loginForm.value.rememberMe;

    this.authService.login(payload, rememberMe).subscribe({
      next: (res: AuthResponse) => {
        this.loading = false;

        if (res.twoFactorRequired) {
          this.successMessage = 'Code de vérification requis.';
          sessionStorage.setItem('2fa_email', payload.email);
          sessionStorage.setItem('remember_me', String(rememberMe));
          this.router.navigate(['/auth/2fa']);
          return;
        }

        this.successMessage = 'Connexion réussie.';
        this.router.navigate(['/app/dashboard']);
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;

        if (err.status === 401) {
          this.errorMessage = 'Email ou mot de passe incorrect.';
        } else {
          this.errorMessage = 'Erreur serveur. Réessaye plus tard.';
        }

        console.error('LOGIN ERROR', err);
      }
    });
  }
}