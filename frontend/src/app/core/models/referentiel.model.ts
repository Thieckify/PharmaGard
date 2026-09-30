// Miroir de QuartierResponse (backend)
export interface Quartier {
  id: number;
  nom: string;
  zoneId: number;
  nomZone: string;
  // Centre approximatif du quartier (null si inconnu) : sert à ouvrir les cartes au bon endroit
  latitude: number | null;
  longitude: number | null;
}