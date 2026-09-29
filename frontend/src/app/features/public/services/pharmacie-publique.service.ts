import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { PharmaciePublique } from '../models/pharmacie-publique.model';

// Accède à la recherche publique de pharmacies (aucune authentification requise)
@Injectable({
  providedIn: 'root'
})
export class PharmaciePubliqueService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/public`;

  // GET /api/public/pharmacies?quartier=... : sans quartierId, renvoie toutes les
  // pharmacies actives (voir PublicPharmacieController côté backend)
  rechercher(quartierId?: number): Observable<PharmaciePublique[]> {
    let params = new HttpParams();
    if (quartierId != null) {
      params = params.set('quartier', quartierId);
    }
    return this.http.get<PharmaciePublique[]>(`${this.apiUrl}/pharmacies`, { params });
  }
}