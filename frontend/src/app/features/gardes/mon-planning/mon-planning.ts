import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, ViewportScroller } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePicker } from 'primeng/datepicker';
import { MessageModule } from 'primeng/message';
import { Select } from 'primeng/select';
import { GardeService } from '../services/garde.service';
import { Garde, GardeRequest, TypeGarde } from '../models/garde.model';
import { PageLayout } from '../../../shared/layout/page-layout/page-layout';

// Message affiché au-dessus de la liste après une suppression, en succès ou en erreur
interface Message {
  type: 'succes' | 'erreur';
  texte: string;
}

/**
 * Page de gestion du planning de garde du pharmacien connecté : déclaration,
 * modification et suppression de ses propres périodes (CDC — le pharmacien
 * "gère... son planning de garde"). Le backend applique le scoping par tenant (RG-01),
 * l'exigence d'une fiche validée (RG-12) et l'anti-antidatage (RG-13) : ses messages
 * d'erreur sont affichés tels quels.
 */
@Component({
  selector: 'app-mon-planning',
  standalone: true,
  imports: [FormsModule, DatePicker, Select, MessageModule, DatePipe, PageLayout],
  templateUrl: './mon-planning.html',
  styleUrl: './mon-planning.scss'
})
export class MonPlanning implements OnInit {
  private gardeService = inject(GardeService);
  private viewportScroller = inject(ViewportScroller);

  gardes = signal<Garde[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);
  enregistrement = signal(false);
  erreurFormulaire = signal<string | null>(null);
  messageSucces = signal<string | null>(null);
  messageListe = signal<Message | null>(null);

  // Les périodes de la plus récente à la plus ancienne (date de début décroissante) : la
  // dernière garde est la première visible. Les dates "AAAA-MM-JJ" se comparent comme du
  // texte, et on trie une copie pour ne jamais modifier la liste d'origine.
  gardesTriees = computed(() =>
    [...this.gardes()].sort((a, b) => b.dateDebut.localeCompare(a.dateDebut))
  );

  // Période dont la suppression attend une confirmation (une seule à la fois)
  idEnConfirmation = signal<number | null>(null);
  // Période en cours de suppression (ses boutons sont alors désactivés)
  idSuppressionEnCours = signal<number | null>(null);

  // Champs de saisie du formulaire : propriétés simples (pas des signals), comme
  // dans Login — ils ne pilotent aucun affichage ailleurs, juste le formulaire lui-même
  dateDebut: Date | null = null;
  dateFin: Date | null = null;
  type: TypeGarde = 'COMPLETE';

  // Non-null quand on modifie une garde existante ; null en mode "nouvelle déclaration"
  gardeEnEdition = signal<Garde | null>(null);

  typesGarde = [
    { label: 'Garde de jour', value: 'JOUR' as TypeGarde },
    { label: 'Garde de nuit', value: 'NUIT' as TypeGarde },
    { label: 'Garde complète', value: 'COMPLETE' as TypeGarde }
  ];

  ngOnInit(): void {
    this.charger(true);
  }

  // Recharge le planning. Le texte "Chargement..." ne s'affiche qu'au premier chargement :
  // après une action, la liste reste visible et se met à jour sans clignoter.
  private charger(afficherChargement: boolean): void {
    if (afficherChargement) {
      this.chargement.set(true);
    }
    this.erreur.set(null);
    this.gardeService.listMesGardes().subscribe({
      next: (donnees) => {
        this.gardes.set(donnees);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger votre planning.');
        this.chargement.set(false);
      }
    });
  }

  // Prépare le formulaire pour modifier une garde existante, et remonte en haut de page :
  // le formulaire est au-dessus de la liste, on ne verrait pas qu'il a changé
  modifier(garde: Garde): void {
    this.gardeEnEdition.set(garde);
    this.dateDebut = new Date(garde.dateDebut);
    this.dateFin = new Date(garde.dateFin);
    this.type = garde.type;
    this.erreurFormulaire.set(null);
    this.messageSucces.set(null);
    this.messageListe.set(null);
    this.idEnConfirmation.set(null);
    this.viewportScroller.scrollToPosition([0, 0]);
  }

  annulerEdition(): void {
    this.gardeEnEdition.set(null);
    this.erreurFormulaire.set(null);
    this.reinitialiserFormulaire();
  }

  // Étape 1 de la suppression : on demande confirmation, rien n'est encore supprimé
  demanderSuppression(id: number): void {
    this.idEnConfirmation.set(id);
    this.messageListe.set(null);
  }

  annulerSuppression(): void {
    this.idEnConfirmation.set(null);
  }

  // Étape 2 : suppression définitive de la période
  supprimer(garde: Garde): void {
    this.idSuppressionEnCours.set(garde.id);

    this.gardeService.supprimerGarde(garde.id).subscribe({
      next: () => {
        this.idSuppressionEnCours.set(null);
        this.idEnConfirmation.set(null);
        this.messageListe.set({ type: 'succes', texte: 'La période a été supprimée.' });
        // Si on était justement en train de modifier cette période, on quitte le mode édition
        if (this.gardeEnEdition()?.id === garde.id) {
          this.annulerEdition();
        }
        this.charger(false);
      },
      error: (err: HttpErrorResponse) => {
        this.idSuppressionEnCours.set(null);
        this.idEnConfirmation.set(null);
        this.messageListe.set({
          type: 'erreur',
          texte: err.error?.message ?? 'Impossible de supprimer cette période.'
        });
      }
    });
  }

  onSubmit(): void {
    if (!this.dateDebut || !this.dateFin) {
      this.erreurFormulaire.set('Les deux dates sont obligatoires.');
      return;
    }

    this.erreurFormulaire.set(null);
    this.messageSucces.set(null);
    this.enregistrement.set(true);

    const request: GardeRequest = {
      dateDebut: this.formatDate(this.dateDebut),
      dateFin: this.formatDate(this.dateFin),
      type: this.type
    };

    const enEdition = this.gardeEnEdition();
    const appel = enEdition
      ? this.gardeService.modifierGarde(enEdition.id, request)
      : this.gardeService.declarerGarde(request);

    appel.subscribe({
      next: () => {
        this.enregistrement.set(false);
        this.messageSucces.set(enEdition ? 'La période a été modifiée.' : 'La période a été déclarée.');
        this.gardeEnEdition.set(null);
        this.reinitialiserFormulaire();
        this.charger(false);
      },
      error: (err: HttpErrorResponse) => {
        this.enregistrement.set(false);
        this.erreurFormulaire.set(err.error?.message ?? "Impossible d'enregistrer cette période.");
      }
    });
  }

  private reinitialiserFormulaire(): void {
    this.dateDebut = null;
    this.dateFin = null;
    this.type = 'COMPLETE';
  }

  // Même logique que sur la page publique : construction locale pour éviter
  // le décalage de fuseau horaire de toISOString().
  private formatDate(date: Date): string {
    const annee = date.getFullYear();
    const mois = String(date.getMonth() + 1).padStart(2, '0');
    const jour = String(date.getDate()).padStart(2, '0');
    return `${annee}-${mois}-${jour}`;
  }

  libelleType(type: TypeGarde): string {
    switch (type) {
      case 'JOUR': return 'Garde de jour';
      case 'NUIT': return 'Garde de nuit';
      case 'COMPLETE': return 'Garde complète';
      default: return type;
    }
  }
}