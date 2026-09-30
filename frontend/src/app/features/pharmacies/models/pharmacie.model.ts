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
  // Qui a validé la fiche et quand (null tant qu'elle est en attente)
  valideParUserId: number | null;
  valideLe: string | null;
  // Preuve d'exercice déclarée par le pharmacien : le superviseur la contrôle avant de valider
  numeroAgrementOrdre: string;
}

// Miroir de PharmacieRequest (backend) : ce qu'on envoie pour créer/modifier.
// numeroAgrementOrdre est obligatoire à la création. Pour une fiche existante, un numéro
// différent de l'actuel le remplace mais remet la fiche en attente de validation (RG-16) ;
// un numéro identique ne change rien.
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