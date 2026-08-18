import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeng/themes/aura';

import { routes } from './app.routes';

/**
 * Configuration racine de l'application Angular.
 * Enregistre le router, le client HTTP (pour appeler l'API),
 * et PrimeNG avec un thème personnalisé (Aura, préréglé teal).
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
        provideHttpClient(withInterceptors([jwtInterceptor])),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          // Sélecteur CSS où appliquer le thème sombre (facultatif, on garde le clair par défaut)
          darkModeSelector: '.dark-mode',
          // Ordre des couches CSS : notre CSS custom prime sur celui de PrimeNG
          cssLayer: {
            name: 'primeng',
            order: 'app-styles, primeng'
          }
        }
      }
    })
  ]
};