// Calculs de distance entre positions GPS. Fonctions pures, sans dépendance :
// équivalent frontend d'une classe utilitaire statique côté backend (cf. UserMapper,
// GardeMapper) — pas un service, puisqu'il n'y a ni état ni injection ni accès réseau.
// Génériques : utilisées aussi bien pour PharmaciePublique (recherche par quartier)
// que PharmacieDeGarde (recherche par date), qui partagent latitude/longitude/
// distanceKm sans avoir de type commun explicite.

const RAYON_TERRE_KM = 6371;

// Rayon par défaut (en km) au-delà duquel une pharmacie n'est plus considérée "proche"
// lors d'une recherche par géolocalisation.
export const RAYON_PROXIMITE_KM = 5;

interface AvecPosition {
  latitude: number | null;
  longitude: number | null;
  distanceKm?: number;
}

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

// Calcule la distance de chaque élément ayant des coordonnées, puis trie la liste par
// distance croissante. Les éléments sans coordonnées sont relégués en fin de liste,
// sans distance affichée. Fonction pure : renvoie un nouveau tableau, ne modifie jamais
// celui reçu en argument.
export function trierParDistance<T extends AvecPosition>(
  items: T[],
  latVisiteur: number,
  lonVisiteur: number
): T[] {
  const avecDistance = items.map((item) => {
    if (item.latitude == null || item.longitude == null) {
      return item;
    }
    return { ...item, distanceKm: distanceKm(latVisiteur, lonVisiteur, item.latitude, item.longitude) };
  });

  return avecDistance.sort((a, b) => {
    if (a.distanceKm == null) return 1;
    if (b.distanceKm == null) return -1;
    return a.distanceKm - b.distanceKm;
  });
}

// Garde uniquement les éléments à moins de rayonKm (la liste reçue doit déjà être
// triée par trierParDistance). Les éléments sans distance calculée sont exclus.
export function filtrerParRayon<T extends AvecPosition>(items: T[], rayonKm: number): T[] {
  return items.filter((item) => item.distanceKm != null && item.distanceKm <= rayonKm);
}

function toRadians(degres: number): number {
  return (degres * Math.PI) / 180;
}