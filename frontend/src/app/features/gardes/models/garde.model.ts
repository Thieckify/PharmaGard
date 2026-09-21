export type TypeGarde = 'JOUR' | 'NUIT' | 'COMPLETE';

// Miroir de GardeResponse (backend)
export interface Garde {
  id: number;
  pharmacieId: number;
  nomPharmacie: string;
  dateDebut: string;
  dateFin: string;
  type: TypeGarde;
}

// Miroir de GardeRequest (backend) — utilisé pour déclarer ET modifier une période
export interface GardeRequest {
  dateDebut: string;
  dateFin: string;
  type: TypeGarde;
}