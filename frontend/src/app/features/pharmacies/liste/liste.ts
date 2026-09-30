import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { PharmacieService } from '../services/pharmacie.service';
import { Pharmacie } from '../models/pharmacie.model';
import { AuthService } from '../../../core/services/auth.service';
import { PageLayout } from '../../../shared/layout/page-layout/page-layout';
import { CartePharmacies } from '../../../shared/components/carte-pharmacies/carte-pharmacies';
import { LieuCarte } from '../../../shared/models/lieu-carte.model';

// Message affiché après une action (validation, suppression), en succès ou en erreur
interface Message {
  type: 'succes' | 'erreur';
  texte: string;
}

/**
 * Affiche les pharmacies visibles pour l'utilisateur connecté. Le scope (sa pharmacie si
 * PHARMACIEN, sa zone ou toutes si SUPERVISEUR) est appliqué côté backend (RG-01 / RG-02).
 * Pour un SUPERVISEUR, la page sert aussi à modérer : voir le numéro d'agrément de chaque
 * fiche, la valider ou retirer sa validation, ou la supprimer (RG-15), avec une carte de
 * toutes ses pharmacies (repère doré = en attente, vert = validée). Le serveur reste
 * juge de ces droits : les boutons cachés au pharmacien ne sont qu'un confort.
 */
@Component({
  selector: 'app-pharmacies-liste',
  standalone: true,
  imports: [DatePipe, PageLayout, CartePharmacies],
  templateUrl: './liste.html',
  styleUrl: './liste.scss'
})
export class Liste implements OnInit {
  private pharmacieService = inject(PharmacieService);
  private authService = inject(AuthService);

  pharmacies = signal<Pharmacie[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);

  // Fiche dont la suppression attend une confirmation (une seule à la fois)
  idEnConfirmation = signal<number | null>(null);
  // Fiche sur laquelle une action est en cours d'envoi (ses boutons sont alors désactivés)
  idEnCours = signal<number | null>(null);
  message = signal<Message | null>(null);

  estSuperviseur = computed(() => this.authService.currentUser()?.role === 'SUPERVISEUR');

  sousTitre = computed(() => {
    if (!this.estSuperviseur()) {
      return 'La fiche de votre pharmacie, telle qu\'elle est enregistrée sur la plateforme.';
    }
    return this.authService.currentUser()?.zoneId != null
      ? 'Les fiches de votre zone : contrôlez le numéro d\'agrément avant de valider.'
      : 'Les fiches de toutes les zones : contrôlez le numéro d\'agrément avant de valider.';
  });

  // Les fiches en attente d'abord (c'est ce que le superviseur a à traiter), puis par nom
  pharmaciesTriees = computed(() =>
    [...this.pharmacies()].sort(
      (a, b) => Number(a.active) - Number(b.active) || a.nom.localeCompare(b.nom)
    )
  );

  nbEnAttente = computed(() => this.pharmacies().filter(p => !p.active).length);
  nbValidees = computed(() => this.pharmacies().filter(p => p.active).length);

  // Les fiches telles que la carte les attend : la couleur du repère dépend du statut.
  // La carte ignore d'elle-même celles qui n'ont pas de position.
  lieuxCarte = computed(() =>
    this.pharmacies().map((p): LieuCarte => ({
      nom: p.nom,
      adresse: p.adresse,
      latitude: p.latitude,
      longitude: p.longitude,
      statut: p.active ? 'validee' : 'attente'
    }))
  );

  // Fiches qui ne peuvent pas figurer sur la carte faute de position
  nbSansPosition = computed(
    () => this.pharmacies().filter(p => p.latitude == null || p.longitude == null).length
  );

  ngOnInit(): void {
    this.pharmacieService.listPharmacies().subscribe({
      next: (donnees) => {
        this.pharmacies.set(donnees);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger les pharmacies.');
        this.chargement.set(false);
      }
    });
  }

  // Valide (true) ou retire la validation (false) d'une fiche, puis met la liste à jour
  changerValidation(pharmacie: Pharmacie, valide: boolean): void {
    this.idEnCours.set(pharmacie.id);
    this.message.set(null);

    this.pharmacieService.validerPharmacie(pharmacie.id, valide).subscribe({
      next: (misAJour) => {
        this.pharmacies.update(liste => liste.map(p => (p.id === misAJour.id ? misAJour : p)));
        this.idEnCours.set(null);
        this.message.set({
          type: 'succes',
          texte: valide
            ? `« ${misAJour.nom} » est validée : elle apparaît maintenant sur le site.`
            : `La validation de « ${misAJour.nom} » est retirée : elle repasse en attente.`
        });
      },
      error: (err: HttpErrorResponse) => this.afficherErreur(err, 'Impossible de modifier la validation.')
    });
  }

  // Étape 1 de la suppression : on demande confirmation, rien n'est encore supprimé
  demanderSuppression(id: number): void {
    this.idEnConfirmation.set(id);
    this.message.set(null);
  }

  annulerSuppression(): void {
    this.idEnConfirmation.set(null);
  }

  // Étape 2 : suppression définitive (fiche, planning et compte du pharmacien, côté serveur)
  supprimer(pharmacie: Pharmacie): void {
    this.idEnCours.set(pharmacie.id);

    this.pharmacieService.supprimerPharmacie(pharmacie.id).subscribe({
      next: () => {
        this.pharmacies.update(liste => liste.filter(p => p.id !== pharmacie.id));
        this.idEnCours.set(null);
        this.idEnConfirmation.set(null);
        this.message.set({ type: 'succes', texte: `« ${pharmacie.nom} » a été supprimée, ainsi que son compte.` });
      },
      error: (err: HttpErrorResponse) => this.afficherErreur(err, 'Impossible de supprimer la pharmacie.')
    });
  }

  // Affiche le vrai message du backend quand il y en a un (ex. le 403 hors zone)
  private afficherErreur(err: HttpErrorResponse, messageParDefaut: string): void {
    this.idEnCours.set(null);
    this.idEnConfirmation.set(null);
    this.message.set({ type: 'erreur', texte: err.error?.message ?? messageParDefaut });
  }
}