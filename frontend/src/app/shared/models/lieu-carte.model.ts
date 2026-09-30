// Statut d'une pharmacie, pour colorer son repère sur la carte des superviseurs :
// "validee" = fiche validée, "attente" = fiche à valider (ou remise en attente)
export type StatutLieu = 'validee' | 'attente';

// Forme minimale nécessaire pour afficher un lieu sur la carte Leaflet : un nom, une
// adresse, et des coordonnées GPS. PharmaciePublique et PharmacieDeGarde satisfont
// toutes les deux cette forme sans avoir besoin de l'implémenter explicitement (typage
// structurel de TypeScript). "statut" est facultatif : sans lui, le repère par défaut est
// utilisé (cas de l'espace public).
export interface LieuCarte {
  nom: string;
  adresse: string;
  latitude: number | null;
  longitude: number | null;
  statut?: StatutLieu;
}