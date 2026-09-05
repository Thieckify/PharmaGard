import { Component, OnInit, inject, signal } from '@angular/core';
import { PharmacieService } from '../services/pharmacie.service';
import { Pharmacie } from '../models/pharmacie.model';

/**
 * Affiche la liste des pharmacies visibles pour l'utilisateur connecté.
 * Le scope (sa pharmacie si PHARMACIEN, sa zone ou toutes si SUPERVISEUR)
 * est appliqué automatiquement côté backend (RG-01 / RG-02).
 */
@Component({
  selector: 'app-pharmacies-liste',
  standalone: true,
  imports: [],
  templateUrl: './liste.html',
  styleUrl: './liste.scss'
})
export class Liste implements OnInit {
  private pharmacieService = inject(PharmacieService);

  pharmacies = signal<Pharmacie[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);

  ngOnInit(): void {
    this.pharmacieService.listPharmacies().subscribe({
      next: (donnees) => {
        this.pharmacies.set(donnees);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger les pharmacies.');
        this.chargement.set(false);
      }
    });
  }
}