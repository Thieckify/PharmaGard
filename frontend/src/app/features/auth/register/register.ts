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

// Page d'inscription self-service d'un pharmacien : crée le compte ET la fiche pharmacie
// en un seul envoi (POST /api/auth/register, transaction "tout ou rien" côté backend),
// puis connecte directement l'utilisateur. La fiche reste inactive jusqu'à validation par
// un superviseur (RG-05). Sélection en cascade zone -> quartier, comme dans Formulaire.
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, InputTextModule, PasswordModule, Select, MessageModule],
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

    // Dès que l'utilisateur corrige son email, l'erreur "compte déjà existant" n'a plus lieu d'être
    this.formulaire.controls.email.valueChanges.subscribe(() => this.erreurEmail.set(null));
  }

  onSubmit(): void {
    if (this.formulaire.invalid) {
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

    // Le rôle n'est jamais envoyé : le backend force PHARMACIEN. Latitude/longitude restent
    // à null tant que le sélecteur de position sur carte n'est pas branché.
    const requete: RegisterRequest = {
      email: v.email.trim(),
      password: v.password,
      pharmacie: {
        nom: v.nom.trim(),
        adresse: v.adresse.trim(),
        quartierId: v.quartierId,
        latitude: null,
        longitude: null,
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