import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard qui protège une route en exigeant que l'utilisateur soit connecté.
 * Si non connecté, redirige vers la page de login.
 * À appliquer sur toutes les routes qui nécessitent une authentification.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // Pas connecté : redirection vers le login
  router.navigate(['/auth/login']);
  return false;
};