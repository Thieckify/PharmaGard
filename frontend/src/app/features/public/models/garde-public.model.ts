// Miroir de PharmacieDeGardeResponse (backend) : représentation publique d'une
// pharmacie de garde, sans aucune information administrative interne (RG-04).
export type TypeGarde = 'JOUR' | 'NUIT' | 'COMPLETE';

export interface PharmacieDeGarde {
  pharmacieId: number;
  nom: string;
  adresse: string;
  nomQuartier: string;
  nomZone: string;
  latitude: number;
  longitude: number;
  telephone: string;
  horaireOuverture: string;
  horaireFermeture: string;
  typeGarde: TypeGarde;
  dateDebutGarde: string;
  dateFinGarde: string;
}