// Miroir de PharmacieResponse (backend) : ce que l'API renvoie
export interface Pharmacie {
  id: number;
  nom: string;
  adresse: string;
  quartierId: number;
  nomQuartier: string;
  zoneId: number;
  nomZone: string;
  latitude: number | null;
  longitude: number | null;
  telephone: string | null;
  horaireOuverture: string | null;
  horaireFermeture: string | null;
  active: boolean;
  tenantUserId: number;
}

// Miroir de PharmacieRequest (backend) : ce qu'on envoie pour créer/modifier
export interface PharmacieRequest {
  nom: string;
  adresse: string;
  quartierId: number;
  latitude: number | null;
  longitude: number | null;
  telephone: string | null;
  horaireOuverture: string | null;
  horaireFermeture: string | null;
  numeroAgrementOrdre: string;
}