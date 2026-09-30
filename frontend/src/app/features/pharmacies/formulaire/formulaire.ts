import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { Select } from 'primeng/select';
import { PharmacieService } from '../services/pharmacie.service';
import { Pharmacie, PharmacieRequest } from '../models/pharmacie.model';
import { ReferentielService } from '../../../core/services/referentiel.service';
import { Quartier } from '../../../core/models/referentiel.model';
import { PageLayout } from '../../../shared/layout/page-layout/page-layout';
import { PositionGps, SelecteurPosition } from '../../../shared/components/selecteur-position/selecteur-position';

/**
 * Formulaire de création ou mise à jour de la fiche pharmacie du pharmacien connecté.
 * Un seul formulaire sert aux deux cas (upsert) : le backend décide de créer ou mettre à jour.
 * S'il existe déjà une fiche, le formulaire s'ouvre prérempli, avec son statut (validée ou en
 * attente) et un avertissement si un changement de zone la remettrait en attente (RG-11).
 * Le numéro d'agrément se saisit à la création seulement : il ne se modifie plus ensuite.
 * La position se choisit sur une carte (SelecteurPosition) et elle est obligatoire (RG-14).
 */
@Component({
  selector: 'app-pharmacies-formulaire',
  standalone: true,
  imports: [ReactiveFormsModule, InputTextModule, MessageModule, Select, SelecteurPosition, PageLayout],
  templateUrl: './formulaire.html',
  styleUrl: './formulaire.scss'
})
export class Formulaire implements OnInit {
  private fb = inject(FormBuilder);
  private pharmacieService = inject(PharmacieService);
  private referentielService = inject(ReferentielService);

  // Liste complète des quartiers, chargée une fois : seulement filtrée par zone dans
  // quartiersFiltres, donc pas besoin d'un signal
  private tousLesQuartiers: Quartier[] = [];

  quartiersFiltres = signal<Quartier[]>([]);
  zones = signal<{ id: number; nom: string }[]>([]);

  // Fiche déjà enregistrée (null tant que le pharmacien n'en a pas créé)
  fiche = signal<Pharmacie | null>(null);
  chargement = signal(true);
  erreurChargement = signal<string | null>(null);

  // Zone actuellement choisie dans le formulaire (sert à repérer un changement de zone)
  zoneChoisie = signal<number | null>(null);

  // Vrai quand la zone choisie n'est plus celle de la fiche enregistrée : l'enregistrement
  // remettra la fiche en attente de validation par le superviseur de la nouvelle zone (RG-11)
  changementDeZone = computed(() => {
    const fiche = this.fiche();
    const zone = this.zoneChoisie();
    return fiche != null && zone != null && zone !== fiche.zoneId;
  });

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
    // Cascade : changer de zone filtre les quartiers proposés et vide le quartier choisi
    this.formulaire.controls.zoneId.valueChanges.subscribe((zoneId) => {
      this.zoneChoisie.set(zoneId);
      this.quartiersFiltres.set(
        zoneId != null ? this.tousLesQuartiers.filter(q => q.zoneId === zoneId) : []
      );
      this.formulaire.controls.quartierId.setValue(null);
    });

    // Changer de quartier (ou le vider) : l'ancienne position n'a plus de sens, on la retire,
    // et la carte s'ouvre sur le centre du nouveau quartier (s'il est connu)
    this.formulaire.controls.quartierId.valueChanges.subscribe((quartierId) => {
      this.position.set(null);
      this.centreCarte.set(this.centreDuQuartier(quartierId));
    });

    // On charge les quartiers ET la fiche existante avant d'afficher le formulaire, pour
    // qu'il s'ouvre directement prérempli (sans flash de formulaire vide)
    forkJoin({
      quartiers: this.referentielService.listQuartiers(),
      fiches: this.pharmacieService.listPharmacies()
    }).subscribe({
      next: ({ quartiers, fiches }) => {
        this.tousLesQuartiers = quartiers;

        // Une zone n'a pas d'endpoint propre : on la déduit des quartiers (une entrée par zone)
        const zonesUniques = new Map<number, string>();
        quartiers.forEach(q => zonesUniques.set(q.zoneId, q.nomZone));
        this.zones.set(
          Array.from(zonesUniques, ([id, nom]) => ({ id, nom }))
            .sort((a, b) => a.nom.localeCompare(b.nom))
        );

        // Un pharmacien n'a qu'une seule fiche (RG-01) : la liste en contient 0 ou 1
        if (fiches.length > 0) {
          this.preremplir(fiches[0]);
        }
        this.chargement.set(false);
      },
      error: () => {
        this.erreurChargement.set('Impossible de charger votre fiche. Rechargez la page.');
        this.chargement.set(false);
      }
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

    // Calculé avant l'envoi : après, la fiche rechargée aura déjà la nouvelle zone
    const zoneChangee = this.changementDeZone();

    this.envoiEnCours.set(true);
    this.messageSucces.set(null);
    this.messageErreur.set(null);

    // La zone n'est pas envoyée : le backend la déduit du quartier. Le numéro d'agrément est
    // vide quand la fiche existe déjà : le backend l'ignore (il ne se modifie plus).
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
        this.messageSucces.set(
          zoneChangee
            ? 'Fiche enregistrée. Comme vous avez changé de zone, elle repasse en attente de validation.'
            : 'Fiche pharmacie enregistrée avec succès.'
        );
        this.envoiEnCours.set(false);
        this.actualiserFiche();
      },
      error: (err: HttpErrorResponse) => {
        // Vrai message métier du backend quand il y en a un
        this.messageErreur.set(err.error?.message ?? "Erreur lors de l'enregistrement.");
        this.envoiEnCours.set(false);
      }
    });
  }

  // Remplit le formulaire avec la fiche existante. On utilise emitEvent: false : sans cela,
  // les réactions "changement de zone" et "changement de quartier" videraient aussitôt
  // la sélection et la position qu'on vient de remplir.
  private preremplir(fiche: Pharmacie): void {
    this.fiche.set(fiche);
    this.zoneChoisie.set(fiche.zoneId);
    this.quartiersFiltres.set(this.tousLesQuartiers.filter(q => q.zoneId === fiche.zoneId));

    this.formulaire.patchValue({
      nom: fiche.nom,
      adresse: fiche.adresse,
      zoneId: fiche.zoneId,
      quartierId: fiche.quartierId,
      telephone: fiche.telephone ?? '',
      horaireOuverture: fiche.horaireOuverture ?? '',
      horaireFermeture: fiche.horaireFermeture ?? ''
    }, { emitEvent: false });

    this.verrouillerAgrement();

    if (fiche.latitude != null && fiche.longitude != null) {
      // La carte s'ouvre directement sur le repère de la pharmacie
      this.position.set({ latitude: fiche.latitude, longitude: fiche.longitude });
    } else {
      // Fiche ancienne sans position : la carte s'ouvre sur le quartier, en attendant que
      // le pharmacien place sa pharmacie (obligatoire à l'enregistrement)
      this.centreCarte.set(this.centreDuQuartier(fiche.quartierId));
    }
  }

  // Une fois la fiche créée, le numéro d'agrément n'est plus demandé
  private verrouillerAgrement(): void {
    const agrement = this.formulaire.controls.numeroAgrementOrdre;
    agrement.clearValidators();
    agrement.updateValueAndValidity({ emitEvent: false });
  }

  // Recharge la fiche après un enregistrement, pour mettre à jour le statut affiché et la
  // zone de référence (et masquer le champ d'agrément si la fiche vient d'être créée)
  private actualiserFiche(): void {
    this.pharmacieService.listPharmacies().subscribe({
      next: (fiches) => {
        const fiche = fiches[0] ?? null;
        this.fiche.set(fiche);
        if (fiche) {
          this.verrouillerAgrement();
        }
      }
    });
  }

  // Centre approximatif d'un quartier, ou null s'il est inconnu
  private centreDuQuartier(quartierId: number | null): PositionGps | null {
    const quartier = this.tousLesQuartiers.find(q => q.id === quartierId);
    return quartier?.latitude != null && quartier?.longitude != null
      ? { latitude: quartier.latitude, longitude: quartier.longitude }
      : null;
  }
}