import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { PharmacieDeGarde } from '../models/garde-public.model';

// Accède à l'espace public "pharmacies de garde" (aucune authentification requise)
@Injectable({
  providedIn: 'root'
})
export class GardePublicService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/public`;

  // GET /api/public/gardes?date=... : pharmacies de garde à une date donnée.
  // Si "date" est omis, le backend applique "aujourd'hui" par défaut (voir PublicGardeController).
  listPharmaciesDeGarde(date?: string): Observable<PharmacieDeGarde[]> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<PharmacieDeGarde[]>(`${this.apiUrl}/gardes`, { params });
  }
}