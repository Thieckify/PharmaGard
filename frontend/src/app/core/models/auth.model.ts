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
 * Réponse reçue de POST /api/auth/login.
 * Correspond exactement au LoginResponse du backend.
 */
export interface LoginResponse {
  token: string;
  userId: number;
  email: string;
  role: Role;
  tenantId: number | null;
  zoneId: number | null;
}