import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PharmacieService } from '../services/pharmacie.service';
import { ReferentielService } from '../../../core/services/referentiel.service';
import { Quartier } from '../../../core/models/referentiel.model';

/**
 * Formulaire de création ou mise à jour de la fiche pharmacie du pharmacien connecté.
 * Un seul formulaire sert aux deux cas (upsert) : le backend décide de créer ou mettre à jour.
 * Sélection en cascade : la zone filtre la liste des quartiers proposés (EF-1.2 du CDC).
 */
@Component({
  selector: 'app-pharmacies-formulaire',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './formulaire.html',
  styleUrl: './formulaire.scss'
})
export class Formulaire implements OnInit {
  private fb = inject(FormBuilder);
  private pharmacieService = inject(PharmacieService);
  private referentielService = inject(ReferentielService);

  tousLesQuartiers: Quartier[] = [];
  quartiersFiltres = signal<Quartier[]>([]);
  zones = signal<{ id: number; nom: string }[]>([]);

  envoiEnCours = signal(false);
  messageSucces = signal<string | null>(null);
  messageErreur = signal<string | null>(null);

  formulaire = this.fb.group({
    nom: ['', Validators.required],
    adresse: ['', Validators.required],
    zoneId: [null as number | null, Validators.required],
    quartierId: [null as number | null, Validators.required],
    latitude: [null as number | null],
    longitude: [null as number | null],
    telephone: [''],
    horaireOuverture: ['08:00'],
    horaireFermeture: ['23:00'],
    numeroAgrementOrdre: ['', Validators.required]
  });

  ngOnInit(): void {
    this.referentielService.listQuartiers().subscribe({
      next: (donnees) => {
        this.tousLesQuartiers = donnees;

        const zonesUniques = new Map<number, string>();
        donnees.forEach(q => zonesUniques.set(q.zoneId, q.nomZone));
        this.zones.set(
          Array.from(zonesUniques, ([id, nom]) => ({ id, nom }))
            .sort((a, b) => a.nom.localeCompare(b.nom))
        );
      }
    });

    this.formulaire.get('zoneId')?.valueChanges.subscribe((zoneId) => {
      this.quartiersFiltres.set(
        zoneId ? this.tousLesQuartiers.filter(q => q.zoneId === zoneId) : []
      );
      this.formulaire.get('quartierId')?.setValue(null);
    });
  }

  onSubmit(): void {
    if (this.formulaire.invalid) {
      this.formulaire.markAllAsTouched();
      return;
    }

    this.envoiEnCours.set(true);
    this.messageErreur.set(null);

    const { zoneId, ...donneesAEnvoyer } = this.formulaire.getRawValue();

    this.pharmacieService.upsertPharmacie(donneesAEnvoyer as any).subscribe({
      next: () => {
        this.messageSucces.set('Fiche pharmacie enregistrée avec succès.');
        this.envoiEnCours.set(false);
      },
      error: () => {
        this.messageErreur.set("Erreur lors de l'enregistrement.");
        this.envoiEnCours.set(false);
      }
    });
  }
}