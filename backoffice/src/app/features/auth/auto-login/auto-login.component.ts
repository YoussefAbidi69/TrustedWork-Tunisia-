import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-auto-login',
  template: `<p style="text-align:center;margin-top:2rem;">Redirection en cours...</p>`
})
export class AutoLoginComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Récupération des paramètres transmis depuis le frontoffice
    const token  = this.route.snapshot.queryParamMap.get('token');
    const userId = this.route.snapshot.queryParamMap.get('userId');
    const email  = this.route.snapshot.queryParamMap.get('email');
    const role   = this.route.snapshot.queryParamMap.get('role');

    if (!token || role?.toUpperCase() !== 'ADMIN') {
      // Token manquant ou rôle non autorisé → login classique
      this.router.navigate(['/auth/login']);
      return;
    }

    // Sauvegarde de la session dans localStorage (même clés que l'auth service backoffice)
    localStorage.setItem('token',  token);
    localStorage.setItem('role',   role);
    localStorage.setItem('email',  email  || '');
    localStorage.setItem('userId', userId || '');

    // Redirection vers le dashboard admin
    this.router.navigate(['/admin/dashboard']);
  }
}