import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { GardePublicService } from '../services/garde-public.service';
import { PharmacieDeGarde } from '../models/garde-public.model';
import { PharmaciePubliqueService } from '../services/pharmacie-publique.service';
import { PharmaciePublique } from '../models/pharmacie-publique.model';
import { GeolocalisationService } from '../../../core/services/geolocalisation.service';
import { ReferentielService } from '../../../core/services/referentiel.service';
import { Quartier } from '../../../core/models/referentiel.model';
import { trierParDistance, filtrerParRayon, RAYON_PROXIMITE_KM } from '../utils/distance.util';
import { CartePharmacies } from '../../../shared/components/carte-pharmacies/carte-pharmacies';
import { ChatAssistant } from '../../../shared/components/chat-assistant/chat-assistant';

interface Coordonnees {
  latitude: number;
  longitude: number;
}

// Page d'accueil publique de PharmaGarde : accessible sans authentification (RG-04).
// Deux modes de recherche, dans le même bloc "recherche" : par date (pharmacies de
// garde, RG-03/04/05) et par quartier (toutes les pharmacies actives, RG-05) —
// intentionnellement deux périmètres différents (cf. dette n°13 CONTEXT.md). Chaque
// mode a sa propre carte Leaflet et son propre repli de géolocalisation. Règle commune
// aux deux modes : géolocalisation et filtre quartier sont MUTUELLEMENT EXCLUSIFS —
// utiliser sa position doit toujours chercher la plus proche parmi TOUT, jamais
// restreinte à un quartier choisi précédemment ; choisir un quartier après coup annule
// la géolocalisation active.
@Component({
  selector: 'app-pharmacies-de-garde',
  standalone: true,
  imports: [FormsModule, RouterLink, DatePicker, Select, DecimalPipe, CartePharmacies, ChatAssistant],
  templateUrl: './pharmacies-de-garde.html',
  styleUrl: './pharmacies-de-garde.scss'
})
export class PharmaciesDeGarde implements OnInit {
  private gardePublicService = inject(GardePublicService);
  private pharmaciePubliqueService = inject(PharmaciePubliqueService);
  private referentielService = inject(ReferentielService);
  private geolocalisationService = inject(GeolocalisationService);

  // Exposé au template, qui ne peut pas importer une constante directement
  readonly rayonProximiteKm = RAYON_PROXIMITE_KM;

  // Onglet actif : pilote le contrôle affiché dans .recherche ET le bloc de résultats
  modeRecherche = signal<'date' | 'quartier'>('date');

  // Référentiel des quartiers, chargé une seule fois : sert au menu déroulant de
  // l'onglet "quartier" (recherche serveur) ET au filtre de repli de l'onglet "date"
  // (filtre client, cf. commentaire sur nomQuartierFiltreDate)
  quartiers = signal<Quartier[]>([]);

  // -- Recherche par date (pharmacies de garde) --
  dateSelectionnee = signal<Date>(new Date());
  pharmacies = signal<PharmacieDeGarde[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);
  quartierFiltreDate = signal<number | null>(null);
  positionCoordsDate = signal<Coordonnees | null>(null);
  chargementPositionDate = signal(false);
  erreurPositionDate = signal<string | null>(null);

  // Nom du quartier choisi en repli (pour filtrer pharmacies() par nomQuartier, seul
  // champ dont dispose PharmacieDeGardeResponse — pas de quartierId sur ce DTO)
  private nomQuartierFiltreDate = computed(() => {
    const id = this.quartierFiltreDate();
    return id != null ? (this.quartiers().find((q) => q.id === id)?.nom ?? null) : null;
  });

  // Pharmacies de garde après filtre quartier (repli manuel), avant tri par proximité
  private pharmaciesFiltreesDate = computed(() => {
    const nom = this.nomQuartierFiltreDate();
    return nom ? this.pharmacies().filter((p) => p.nomQuartier === nom) : this.pharmacies();
  });

  // Résultat final affiché pour l'onglet "date" : combine filtre quartier + tri/filtre
  // par proximité (si la position a été utilisée) — mutuellement exclusifs en pratique
  // (cf. onQuartierFiltreDateChange / utiliserMaPositionDate), mais ce calcul reste
  // correct même si l'un des deux seulement est actif. Un seul point de calcul, pour ne
  // jamais désynchroniser la liste, le message d'entête et la carte.
  resultatDate = computed(() => {
    const base = this.pharmaciesFiltreesDate();
    const coords = this.positionCoordsDate();

    if (!coords) {
      return { liste: base, rayonDepasse: false };
    }

    const trie = trierParDistance(base, coords.latitude, coords.longitude);
    const proches = filtrerParRayon(trie, RAYON_PROXIMITE_KM);

    return proches.length > 0
      ? { liste: proches, rayonDepasse: false }
      : { liste: trie, rayonDepasse: trie.length > 0 };
  });

  // -- Recherche par quartier (toutes les pharmacies actives) --
  quartierSelectionne = signal<number | null>(null);
  pharmaciesPubliques = signal<PharmaciePublique[]>([]);
  chargementPharmacies = signal(true);
  erreurPharmacies = signal<string | null>(null);
  positionActiveeQuartier = signal(false);
  chargementPositionQuartier = signal(false);
  erreurPositionQuartier = signal<string | null>(null);
  rayonDepasseQuartier = signal(false);

  ngOnInit(): void {
    this.chargerGardes();
    this.chargerQuartiers();
  }

  onModeChange(mode: 'date' | 'quartier'): void {
    this.modeRecherche.set(mode);
    if (mode === 'quartier' && this.pharmaciesPubliques().length === 0) {
      this.chargerPharmaciesPubliques();
    }
  }

  onDateChange(date: Date): void {
    this.dateSelectionnee.set(date);
    this.quartierFiltreDate.set(null);
    this.positionCoordsDate.set(null);
    this.erreurPositionDate.set(null);
    this.chargerGardes();
  }

  // Filtre de repli manuel (sans backend). Choisir un quartier annule toute
  // géolocalisation active : les deux ne se combinent jamais (cf. commentaire de classe).
  onQuartierFiltreDateChange(quartierId: number | null): void {
    this.quartierFiltreDate.set(quartierId);
    this.positionCoordsDate.set(null);
    this.erreurPositionDate.set(null);
  }

  // quartierId à null : le p-select a été vidé (showClear), on revient à "toutes les
  // pharmacies actives". Choisir un quartier annule toute géolocalisation active.
  onQuartierChange(quartierId: number | null): void {
    this.quartierSelectionne.set(quartierId);
    this.positionActiveeQuartier.set(false);
    this.erreurPositionQuartier.set(null);
    this.rayonDepasseQuartier.set(false);
    this.chargerPharmaciesPubliques();
  }

  // Géolocalisation pour la recherche par date : annule d'abord un éventuel filtre
  // quartier (la position prime, cf. commentaire de classe), puis mémorise la position —
  // le calcul de tri/filtre est entièrement délégué à resultatDate() (computed).
  async utiliserMaPositionDate(): Promise<void> {
    this.chargementPositionDate.set(true);
    this.erreurPositionDate.set(null);

    try {
      const coords = await this.geolocalisationService.obtenirPosition();
      this.quartierFiltreDate.set(null);
      this.positionCoordsDate.set({ latitude: coords.latitude, longitude: coords.longitude });
    } catch {
      this.erreurPositionDate.set('Position refusée ou indisponible. Filtrez par quartier ci-dessous.');
    } finally {
      this.chargementPositionDate.set(false);
    }
  }

  // Géolocalisation pour la recherche par quartier : annule d'abord un éventuel
  // quartier choisi et recharge la liste COMPLÈTE des pharmacies actives, pour être
  // sûr de trouver la plus proche réellement — pas seulement la plus proche du
  // quartier sélectionné avant.
  async utiliserMaPositionQuartier(): Promise<void> {
    this.chargementPositionQuartier.set(true);
    this.erreurPositionQuartier.set(null);

    try {
      const coords = await this.geolocalisationService.obtenirPosition();
      this.quartierSelectionne.set(null);

      const toutes = await firstValueFrom(this.pharmaciePubliqueService.rechercher());
      const trie = trierParDistance(toutes, coords.latitude, coords.longitude);
      const proches = filtrerParRayon(trie, RAYON_PROXIMITE_KM);

      if (proches.length > 0) {
        this.pharmaciesPubliques.set(proches);
        this.rayonDepasseQuartier.set(false);
      } else {
        this.pharmaciesPubliques.set(trie);
        this.rayonDepasseQuartier.set(true);
      }
      this.positionActiveeQuartier.set(true);
    } catch {
      this.erreurPositionQuartier.set('Position refusée ou indisponible. Choisissez votre quartier ci-dessous.');
    } finally {
      this.chargementPositionQuartier.set(false);
    }
  }

  private chargerGardes(): void {
    this.chargement.set(true);
    this.erreur.set(null);
    const dateFormatee = this.formatDate(this.dateSelectionnee());

    this.gardePublicService.listPharmaciesDeGarde(dateFormatee).subscribe({
      next: (resultat) => {
        this.pharmacies.set(resultat);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger les pharmacies de garde pour le moment.');
        this.chargement.set(false);
      }
    });
  }

  private chargerQuartiers(): void {
    this.referentielService.listQuartiers().subscribe({
      next: (resultat) => this.quartiers.set(resultat),
      // Si le référentiel échoue, les deux menus déroulants restent vides mais les
      // deux recherches restent utilisables sans filtre
      error: () => {}
    });
  }

  private chargerPharmaciesPubliques(): void {
    this.chargementPharmacies.set(true);
    this.erreurPharmacies.set(null);
    const quartierId = this.quartierSelectionne() ?? undefined;

    this.pharmaciePubliqueService.rechercher(quartierId).subscribe({
      next: (resultat) => {
        this.pharmaciesPubliques.set(resultat);
        this.chargementPharmacies.set(false);
      },
      error: () => {
        this.erreurPharmacies.set('Impossible de charger les pharmacies pour le moment.');
        this.chargementPharmacies.set(false);
      }
    });
  }

  // Formate une Date en "YYYY-MM-DD" à partir des valeurs LOCALES (jour/mois/année) —
  // volontairement PAS date.toISOString(), qui convertit en UTC et peut faire glisser
  // la date d'un jour selon le fuseau horaire du visiteur.
  private formatDate(date: Date): string {
    const annee = date.getFullYear();
    const mois = String(date.getMonth() + 1).padStart(2, '0');
    const jour = String(date.getDate()).padStart(2, '0');
    return `${annee}-${mois}-${jour}`;
  }

  libelleType(type: string): string {
    switch (type) {
      case 'JOUR': return 'Garde de jour';
      case 'NUIT': return 'Garde de nuit';
      case 'COMPLETE': return 'Garde complète';
      default: return type;
    }
  }

  // Lien "tel:" pour déclencher un appel direct depuis un mobile.
  // On retire les espaces : un numéro affiché "+221 33 821 00 01" doit devenir "+221338210001".
  lienAppel(telephone: string): string {
    return `tel:${telephone.replace(/\s/g, '')}`;
  }

  // Ouvre Google Maps aux coordonnées GPS de la pharmacie (itinéraire depuis la position du visiteur)
  lienItineraire(pharmacie: PharmacieDeGarde): string {
    return `https://www.google.com/maps/dir/?api=1&destination=${pharmacie.latitude},${pharmacie.longitude}`;
  }

  // Même principe pour une pharmacie de la recherche par quartier — appelé uniquement
  // quand les coordonnées existent (coordonnées facultatives, cf. template)
  lienItinerairePublique(pharmacie: PharmaciePublique): string {
    return `https://www.google.com/maps/dir/?api=1&destination=${pharmacie.latitude},${pharmacie.longitude}`;
  }
}