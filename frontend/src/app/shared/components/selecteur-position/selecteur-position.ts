import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, effect, inject, input, output, signal } from '@angular/core';
import * as L from 'leaflet';
import { GeolocalisationService } from '../../../core/services/geolocalisation.service';
import { ICONE_MARQUEUR } from '../../utils/icone-marqueur';

// Position GPS choisie par l'utilisateur
export interface PositionGps {
  latitude: number;
  longitude: number;
}

// Sélecteur de position sur carte : l'utilisateur clique sur la carte, fait glisser le
// repère, ou part de sa position actuelle. Composant "contrôlé" : il ne mémorise rien,
// le formulaire parent garde la position (dans un signal) et la lui redonne en entrée ;
// la carte prévient le parent par positionChange dès que l'utilisateur en choisit une.
// L'entrée "centre" permet au parent d'ouvrir la carte sur un endroit précis (ex. le
// centre du quartier choisi) sans placer de repère.
@Component({
  selector: 'app-selecteur-position',
  standalone: true,
  templateUrl: './selecteur-position.html',
  styleUrl: './selecteur-position.scss'
})
export class SelecteurPosition implements AfterViewInit, OnDestroy {
  @ViewChild('conteneur', { static: true }) conteneurRef!: ElementRef<HTMLDivElement>;

  latitude = input<number | null>(null);
  longitude = input<number | null>(null);
  centre = input<PositionGps | null>(null);
  positionChange = output<PositionGps>();

  private geolocalisationService = inject(GeolocalisationService);

  chargementPosition = signal(false);
  erreurPosition = signal<string | null>(null);

  private carte: L.Map | null = null;
  private marqueur: L.Marker | null = null;

  // Dernière position émise par ce composant : sert à reconnaître l'écho du parent
  // (il nous la renvoie en entrée) et à ne recentrer la carte que si la position vient
  // vraiment de l'extérieur (ex. une fiche existante chargée), pas d'un clic de l'utilisateur.
  private derniereEmission: PositionGps | null = null;

  constructor() {
    // Redessine le repère chaque fois que le parent change la position
    effect(() => {
      this.afficherPosition(this.latitude(), this.longitude());
    });

    // Ouvre la carte sur le centre donné par le parent, chaque fois qu'il change
    effect(() => {
      this.recentrer(this.centre());
    });
  }

  ngAfterViewInit(): void {
    // Centrée par défaut sur Dakar, jusqu'à ce qu'une position ou un centre soit donné
    this.carte = L.map(this.conteneurRef.nativeElement).setView([14.6928, -17.4467], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19
    }).addTo(this.carte);

    this.carte.on('click', (e: L.LeafletMouseEvent) => this.choisir(e.latlng.lat, e.latlng.lng));

    this.afficherPosition(this.latitude(), this.longitude());
    this.recentrer(this.centre());
  }

  ngOnDestroy(): void {
    this.carte?.remove();
  }

  // Part de la position actuelle du visiteur (popup d'autorisation du navigateur).
  // Ce n'est qu'un point de départ : le repère reste déplaçable.
  async utiliserMaPosition(): Promise<void> {
    this.chargementPosition.set(true);
    this.erreurPosition.set(null);

    try {
      const coords = await this.geolocalisationService.obtenirPosition();
      this.choisir(coords.latitude, coords.longitude);
      this.carte?.setView([coords.latitude, coords.longitude], 17);
    } catch {
      this.erreurPosition.set('Position refusée ou indisponible. Cliquez directement sur la carte pour placer votre pharmacie.');
    } finally {
      this.chargementPosition.set(false);
    }
  }

  // Enregistre un choix de l'utilisateur et prévient le parent
  private choisir(latitude: number, longitude: number): void {
    // 6 décimales = précision d'environ 10 cm, et évite des valeurs comme 14.716700000000001
    const position: PositionGps = {
      latitude: Number(latitude.toFixed(6)),
      longitude: Number(longitude.toFixed(6))
    };
    this.derniereEmission = position;
    this.erreurPosition.set(null);
    this.positionChange.emit(position);
  }

  // Ouvre la carte sur le centre donné (zoom 14 : de quoi voir un quartier entier, l'utilisateur
  // zoomera ensuite pour placer le repère). Sans centre, on ne touche à rien.
  private recentrer(centre: PositionGps | null): void {
    if (!this.carte || !centre) {
      return;
    }
    this.carte.setView([centre.latitude, centre.longitude], 14);
  }

  // Place, déplace ou retire le repère selon la position reçue du parent
  private afficherPosition(lat: number | null, lng: number | null): void {
    if (!this.carte) {
      return;
    }

    if (lat == null || lng == null) {
      this.marqueur?.remove();
      this.marqueur = null;
      this.derniereEmission = null;
      return;
    }

    const point = L.latLng(lat, lng);

    if (this.marqueur) {
      this.marqueur.setLatLng(point);
    } else {
      this.marqueur = this.creerMarqueur(this.carte, point);
    }

    const estUnEcho =
      this.derniereEmission?.latitude === lat && this.derniereEmission?.longitude === lng;
    if (!estUnEcho) {
      this.carte.setView(point, 16);
    }
  }

  // Repère déplaçable : à la fin du glissement, la nouvelle position est enregistrée
  private creerMarqueur(carte: L.Map, point: L.LatLng): L.Marker {
    const marqueur = L.marker(point, { icon: ICONE_MARQUEUR, draggable: true }).addTo(carte);
    marqueur.on('dragend', () => {
      const p = marqueur.getLatLng();
      this.choisir(p.lat, p.lng);
    });
    return marqueur;
  }
}