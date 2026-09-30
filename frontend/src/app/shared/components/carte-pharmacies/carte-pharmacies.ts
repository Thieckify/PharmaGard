import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, effect, input } from '@angular/core';
import * as L from 'leaflet';
import { LieuCarte } from '../../models/lieu-carte.model';
import { ICONE_ATTENTE, ICONE_MARQUEUR, ICONE_VALIDEE } from '../../utils/icone-marqueur';

// Affiche les lieux reçus en entrée sous forme de marqueurs sur une carte
// Leaflet/OpenStreetMap. Composant "bête" : ne fait aucun appel HTTP, se contente
// d'afficher ce qu'on lui donne. Typé sur LieuCarte (nom/adresse/latitude/longitude, et un
// statut facultatif) plutôt que sur un type de pharmacie précis : réutilisable telle quelle
// pour l'espace public (recherche par quartier et par date) et pour la vue superviseur,
// où le repère est doré pour une fiche en attente et teal pour une fiche validée.
@Component({
  selector: 'app-carte-pharmacies',
  standalone: true,
  templateUrl: './carte-pharmacies.html',
  styleUrl: './carte-pharmacies.scss'
})
export class CartePharmacies implements AfterViewInit, OnDestroy {
  @ViewChild('conteneur', { static: true }) conteneurRef!: ElementRef<HTMLDivElement>;

  pharmacies = input<LieuCarte[]>([]);

  private carte: L.Map | null = null;
  private marqueurs: L.Marker[] = [];

  constructor() {
    // Se redéclenche à chaque changement de la liste reçue (recherche, tri, filtre) :
    // on efface les anciens marqueurs et on redessine.
    effect(() => {
      this.mettreAJourMarqueurs(this.pharmacies());
    });
  }

  ngAfterViewInit(): void {
    // Centrée par défaut sur Dakar ; ajustée automatiquement dès qu'il y a des marqueurs
    this.carte = L.map(this.conteneurRef.nativeElement).setView([14.6928, -17.4467], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19
    }).addTo(this.carte);

    this.mettreAJourMarqueurs(this.pharmacies());
  }

  ngOnDestroy(): void {
    this.carte?.remove();
  }

  private mettreAJourMarqueurs(lieux: LieuCarte[]): void {
    if (!this.carte) {
      return;
    }

    this.marqueurs.forEach((m) => m.remove());
    this.marqueurs = [];

    const avecPosition = lieux.filter((l) => l.latitude != null && l.longitude != null);

    for (const lieu of avecPosition) {
      const marqueur = L.marker([lieu.latitude!, lieu.longitude!], { icon: this.iconePour(lieu) })
        .addTo(this.carte)
        .bindPopup(this.creerPopup(lieu));
      this.marqueurs.push(marqueur);
    }

    // Recentre et zoome automatiquement pour que tous les marqueurs soient visibles
    if (avecPosition.length > 0) {
      const groupe = L.featureGroup(this.marqueurs);
      this.carte.fitBounds(groupe.getBounds().pad(0.2));
    }
  }

  // Repère selon le statut : doré (en attente), teal (validée), ou repère par défaut
  private iconePour(lieu: LieuCarte): L.Icon | L.DivIcon {
    switch (lieu.statut) {
      case 'validee':
        return ICONE_VALIDEE;
      case 'attente':
        return ICONE_ATTENTE;
      default:
        return ICONE_MARQUEUR;
    }
  }

  // Contenu de la fenêtre qui s'ouvre au clic sur un repère. Construit avec des éléments et
  // textContent, jamais avec du HTML assemblé à partir des données : le nom et l'adresse sont
  // saisis par les pharmaciens, et une fiche piégée (un nom contenant du code) s'y serait
  // exécutée chez le superviseur ou le visiteur.
  private creerPopup(lieu: LieuCarte): HTMLElement {
    const conteneur = document.createElement('div');

    const nom = document.createElement('strong');
    nom.textContent = lieu.nom;

    const adresse = document.createElement('div');
    adresse.textContent = lieu.adresse;

    conteneur.append(nom, adresse);

    if (lieu.statut) {
      const statut = document.createElement('div');
      statut.textContent = lieu.statut === 'validee' ? 'Validée' : 'En attente de validation';
      statut.style.marginTop = '4px';
      statut.style.fontSize = '12px';
      statut.style.color = '#64748b';
      conteneur.append(statut);
    }

    return conteneur;
  }
}