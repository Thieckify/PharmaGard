import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { Role } from '../../core/models/auth.model';
import { ReferentielService } from '../../core/services/referentiel.service';
import { PageLayout } from '../../shared/layout/page-layout/page-layout';
import { PharmacieService } from '../pharmacies/services/pharmacie.service';
import { Pharmacie } from '../pharmacies/models/pharmacie.model';
import { GardeService } from '../gardes/services/garde.service';
import { Garde, TypeGarde } from '../gardes/models/garde.model';
import { GardePublicService } from '../public/services/garde-public.service';
import { PharmacieDeGarde } from '../public/models/garde-public.model';

// Noms des mois, pour l'étiquette "jours de garde en octobre". Écrits ici pour ne pas
// dépendre de la langue configurée dans Angular
const MOIS = [
  'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
  'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre'
];

// Une ligne du tableau "gardes d'aujourd'hui, par zone" du superviseur
interface LigneZone {
  id: number;
  nom: string;
  validees: number;
  enAttente: number;
  deGarde: number;
  // ok : au moins une pharmacie de garde aujourd'hui ;
  // alerte : aucune, alors qu'une pharmacie validée existe dans la zone (le vrai problème) ;
  // vide : aucune pharmacie validée dans la zone, donc aucune garde possible
  couverture: 'ok' | 'alerte' | 'vide';
}

/**
 * Tableau de bord : point d'entrée après la connexion. Pour un PHARMACIEN, il résume sa fiche
 * (statut de validation), sa prochaine garde et ses chiffres de garde (périodes à venir, jours
 * de garde du mois et de l'année), avec des raccourcis vers "Ma fiche" et "Mon planning".
 * Pour un SUPERVISEUR, il donne des compteurs sur ses fiches et la couverture de garde du jour
 * zone par zone. Pour l'admin, il affiche les infos du compte.
 * C'est une page d'agrégation : elle lit les services de plusieurs features, et tous les
 * chiffres sont calculés ici à partir de listes déjà chargées (aucun endpoint dédié).
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, RouterLink, PageLayout],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
  private authService = inject(AuthService);
  private pharmacieService = inject(PharmacieService);
  private gardeService = inject(GardeService);
  private gardePublicService = inject(GardePublicService);
  private referentielService = inject(ReferentielService);

  utilisateur = this.authService.currentUser;

  estPharmacien = computed(() => this.utilisateur()?.role === 'PHARMACIEN');
  estSuperviseur = computed(() => this.utilisateur()?.role === 'SUPERVISEUR');

  chargement = signal(true);
  erreur = signal<string | null>(null);

  // ---- Pharmacien : sa fiche et son planning ----

  // Fiche du pharmacien (null s'il n'en a pas encore créé)
  fiche = signal<Pharmacie | null>(null);

  // Planning du pharmacien, chargé seulement s'il a une fiche
  gardes = signal<Garde[]>([]);
  chargementPlanning = signal(false);
  erreurPlanning = signal(false);

  // Date du jour au format "AAAA-MM-JJ", construite à partir des valeurs locales (pas de
  // toISOString(), qui peut décaler d'un jour selon le fuseau). Ce format se compare
  // directement, comme du texte, avec les dates renvoyées par le serveur.
  private aujourdhui = this.dateDuJour();

  // Année, mois (de 1 à 12) et bornes du mois en cours, tirés de la date du jour
  private annee = Number(this.aujourdhui.slice(0, 4));
  private mois = Number(this.aujourdhui.slice(5, 7));
  private debutMois = `${this.annee}-${String(this.mois).padStart(2, '0')}-01`;
  // new Date(annee, mois, 0) : le jour 0 du mois suivant est le dernier jour du mois en cours
  private finMois = `${this.annee}-${String(this.mois).padStart(2, '0')}-${String(new Date(this.annee, this.mois, 0).getDate()).padStart(2, '0')}`;

  // Affichés dans les étiquettes des compteurs ("jours de garde en octobre", "... en 2026")
  nomDuMois = MOIS[this.mois - 1];
  anneeCourante = this.annee;

  // Périodes pas encore terminées, de la plus proche à la plus lointaine
  private periodesAVenir = computed(() =>
    this.gardes()
      .filter(g => g.dateFin >= this.aujourdhui)
      .sort((a, b) => a.dateDebut.localeCompare(b.dateDebut))
  );

  // La période en cours, ou à défaut la prochaine
  prochaineGarde = computed(() => this.periodesAVenir()[0] ?? null);

  enCours = computed(() => {
    const garde = this.prochaineGarde();
    return garde != null && garde.dateDebut <= this.aujourdhui;
  });

  // Nombre de périodes à venir en plus de celle affichée
  autresPeriodes = computed(() => Math.max(this.periodesAVenir().length - 1, 0));

  // Chiffres de garde du pharmacien, calculés à partir du planning déjà chargé
  nbGardesAVenir = computed(() => this.periodesAVenir().length);
  joursGardeMois = computed(() => this.joursDeGardeEntre(this.debutMois, this.finMois));
  joursGardeAnnee = computed(() => this.joursDeGardeEntre(`${this.annee}-01-01`, `${this.annee}-12-31`));

  // ---- Superviseur : ses fiches et la couverture de garde du jour ----

  // Fiches visibles du superviseur (sa zone, ou toutes s'il est général : le serveur filtre)
  pharmacies = signal<Pharmacie[]>([]);

  // Zones connues (déduites des quartiers) et pharmacies de garde aujourd'hui (endpoint public)
  zones = signal<{ id: number; nom: string }[]>([]);
  gardesAujourdhui = signal<PharmacieDeGarde[]>([]);
  chargementCouverture = signal(true);
  erreurCouverture = signal(false);

  nbEnAttente = computed(() => this.pharmacies().filter(p => !p.active).length);
  nbValidees = computed(() => this.pharmacies().filter(p => p.active).length);

  // Fiches sans position : elles ne peuvent pas figurer sur la carte
  nbSansPosition = computed(
    () => this.pharmacies().filter(p => p.latitude == null || p.longitude == null).length
  );

  // Fiches actuellement validées dont la validation date de moins de 7 jours. valideLe est une
  // date sans fuseau ("2026-09-28T19:44:06.560394") : on garde les 19 premiers caractères
  // (jusqu'aux secondes) pour que tous les navigateurs la lisent de la même façon.
  nbValideesSemaine = computed(() => {
    const limite = Date.now() - 7 * 24 * 60 * 60 * 1000;
    return this.pharmacies().filter(
      p => p.active && p.valideLe != null && new Date(p.valideLe.slice(0, 19)).getTime() >= limite
    ).length;
  });

  // Une ligne par zone : le superviseur général les voit toutes, celui d'une zone seulement la sienne
  lignesZones = computed<LigneZone[]>(() => {
    const zoneDuSuperviseur = this.utilisateur()?.zoneId ?? null;
    const pharmacies = this.pharmacies();
    const gardes = this.gardesAujourdhui();

    return this.zones()
      .filter(z => zoneDuSuperviseur == null || z.id === zoneDuSuperviseur)
      .map((z): LigneZone => {
        const validees = pharmacies.filter(p => p.zoneId === z.id && p.active).length;
        const enAttente = pharmacies.filter(p => p.zoneId === z.id && !p.active).length;
        // L'endpoint public ne renvoie que le nom de la zone : on compte par nom
        const deGarde = gardes.filter(g => g.nomZone === z.nom).length;
        const couverture: LigneZone['couverture'] = deGarde > 0 ? 'ok' : validees > 0 ? 'alerte' : 'vide';
        return { id: z.id, nom: z.nom, validees, enAttente, deGarde, couverture };
      });
  });

  // Plus grand nombre de fiches dans une zone : sert d'échelle commune aux barres
  maxParZone = computed(() => Math.max(1, ...this.lignesZones().map(l => l.validees + l.enAttente)));

  // Noms des zones sans aucune pharmacie de garde aujourd'hui (alors qu'elles en ont de validées)
  zonesSansGarde = computed(() => this.lignesZones().filter(l => l.couverture === 'alerte').map(l => l.nom));

  // ---- Bandeau du layout ----

  titre = computed(() => (this.estPharmacien() && this.fiche() ? 'Bonjour,' : 'Bienvenue sur'));
  titreAccent = computed(() => (this.estPharmacien() ? (this.fiche()?.nom ?? 'PharmaGarde') : 'PharmaGarde'));

  sousTitre = computed(() => {
    if (this.estPharmacien()) {
      const fiche = this.fiche();
      if (!fiche) {
        return 'Créez la fiche de votre pharmacie pour apparaître sur le site.';
      }
      return fiche.active
        ? 'Votre fiche est visible sur le site. Voici l\'essentiel en un coup d\'œil.'
        : 'Votre fiche attend la validation d\'un superviseur.';
    }

    if (this.estSuperviseur()) {
      const zoneId = this.utilisateur()?.zoneId ?? null;
      if (zoneId == null) {
        return 'Toutes les zones : les fiches à contrôler et les gardes du jour.';
      }
      const zone = this.zones().find(z => z.id === zoneId);
      return zone
        ? `Zone « ${zone.nom} » : les fiches à contrôler et les gardes du jour.`
        : 'Votre zone : les fiches à contrôler et les gardes du jour.';
    }

    return 'Votre espace d\'administration.';
  });

  ngOnInit(): void {
    if (this.estPharmacien()) {
      this.chargerFiche();
    } else if (this.estSuperviseur()) {
      this.chargerSupervision();
    }
  }

  private chargerFiche(): void {
    this.pharmacieService.listPharmacies().subscribe({
      next: (fiches) => {
        // Un pharmacien n'a qu'une seule fiche (RG-01) : la liste en contient 0 ou 1
        const fiche = fiches[0] ?? null;
        this.fiche.set(fiche);
        this.chargement.set(false);
        if (fiche) {
          this.chargerPlanning();
        }
      },
      error: () => {
        this.erreur.set('Impossible de charger votre tableau de bord.');
        this.chargement.set(false);
      }
    });
  }

  // Le planning est une information secondaire : s'il ne se charge pas, la carte de la
  // fiche reste affichée et la carte "Prochaine garde" le signale
  private chargerPlanning(): void {
    this.chargementPlanning.set(true);
    this.gardeService.listMesGardes().subscribe({
      next: (gardes) => {
        this.gardes.set(gardes);
        this.chargementPlanning.set(false);
      },
      error: () => {
        this.erreurPlanning.set(true);
        this.chargementPlanning.set(false);
      }
    });
  }

  // Deux chargements indépendants : si la couverture de garde échoue, les compteurs restent
  // affichés, et inversement
  private chargerSupervision(): void {
    this.pharmacieService.listPharmacies().subscribe({
      next: (pharmacies) => {
        this.pharmacies.set(pharmacies);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger votre tableau de bord.');
        this.chargement.set(false);
      }
    });

    forkJoin({
      quartiers: this.referentielService.listQuartiers(),
      gardes: this.gardePublicService.listPharmaciesDeGarde()
    }).subscribe({
      next: ({ quartiers, gardes }) => {
        // Une zone n'a pas d'endpoint propre : on la déduit des quartiers (une entrée par zone)
        const zonesUniques = new Map<number, string>();
        quartiers.forEach(q => zonesUniques.set(q.zoneId, q.nomZone));
        this.zones.set(
          Array.from(zonesUniques, ([id, nom]) => ({ id, nom }))
            .sort((a, b) => a.nom.localeCompare(b.nom))
        );
        this.gardesAujourdhui.set(gardes);
        this.chargementCouverture.set(false);
      },
      error: () => {
        this.erreurCouverture.set(true);
        this.chargementCouverture.set(false);
      }
    });
  }

  // Nombre de jours de garde déclarés entre deux dates (incluses). Pour chaque période, on
  // compte les jours qui tombent dans l'intervalle : une période du 29 au 3 du mois suivant
  // ne compte, dans le mois, que ses jours de ce mois. Les périodes ne se chevauchent pas
  // (RG-03), donc aucun jour n'est compté deux fois.
  private joursDeGardeEntre(debut: string, fin: string): number {
    const borneDebut = this.numeroDeJour(debut);
    const borneFin = this.numeroDeJour(fin);

    return this.gardes().reduce((total, garde) => {
      const du = Math.max(this.numeroDeJour(garde.dateDebut), borneDebut);
      const au = Math.min(this.numeroDeJour(garde.dateFin), borneFin);
      return total + Math.max(0, au - du + 1);
    }, 0);
  }

  // Transforme une date "AAAA-MM-JJ" en numéro de jour, pour calculer des écarts en jours.
  // Le calcul se fait en UTC pour ne jamais être faussé par un changement d'heure.
  private numeroDeJour(dateIso: string): number {
    const [annee, mois, jour] = dateIso.split('-').map(Number);
    return Math.floor(Date.UTC(annee, mois - 1, jour) / 86_400_000);
  }

  libelleType(type: TypeGarde): string {
    switch (type) {
      case 'JOUR': return 'Garde de jour';
      case 'NUIT': return 'Garde de nuit';
      case 'COMPLETE': return 'Garde complète';
      default: return type;
    }
  }

  libelleRole(role: Role): string {
    switch (role) {
      case 'PHARMACIEN': return 'Pharmacien';
      case 'SUPERVISEUR': return 'Superviseur';
      case 'ADMIN': return 'Administrateur';
      default: return role;
    }
  }

  private dateDuJour(): string {
    const date = new Date();
    const annee = date.getFullYear();
    const mois = String(date.getMonth() + 1).padStart(2, '0');
    const jour = String(date.getDate()).padStart(2, '0');
    return `${annee}-${mois}-${jour}`;
  }
}