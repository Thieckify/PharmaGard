import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { GardePublicService } from '../services/garde-public.service';
import { PharmacieDeGarde } from '../models/garde-public.model';
import { PharmaciePubliqueService } from '../services/pharmacie-publique.service';
import { PharmaciePublique } from '../models/pharmacie-publique.model';
import { GeolocalisationService } from '../services/geolocalisation.service';
import { ReferentielService } from '../../../core/services/referentiel.service';
import { Quartier } from '../../../core/models/referentiel.model';
import { trierParDistance } from '../utils/distance.util';

// Page d'accueil publique de PharmaGarde : accessible sans authentification (RG-04).
// Deux modes de recherche, dans le même bloc "recherche" : par date (pharmacies de
// garde) et par quartier (toutes les pharmacies actives, RG-05, avec option de tri
// par proximité via géolocalisation). Un seul mode de résultats est affiché à la fois,
// selon l'onglet actif.
@Component({
  selector: 'app-pharmacies-de-garde',
  standalone: true,
  imports: [FormsModule, RouterLink, DatePicker, Select, DecimalPipe],
  templateUrl: './pharmacies-de-garde.html',
  styleUrl: './pharmacies-de-garde.scss'
})
export class PharmaciesDeGarde implements OnInit {
  private gardePublicService = inject(GardePublicService);
  private pharmaciePubliqueService = inject(PharmaciePubliqueService);
  private referentielService = inject(ReferentielService);
  private geolocalisationService = inject(GeolocalisationService);

  // Onglet actif : pilote le contrôle affiché dans .recherche ET le bloc de résultats
  modeRecherche = signal<'date' | 'quartier'>('date');

  // -- Recherche par date (pharmacies de garde) --
  dateSelectionnee = signal<Date>(new Date());
  pharmacies = signal<PharmacieDeGarde[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);

  // -- Recherche par quartier (toutes les pharmacies actives) --
  quartiers = signal<Quartier[]>([]);
  quartierSelectionne = signal<number | null>(null);
  pharmaciesPubliques = signal<PharmaciePublique[]>([]);
  chargementPharmacies = signal(true);
  erreurPharmacies = signal<string | null>(null);

  // -- Géolocalisation --
  positionActivee = signal(false);
  chargementPosition = signal(false);
  erreurPosition = signal<string | null>(null);

  ngOnInit(): void {
    this.chargerGardes();
  }

  // Bascule entre les deux modes. Les données de la recherche par quartier ne sont
  // chargées qu'au premier passage sur cet onglet (inutile si le visiteur ne l'utilise jamais).
  onModeChange(mode: 'date' | 'quartier'): void {
    this.modeRecherche.set(mode);
    if (mode === 'quartier' && this.quartiers().length === 0) {
      this.chargerQuartiers();
      this.chargerPharmaciesPubliques();
    }
  }

  onDateChange(date: Date): void {
    this.dateSelectionnee.set(date);
    this.chargerGardes();
  }

  // quartierId à null : le p-select a été vidé (showClear), on revient à "toutes les pharmacies actives"
  onQuartierChange(quartierId: number | null): void {
    this.quartierSelectionne.set(quartierId);
    this.positionActivee.set(false);
    this.erreurPosition.set(null);
    this.chargerPharmaciesPubliques();
  }

  // Demande la position du visiteur via GeolocalisationService, puis trie les
  // pharmacies déjà chargées par distance croissante (fonction pure de distance.util).
  async utiliserMaPosition(): Promise<void> {
    this.chargementPosition.set(true);
    this.erreurPosition.set(null);

    try {
      const coords = await this.geolocalisationService.obtenirPosition();
      const trie = trierParDistance(this.pharmaciesPubliques(), coords.latitude, coords.longitude);
      this.pharmaciesPubliques.set(trie);
      this.positionActivee.set(true);
    } catch {
      this.erreurPosition.set('Position refusée ou indisponible. Choisissez votre quartier ci-dessous.');
    } finally {
      this.chargementPosition.set(false);
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
      // Si le référentiel échoue, le sélecteur reste vide mais la recherche par quartier
      // reste utilisable sans filtre (toutes les pharmacies actives)
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