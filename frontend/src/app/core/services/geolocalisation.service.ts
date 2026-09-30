import { Injectable } from '@angular/core';

// Encapsule l'accès à l'API navigateur de géolocalisation. Comme pour HttpClient,
// on n'appelle jamais navigator.geolocation directement depuis un composant (cf. §5
// CONTEXT.md — toute logique d'accès à une plateforme externe vit dans un service).
@Injectable({
  providedIn: 'root'
})
export class GeolocalisationService {

  // Demande la position actuelle du visiteur (déclenche la popup d'autorisation du
  // navigateur). Convertit l'API à base de callbacks en Promise, plus simple à
  // consommer depuis un composant avec async/await.
  obtenirPosition(): Promise<GeolocationCoordinates> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('GEOLOCATION_INDISPONIBLE'));
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (position) => resolve(position.coords),
        () => reject(new Error('GEOLOCATION_REFUSEE'))
      );
    });
  }
}