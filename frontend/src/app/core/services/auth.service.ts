import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse } from '../models/auth.model';
import { environment } from '../../../environments/environment';

const TOKEN_KEY = 'pharmagarde_token';

/**
 * Service central d'authentification.
 * Gère le login, la déconnexion, et expose l'état de session
 * (utilisateur connecté ou non) au reste de l'application via un signal.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly apiUrl = `${environment.apiUrl}/auth`;

  // Signal réactif : contient les infos de l'utilisateur connecté, ou null
  currentUser = signal<LoginResponse | null>(this.loadFromStorage());

  constructor(private http: HttpClient, private router: Router) {}

  /**
   * Appelle POST /api/auth/login, stocke le token en cas de succès,
   * et met à jour le signal currentUser.
   */
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => {
        localStorage.setItem(TOKEN_KEY, JSON.stringify(response));
        this.currentUser.set(response);
      })
    );
  }

  /**
   * Déconnecte l'utilisateur : vide le stockage et redirige vers le login.
   */
  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/auth/login']);
  }

  /**
   * Retourne le token JWT brut, utilisé par l'intercepteur HTTP.
   */
  getToken(): string | null {
    return this.currentUser()?.token ?? null;
  }

  /**
   * Vrai si un utilisateur est actuellement connecté (token présent).
   */
  isAuthenticated(): boolean {
    return this.currentUser() !== null;
  }

  /**
   * Recharge la session depuis le localStorage au démarrage de l'app,
   * pour ne pas perdre la connexion à chaque rafraîchissement de page.
   */
  private loadFromStorage(): LoginResponse | null {
    const raw = localStorage.getItem(TOKEN_KEY);
    return raw ? JSON.parse(raw) : null;
  }
}