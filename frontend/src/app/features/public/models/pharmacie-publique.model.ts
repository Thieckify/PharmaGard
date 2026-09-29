// Miroir de PharmaciePubliqueResponse (backend) : représentation publique d'une
// pharmacie dans les résultats de recherche, sans aucune information administrative
// interne (tenant, validation) — endpoint accessible sans authentification (RG-04).
export interface PharmaciePublique {
  pharmacieId: number;
  nom: string;
  adresse: string;
  nomQuartier: string;
  nomZone: string;
  latitude: number | null;
  longitude: number | null;
  telephone: string;
  horaireOuverture: string;
  horaireFermeture: string;
  // Calculé côté client une fois la position du visiteur connue (absent tant que
  // la géolocalisation n'a pas été utilisée)
  distanceKm?: number;
}