import { Component } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../core/services/auth.service';

/**
 * Dashboard temporaire de test.
 * Affiche les infos de l'utilisateur connecté et permet de se déconnecter.
 * Servira de point d'entrée après login jusqu'à ce que les vrais dashboards
 * (Pharmacien, Superviseur, Admin) soient créés dans les prochaines séances.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [ButtonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard {
  constructor(public authService: AuthService) {}

  logout(): void {
    this.authService.logout();
  }
}