import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Pharmacie, PharmacieRequest } from '../models/pharmacie.model';

// Centralise les appels HTTP vers /api/pharmacies (upsert + liste scopée par rôle)
@Injectable({
  providedIn: 'root'
})
export class PharmacieService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/pharmacies`;

  // POST /api/pharmacies : crée ou met à jour la fiche du pharmacien connecté
  upsertPharmacie(request: PharmacieRequest): Observable<Pharmacie> {
    return this.http.post<Pharmacie>(this.apiUrl, request);
  }

  // GET /api/pharmacies : liste scopée selon le rôle (backend applique RG-01/RG-02)
  listPharmacies(): Observable<Pharmacie[]> {
    return this.http.get<Pharmacie[]>(this.apiUrl);
  }
}