import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Intercepteur HTTP fonctionnel (nouvelle syntaxe Angular).
 * Ajoute automatiquement le header Authorization: Bearer <token>
 * à toutes les requêtes sortantes, si un token est disponible.
 * Évite d'avoir à le faire manuellement dans chaque service.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // Pas de token : on laisse passer la requête telle quelle
  // (utile pour /api/auth/login et /api/public/** qui n'exigent pas d'auth)
  if (!token) {
    return next(req);
  }

  // Clone la requête (les requêtes HTTP sont immuables en Angular)
  // et ajoute le header Authorization
  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authReq);
};