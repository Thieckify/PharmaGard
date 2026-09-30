import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PharmacieService } from '../services/pharmacie.service';
import { PharmacieRequest } from '../models/pharmacie.model';
import { ReferentielService } from '../../../core/services/referentiel.service';
import { Quartier } from '../../../core/models/referentiel.model';
import { PositionGps, SelecteurPosition } from '../../../shared/components/selecteur-position/selecteur-position';

/**
 * Formulaire de création ou mise à jour de la fiche pharmacie du pharmacien connecté.
 * Un seul formulaire sert aux deux cas (upsert) : le backend décide de créer ou mettre à jour.
 * Sélection en cascade : la zone filtre la liste des quartiers proposés (EF-1.2 du CDC).
 * La position se choisit sur une carte (SelecteurPosition) et elle est obligatoire (RG-14).
 */
@Component({
  selector: 'app-pharmacies-formulaire',
  standalone: true,
  imports: [ReactiveFormsModule, SelecteurPosition],
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

  // Position choisie sur la carte, centre du quartier choisi, et alerte "position manquante"
  position = signal<PositionGps | null>(null);
  centreCarte = signal<PositionGps | null>(null);
  positionManquante = signal(false);

  envoiEnCours = signal(false);
  messageSucces = signal<string | null>(null);
  messageErreur = signal<string | null>(null);

  formulaire = this.fb.group({
    nom: ['', Validators.required],
    adresse: ['', Validators.required],
    zoneId: [null as number | null, Validators.required],
    quartierId: [null as number | null, Validators.required],
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

    // Changer de quartier (ou le vider) : l'ancienne position n'a plus de sens, on la retire,
    // et la carte s'ouvre sur le centre du nouveau quartier (s'il est connu)
    this.formulaire.get('quartierId')?.valueChanges.subscribe((quartierId) => {
      this.position.set(null);
      const quartier = this.tousLesQuartiers.find(q => q.id === quartierId);
      this.centreCarte.set(
        quartier?.latitude != null && quartier?.longitude != null
          ? { latitude: quartier.latitude, longitude: quartier.longitude }
          : null
      );
    });
  }

  // Appelé par le sélecteur quand l'utilisateur clique, glisse le repère ou utilise sa position
  onPositionChange(position: PositionGps): void {
    this.position.set(position);
    this.positionManquante.set(false);
  }

  onSubmit(): void {
    // La position est obligatoire (RG-14) : on l'affiche en même temps que les autres champs manquants
    const position = this.position();
    this.positionManquante.set(position == null);

    if (this.formulaire.invalid || position == null) {
      this.formulaire.markAllAsTouched();
      return;
    }

    const v = this.formulaire.getRawValue();
    if (v.quartierId == null) {
      return;
    }

    this.envoiEnCours.set(true);
    this.messageSucces.set(null);
    this.messageErreur.set(null);

    // La zone n'est pas envoyée : le backend la déduit du quartier
    const requete: PharmacieRequest = {
      nom: (v.nom ?? '').trim(),
      adresse: (v.adresse ?? '').trim(),
      quartierId: v.quartierId,
      latitude: position.latitude,
      longitude: position.longitude,
      telephone: v.telephone?.trim() || null,
      horaireOuverture: v.horaireOuverture?.trim() || null,
      horaireFermeture: v.horaireFermeture?.trim() || null,
      numeroAgrementOrdre: (v.numeroAgrementOrdre ?? '').trim()
    };

    this.pharmacieService.upsertPharmacie(requete).subscribe({
      next: () => {
        this.messageSucces.set('Fiche pharmacie enregistrée avec succès.');
        this.envoiEnCours.set(false);
      },
      error: (err: HttpErrorResponse) => {
        // Vrai message métier du backend quand il y en a un
        this.messageErreur.set(err.error?.message ?? "Erreur lors de l'enregistrement.");
        this.envoiEnCours.set(false);
      }
    });
  }
}