import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/auth.model';

/**
 * Guard qui vérifie que l'utilisateur connecté a un rôle autorisé.
 * S'utilise comme une factory : roleGuard(['ADMIN', 'SUPERVISEUR'])
 * S'il n'est pas connecté, renvoie vers login ;
 * s'il est connecté mais avec un mauvais rôle, renvoie vers l'accueil.
 */
export const roleGuard = (allowedRoles: Role[]): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const user = authService.currentUser();

    if (!user) {
      router.navigate(['/auth/login']);
      return false;
    }

    if (!allowedRoles.includes(user.role)) {
      // Utilisateur connecté mais rôle non autorisé
      router.navigate(['/']);
      return false;
    }

    return true;
  };
};