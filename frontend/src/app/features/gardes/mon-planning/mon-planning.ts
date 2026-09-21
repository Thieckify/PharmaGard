import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { GardeService } from '../services/garde.service';
import { Garde, GardeRequest, TypeGarde } from '../models/garde.model';

/**
 * Page de gestion du planning de garde du pharmacien connecté : déclaration,
 * modification et suppression de ses propres périodes (CDC — le pharmacien
 * "gère... son planning de garde"). Le backend applique le scoping par tenant (RG-01).
 */
@Component({
  selector: 'app-mon-planning',
  standalone: true,
  imports: [FormsModule, DatePicker, Select],
  templateUrl: './mon-planning.html',
  styleUrl: './mon-planning.scss'
})
export class MonPlanning implements OnInit {
  private gardeService = inject(GardeService);

  gardes = signal<Garde[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);
  enregistrement = signal(false);
  erreurFormulaire = signal<string | null>(null);

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
    this.charger();
  }

  private charger(): void {
    this.chargement.set(true);
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

  // Prépare le formulaire pour modifier une garde existante
  modifier(garde: Garde): void {
    this.gardeEnEdition.set(garde);
    this.dateDebut = new Date(garde.dateDebut);
    this.dateFin = new Date(garde.dateFin);
    this.type = garde.type;
    this.erreurFormulaire.set(null);
  }

  annulerEdition(): void {
    this.gardeEnEdition.set(null);
    this.reinitialiserFormulaire();
  }

  supprimer(garde: Garde): void {
    if (!confirm(`Supprimer la période du ${garde.dateDebut} au ${garde.dateFin} ?`)) {
      return;
    }
    this.gardeService.supprimerGarde(garde.id).subscribe({
      next: () => this.charger(),
      error: () => this.erreur.set('Impossible de supprimer cette période.')
    });
  }

  onSubmit(): void {
    if (!this.dateDebut || !this.dateFin) {
      this.erreurFormulaire.set('Les deux dates sont obligatoires.');
      return;
    }

    this.erreurFormulaire.set(null);
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
        this.gardeEnEdition.set(null);
        this.reinitialiserFormulaire();
        this.charger();
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