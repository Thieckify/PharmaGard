// Calculs de distance entre positions GPS. Fonctions pures, sans dépendance :
// équivalent frontend d'une classe utilitaire statique côté backend (cf. UserMapper,
// GardeMapper) — pas un service, puisqu'il n'y a ni état ni injection ni accès réseau.
import { PharmaciePublique } from '../models/pharmacie-publique.model';

const RAYON_TERRE_KM = 6371;

// Formule de Haversine : distance à vol d'oiseau entre deux points GPS, en kilomètres.
// Tient compte de la courbure terrestre (une simple différence de degrés latitude/
// longitude donnerait un résultat faux).
export function distanceKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const dLat = toRadians(lat2 - lat1);
  const dLon = toRadians(lon2 - lon1);

  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.sin(dLon / 2) ** 2;

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return RAYON_TERRE_KM * c;
}

// Calcule la distance de chaque pharmacie ayant des coordonnées, puis trie la liste
// par distance croissante. Les pharmacies sans coordonnées sont reléguées en fin de
// liste, sans distance affichée. Fonction pure : renvoie un nouveau tableau, ne modifie
// jamais celui reçu en argument.
export function trierParDistance(
  pharmacies: PharmaciePublique[],
  latVisiteur: number,
  lonVisiteur: number
): PharmaciePublique[] {
  const avecDistance = pharmacies.map((p) => {
    if (p.latitude == null || p.longitude == null) {
      return p;
    }
    return { ...p, distanceKm: distanceKm(latVisiteur, lonVisiteur, p.latitude, p.longitude) };
  });

  return avecDistance.sort((a, b) => {
    if (a.distanceKm == null) return 1;
    if (b.distanceKm == null) return -1;
    return a.distanceKm - b.distanceKm;
  });
}

function toRadians(degres: number): number {
  return (degres * Math.PI) / 180;
}