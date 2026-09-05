import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Quartier } from '../models/referentiel.model';

// Centralise l'accès aux données de référentiel (zones, quartiers), utilisées par plusieurs features
@Injectable({
  providedIn: 'root'
})
export class ReferentielService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/public`;

  // GET /api/public/quartiers : liste tous les quartiers, pour peupler menus déroulants et recherche
  listQuartiers(): Observable<Quartier[]> {
    return this.http.get<Quartier[]>(`${this.apiUrl}/quartiers`);
  }
}