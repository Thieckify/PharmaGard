import * as L from 'leaflet';

// Icône de marqueur Leaflet par défaut, partagée par toutes les cartes de l'application.
// L'icône par défaut de Leaflet est cassée par les bundlers modernes (chemins d'images
// relatifs au CSS, non résolus par esbuild) : on la reconstruit depuis un CDN.
export const ICONE_MARQUEUR = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

// Repère en forme de goutte, dessiné en CSS (aucune image à charger), dont on choisit la
// couleur. Les styles sont écrits en ligne : Leaflet crée cet élément lui-même, hors du
// composant Angular, donc les styles d'un composant ne l'atteindraient pas.
// La pointe de la goutte est à environ 27px sous le haut de l'élément : c'est l'ancre.
function goutte(couleur: string): L.DivIcon {
  return L.divIcon({
    className: '',
    html:
      `<span style="position:relative;display:block;width:22px;height:22px;` +
      `background:${couleur};border:3px solid white;border-radius:50% 50% 50% 0;` +
      `transform:rotate(-45deg);box-shadow:0 2px 6px rgba(0,0,0,0.35);">` +
      `<span style="position:absolute;top:50%;left:50%;width:8px;height:8px;` +
      `margin:-4px 0 0 -4px;border-radius:50%;background:white;"></span></span>`,
    iconSize: [22, 22],
    iconAnchor: [11, 27],
    popupAnchor: [0, -26]
  });
}

// Fiche validée : le vert des statuts, le même que la pastille "Validée" des pages
export const ICONE_VALIDEE = goutte('var(--pg-success)');

// Fiche en attente de validation : doré, pour attirer l'œil du superviseur
export const ICONE_ATTENTE = goutte('var(--pg-accent-gold)');