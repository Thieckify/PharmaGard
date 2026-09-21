import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Garde, GardeRequest } from '../models/garde.model';

// Centralise les appels HTTP vers /api/gardes (CRUD du planning du pharmacien connecté)
@Injectable({
  providedIn: 'root'
})
export class GardeService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/gardes`;

  // GET /api/gardes : liste les périodes déjà déclarées par sa propre pharmacie
  listMesGardes(): Observable<Garde[]> {
    return this.http.get<Garde[]>(this.apiUrl);
  }

  // POST /api/gardes : déclare une nouvelle période
  declarerGarde(request: GardeRequest): Observable<Garde> {
    return this.http.post<Garde>(this.apiUrl, request);
  }

  // PUT /api/gardes/{id} : modifie une période existante
  modifierGarde(id: number, request: GardeRequest): Observable<Garde> {
    return this.http.put<Garde>(`${this.apiUrl}/${id}`, request);
  }

  // DELETE /api/gardes/{id} : supprime une période
  supprimerGarde(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}