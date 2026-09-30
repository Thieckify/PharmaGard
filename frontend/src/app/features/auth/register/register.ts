import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { Select } from 'primeng/select';
import { AuthService } from '../../../core/services/auth.service';
import { ReferentielService } from '../../../core/services/referentiel.service';
import { Quartier } from '../../../core/models/referentiel.model';
import { RegisterRequest } from '../../../core/models/auth.model';
import { PositionGps, SelecteurPosition } from '../../../shared/components/selecteur-position/selecteur-position';

// Page d'inscription self-service d'un pharmacien : crée le compte ET la fiche pharmacie
// en un seul envoi (POST /api/auth/register, transaction "tout ou rien" côté backend),
// puis connecte directement l'utilisateur. La fiche reste inactive jusqu'à validation par
// un superviseur (RG-05). Sélection en cascade zone -> quartier, comme dans Formulaire.
// La position de la pharmacie se choisit sur une carte (SelecteurPosition), jamais à la main,
// et elle est obligatoire (RG-14) : la carte s'ouvre sur le quartier choisi, et changer de
// quartier retire l'ancien repère.
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, InputTextModule, PasswordModule, Select, MessageModule, SelecteurPosition],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class Register implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private referentielService = inject(ReferentielService);
  private router = inject(Router);

  // Liste complète des quartiers, chargée une fois : jamais affichée telle quelle,
  // seulement filtrée par zone dans quartiersFiltres (donc pas besoin d'un signal)
  private tousLesQuartiers: Quartier[] = [];

  zones = signal<{ id: number; nom: string }[]>([]);
  quartiersFiltres = signal<Quartier[]>([]);

  // Position choisie sur la carte (null tant que l'utilisateur n'a rien placé). Un signal
  // et non un champ du formulaire : elle pilote l'affichage de la carte (mode zoneless).
  position = signal<PositionGps | null>(null);

  // Centre du quartier choisi : la carte s'ouvre dessus (null si aucun quartier ou centre inconnu)
  centreCarte = signal<PositionGps | null>(null);

  // Vrai quand l'utilisateur a tenté d'envoyer le formulaire sans avoir placé sa pharmacie
  positionManquante = signal(false);

  envoiEnCours = signal(false);
  // Erreur métier propre au champ email (409 : compte déjà existant), affichée sous ce champ
  erreurEmail = signal<string | null>(null);
  messageErreur = signal<string | null>(null);

  formulaire = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    nom: ['', Validators.required],
    adresse: ['', Validators.required],
    zoneId: this.fb.control<number | null>(null, Validators.required),
    quartierId: this.fb.control<number | null>(null, Validators.required),
    telephone: [''],
    horaireOuverture: ['08:00'],
    horaireFermeture: ['23:00'],
    numeroAgrementOrdre: ['', Validators.required]
  });

  ngOnInit(): void {
    this.referentielService.listQuartiers().subscribe({
      next: (donnees) => {
        this.tousLesQuartiers = donnees;

        // Une zone n'a pas d'endpoint propre : on la déduit des quartiers (une entrée par zone)
        const zonesUniques = new Map<number, string>();
        donnees.forEach(q => zonesUniques.set(q.zoneId, q.nomZone));
        this.zones.set(
          Array.from(zonesUniques, ([id, nom]) => ({ id, nom }))
            .sort((a, b) => a.nom.localeCompare(b.nom))
        );
      },
      error: () => {
        this.messageErreur.set('Impossible de charger les zones et quartiers. Rechargez la page.');
      }
    });

    // Cascade : changer de zone filtre les quartiers proposés et vide le quartier choisi
    this.formulaire.controls.zoneId.valueChanges.subscribe((zoneId) => {
      this.quartiersFiltres.set(
        zoneId != null ? this.tousLesQuartiers.filter(q => q.zoneId === zoneId) : []
      );
      this.formulaire.controls.quartierId.setValue(null);
    });

    // Changer de quartier (ou le vider) : l'ancienne position n'a plus de sens, on la retire,
    // et la carte s'ouvre sur le centre du nouveau quartier (s'il est connu)
    this.formulaire.controls.quartierId.valueChanges.subscribe((quartierId) => {
      this.position.set(null);
      const quartier = this.tousLesQuartiers.find(q => q.id === quartierId);
      this.centreCarte.set(
        quartier?.latitude != null && quartier?.longitude != null
          ? { latitude: quartier.latitude, longitude: quartier.longitude }
          : null
      );
    });

    // Dès que l'utilisateur corrige son email, l'erreur "compte déjà existant" n'a plus lieu d'être
    this.formulaire.controls.email.valueChanges.subscribe(() => this.erreurEmail.set(null));
  }

  // Appelé par le sélecteur quand l'utilisateur clique, glisse le repère ou utilise sa position
  onPositionChange(position: PositionGps): void {
    this.position.set(position);
    this.positionManquante.set(false);
  }

  onSubmit(): void {
    // La position est obligatoire (RG-14) : on l'affiche comme les autres champs manquants,
    // en même temps qu'eux, plutôt que de laisser le serveur la refuser
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
    this.erreurEmail.set(null);
    this.messageErreur.set(null);

    // Le rôle n'est jamais envoyé : le backend force PHARMACIEN
    const requete: RegisterRequest = {
      email: v.email.trim(),
      password: v.password,
      pharmacie: {
        nom: v.nom.trim(),
        adresse: v.adresse.trim(),
        quartierId: v.quartierId,
        latitude: position.latitude,
        longitude: position.longitude,
        telephone: v.telephone.trim() || null,
        horaireOuverture: v.horaireOuverture.trim() || null,
        horaireFermeture: v.horaireFermeture.trim() || null,
        numeroAgrementOrdre: v.numeroAgrementOrdre.trim()
      }
    };

    this.authService.register(requete).subscribe({
      next: () => {
        this.envoiEnCours.set(false);
        // Même destination que Login (le routage par rôle reste à faire, dette n°15)
        this.router.navigate(['/dashboard']);
      },
      error: (err: HttpErrorResponse) => {
        this.envoiEnCours.set(false);
        // Vrai message métier du backend (GlobalExceptionHandler) plutôt qu'un texte générique
        const message: string | undefined = err.error?.message;
        if (err.status === 409) {
          this.erreurEmail.set(message ?? 'Un compte existe déjà avec cet email.');
        } else {
          this.messageErreur.set(message ?? "Erreur lors de l'inscription. Réessayez.");
        }
      }
    });
  }
}