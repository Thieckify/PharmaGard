/**
 * Rôles possibles côté PharmaGarde.
 * Doit rester synchronisé avec l'enum Role du backend.
 */
export type Role = 'ADMIN' | 'SUPERVISEUR' | 'PHARMACIEN' | 'VISITEUR';

/**
 * Corps envoyé à POST /api/auth/login
 */
export interface LoginRequest {
  email: string;
  password: string;
}

/**
 * Réponse reçue de POST /api/auth/login ou POST /api/auth/register.
 * Correspond exactement au LoginResponse du backend (les deux endpoints renvoient
 * la même forme : l'inscription connecte directement, décision prise en S5).
 */
export interface LoginResponse {
  token: string;
  userId: number;
  email: string;
  role: Role;
  tenantId: number | null;
  zoneId: number | null;
}

/**
 * Corps envoyé à POST /api/auth/register.
 * Miroir du RegisterRequest backend : compte ET fiche pharmacie créés ensemble,
 * en une seule transaction (AuthService.register côté backend). Le champ
 * "pharmacie" reprend exactement la forme de PharmacieRequest
 * (features/pharmacies/models/pharmacie.model.ts) SANS l'importer : core ne doit
 * jamais dépendre de features (règle de dépendance, §5 CONTEXT.md).
 */
export interface RegisterRequest {
  email: string;
  password: string;
  pharmacie: {
    nom: string;
    adresse: string;
    quartierId: number;
    latitude: number | null;
    longitude: number | null;
    telephone: string | null;
    horaireOuverture: string | null;
    horaireFermeture: string | null;
    numeroAgrementOrdre: string;
  };
}