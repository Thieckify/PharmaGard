import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Pharmacie, PharmacieRequest } from '../models/pharmacie.model';

// Centralise les appels HTTP vers /api/pharmacies (upsert, liste scopée par rôle, et les
// actions de modération réservées aux superviseurs : valider/refuser, supprimer)
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

  // PATCH /api/pharmacies/{id}/validation : un superviseur valide (true) ou refuse (false)
  // une fiche. Le refus la remet en attente, sans la supprimer. Le backend applique RG-02 (403 hors zone).
  validerPharmacie(id: number, valide: boolean): Observable<Pharmacie> {
    return this.http.patch<Pharmacie>(`${this.apiUrl}/${id}/validation`, { valide });
  }

  // DELETE /api/pharmacies/{id} : un superviseur supprime définitivement une fiche, son
  // planning et le compte du pharmacien (RG-15). Le serveur répond 204 sans corps :
  // d'où Observable<void>.
  supprimerPharmacie(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}