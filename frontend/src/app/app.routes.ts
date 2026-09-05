import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

// Table de routage principale de l'application.
// authGuard bloque l'accès si personne n'est connecté ; roleGuard restreint en plus par rôle.
export const routes: Routes = [
  // Page de connexion : accessible sans être authentifié
  {
    path: 'auth/login',
    loadComponent: () => import('./features/auth/login/login').then(m => m.Login)
  },
  // Tableau de bord temporaire de test (S2), accessible à tout utilisateur connecté
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard),
    canActivate: [authGuard]
  },
  // Liste des pharmacies : réservée aux PHARMACIEN et SUPERVISEUR (même restriction que côté backend)
  {
    path: 'pharmacies',
    loadComponent: () => import('./features/pharmacies/liste/liste').then(m => m.Liste),
    canActivate: [authGuard, roleGuard(['PHARMACIEN', 'SUPERVISEUR'])]
  },
  // Formulaire de création/mise à jour de sa fiche pharmacie : réservé au PHARMACIEN (upsert de sa propre fiche)
  {
    path: 'pharmacies/ma-fiche',
    loadComponent: () => import('./features/pharmacies/formulaire/formulaire').then(m => m.Formulaire),
    canActivate: [authGuard, roleGuard(['PHARMACIEN'])]
  },
  // Redirection par défaut : envoie vers dashboard (qui redirigera vers login si pas connecté)
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
];