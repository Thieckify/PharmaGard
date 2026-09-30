import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AssistantRequest, AssistantResponse } from '../models/assistant.model';

// Appelle l'assistant conversationnel public (POST /api/assistant). Chaque question est
// indépendante côté serveur (pas d'historique envoyé) : l'historique affiché dans le widget
// de chat n'existe que côté client, pour le confort visuel de la conversation.
@Injectable({ providedIn: 'root' })
export class AssistantService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/assistant`;

  poserQuestion(question: string): Observable<AssistantResponse> {
    const requete: AssistantRequest = { question };
    return this.http.post<AssistantResponse>(this.apiUrl, requete);
  }
}