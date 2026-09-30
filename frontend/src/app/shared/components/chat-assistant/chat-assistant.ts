import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AssistantService } from '../../../core/services/assistant.service';

// Un message affiché dans le fil de discussion. "erreur" est un rôle à part (pas "assistant")
// pour lui donner un habillage visuel différent (teinte d'alerte douce).
interface MessageChat {
  id: number;
  role: 'utilisateur' | 'assistant' | 'erreur';
  texte: string;
}

// Bulle de discussion flottante (bas droite de l'écran), présente sur l'espace public.
// Chaque question part sans historique côté serveur (cf. AssistantService, brique backend) :
// l'historique affiché ici n'est que visuel, pour le confort de la conversation.
@Component({
  selector: 'app-chat-assistant',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './chat-assistant.html',
  styleUrl: './chat-assistant.scss'
})
export class ChatAssistant {
  private assistantService = inject(AssistantService);

  @ViewChild('zoneMessages') private zoneMessagesRef?: ElementRef<HTMLDivElement>;

  ouvert = signal(false);
  envoiEnCours = signal(false);
  // Simple propriété (pas un signal) : ngModel s'en charge, comme les formulaires de connexion
  question = '';

  messages = signal<MessageChat[]>([
    {
      id: 0,
      role: 'assistant',
      texte: "Bonjour ! Je peux vous aider à trouver une pharmacie de garde aujourd'hui à Dakar. Que cherchez-vous ?"
    }
  ]);

  private prochainId = 1;

  basculer(): void {
    this.ouvert.update((valeur) => !valeur);
    if (this.ouvert()) {
      setTimeout(() => this.defilerVersLeBas(), 50);
    }
  }

  envoyer(): void {
    const texte = this.question.trim();
    if (!texte || this.envoiEnCours()) {
      return;
    }

    this.ajouterMessage('utilisateur', texte);
    this.question = '';
    this.envoiEnCours.set(true);

    this.assistantService.poserQuestion(texte).subscribe({
      next: (reponse) => {
        this.ajouterMessage('assistant', reponse.reponse);
        this.envoiEnCours.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.ajouterMessage('erreur', this.messageErreur(err));
        this.envoiEnCours.set(false);
      }
    });
  }

  // Traduit les codes d'erreur du backend (brique 4) en phrases compréhensibles pour un visiteur
  private messageErreur(err: HttpErrorResponse): string {
    if (err.status === 429) {
      return 'Trop de questions envoyées en peu de temps. Réessayez dans une minute.';
    }
    if (err.status === 503) {
      return "L'assistant est momentanément indisponible. Réessayez dans un instant, ou consultez directement la liste des pharmacies de garde ci-dessus.";
    }
    if (err.status === 400) {
      return err.error?.message ?? 'Question invalide.';
    }
    return 'Une erreur est survenue. Réessayez dans un instant.';
  }

  private ajouterMessage(role: MessageChat['role'], texte: string): void {
    this.messages.update((liste) => [...liste, { id: this.prochainId++, role, texte }]);
    setTimeout(() => this.defilerVersLeBas(), 50);
  }

  private defilerVersLeBas(): void {
    const element = this.zoneMessagesRef?.nativeElement;
    if (element) {
      element.scrollTop = element.scrollHeight;
    }
  }
}