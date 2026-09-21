import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DatePicker } from 'primeng/datepicker';
import { GardePublicService } from '../services/garde-public.service';
import { PharmacieDeGarde } from '../models/garde-public.model';

// Page d'accueil publique de PharmaGarde : accessible sans authentification (RG-04).
// Affiche les pharmacies de garde à une date choisie (aujourd'hui par défaut).
@Component({
  selector: 'app-pharmacies-de-garde',
  standalone: true,
  imports: [FormsModule, RouterLink, DatePicker],
  templateUrl: './pharmacies-de-garde.html',
  styleUrl: './pharmacies-de-garde.scss'
})
export class PharmaciesDeGarde implements OnInit {
  private gardePublicService = inject(GardePublicService);

  dateSelectionnee = signal<Date>(new Date());
  pharmacies = signal<PharmacieDeGarde[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);

  ngOnInit(): void {
    this.charger();
  }

  onDateChange(date: Date): void {
    this.dateSelectionnee.set(date);
    this.charger();
  }

  private charger(): void {
    this.chargement.set(true);
    this.erreur.set(null);
    const dateFormatee = this.formatDate(this.dateSelectionnee());

    this.gardePublicService.listPharmaciesDeGarde(dateFormatee).subscribe({
      next: (resultat) => {
        this.pharmacies.set(resultat);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger les pharmacies de garde pour le moment.');
        this.chargement.set(false);
      }
    });
  }

  // Formate une Date en "YYYY-MM-DD" à partir des valeurs LOCALES (jour/mois/année) —
  // volontairement PAS date.toISOString(), qui convertit en UTC et peut faire glisser
  // la date d'un jour selon le fuseau horaire du visiteur.
  private formatDate(date: Date): string {
    const annee = date.getFullYear();
    const mois = String(date.getMonth() + 1).padStart(2, '0');
    const jour = String(date.getDate()).padStart(2, '0');
    return `${annee}-${mois}-${jour}`;
  }

  libelleType(type: string): string {
    switch (type) {
      case 'JOUR': return 'Garde de jour';
      case 'NUIT': return 'Garde de nuit';
      case 'COMPLETE': return 'Garde complète';
      default: return type;
    }
  }

  // Lien "tel:" pour déclencher un appel direct depuis un mobile.
  // On retire les espaces : un numéro affiché "+221 33 821 00 01" doit devenir "+221338210001".
  lienAppel(telephone: string): string {
    return `tel:${telephone.replace(/\s/g, '')}`;
  }

  // Ouvre Google Maps aux coordonnées GPS de la pharmacie (itinéraire depuis la position du visiteur)
  lienItineraire(pharmacie: PharmacieDeGarde): string {
    return `https://www.google.com/maps/dir/?api=1&destination=${pharmacie.latitude},${pharmacie.longitude}`;
  }
}