import { Component, computed, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

interface LienNav {
  libelle: string;
  chemin: string;
}

// Liens de la barre de navigation selon le rôle de la personne connectée
const LIENS_PAR_ROLE: Record<string, LienNav[]> = {
  PHARMACIEN: [
    { libelle: 'Tableau de bord', chemin: '/dashboard' },
    { libelle: 'Ma fiche', chemin: '/pharmacies/ma-fiche' },
    { libelle: 'Mon planning', chemin: '/pharmacies/mon-planning' }
  ],
  SUPERVISEUR: [
    { libelle: 'Tableau de bord', chemin: '/dashboard' },
    { libelle: 'Pharmacies', chemin: '/pharmacies' }
  ],
  ADMIN: [
    { libelle: 'Tableau de bord', chemin: '/dashboard' }
  ]
};

// Structure commune des pages qui ne sont pas l'espace public : barre de navigation,
// bandeau sombre (petit titre, titre avec un mot en doré, sous-titre), feuille verte
// pâle qui remonte sur le bandeau, et pied de page. Le contenu de la page s'écrit entre
// les balises <app-page-layout>...</app-page-layout>. Un seul endroit pour retoucher le
// design de toutes les pages (dette n°12 du CONTEXT.md).
@Component({
  selector: 'app-page-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './page-layout.html',
  styleUrl: './page-layout.scss',
  host: { '[class.layout--large]': "largeur() === 'large'" }
})
export class PageLayout {
  private authService = inject(AuthService);

  // Petit titre en majuscules au-dessus du titre (ex. "Ma pharmacie")
  eyebrow = input('');
  // Titre de la page ; titreAccent s'ajoute juste après, en doré (ex. "Fiche de votre" + "pharmacie")
  titre = input.required<string>();
  titreAccent = input('');
  sousTitre = input('');
  // "etroite" pour un formulaire, "large" pour une liste ou un planning
  largeur = input<'etroite' | 'large'>('etroite');

  utilisateur = this.authService.currentUser;

  liens = computed(() => LIENS_PAR_ROLE[this.authService.currentUser()?.role ?? ''] ?? []);

  deconnecter(): void {
    this.authService.logout();
  }
}