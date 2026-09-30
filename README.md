# PharmaGarde

Plateforme SaaS multi-tenant de pharmacies de garde à Dakar. Projet intégrateur M1 Ingénierie
Numérique — EPF Africa (encadrant : M. Baba TOP).

Un pharmacien y déclare et gère sa fiche et son planning de garde ; un superviseur valide les
fiches et surveille la couverture de garde par zone ; le grand public consulte gratuitement,
sans compte, quelles pharmacies sont de garde aujourd'hui ou à une date donnée, avec un
assistant conversationnel pour l'aider à s'y retrouver.

## Sommaire

- [Stack technique](#stack-technique)
- [Installation](#installation)
- [Lancement](#lancement)
- [Comptes de démonstration](#comptes-de-démonstration)
- [Variables d'environnement](#variables-denvironnement)
- [Profils Spring (dev / prod)](#profils-spring-dev--prod)
- [Structure du dépôt](#structure-du-dépôt)
- [Contrat d'API](#contrat-dapi)
- [Règles de gestion principales](#règles-de-gestion-principales)
- [Limites connues](#limites-connues)

## Stack technique

| Couche | Techno |
|---|---|
| Backend | Spring Boot 4.0.7 · Spring Data JPA · Spring Security (JWT) · Spring AI 2.0.0 |
| Base de données | PostgreSQL 18.4 |
| IA | Ollama (modèle local, `llama3.2:3b` par défaut) |
| Frontend | Angular 20+ (zoneless) · PrimeNG (thème Aura) · Leaflet |
| Documentation API | springdoc-openapi 3.0.3 (Swagger UI) |
| Java | JDK 21 |

Architecture Clean Architecture des deux côtés : `domain` ne dépend de rien, `application` ne
dépend que de `domain`, `infrastructure` et `adapter` implémentent vers le centre.

## Installation

### Prérequis

- JDK 21
- Maven (le wrapper `mvnw` est fourni, aucune installation séparée requise)
- Node.js 20+ et npm
- PostgreSQL 18 (une version 14+ convient)
- [Ollama](https://ollama.com/download), avec le modèle `llama3.2:3b` :

ollama pull llama3.2:3b

  Ollama doit tourner en arrière-plan pour que l'assistant conversationnel réponde ; le reste
  de l'application fonctionne sans lui (l'assistant renvoie alors une erreur `503`).

### Base de données

Créez une base vide :

```sql
CREATE DATABASE pharmagarde_db;
```

Aucune autre étape manuelle : le schéma est créé automatiquement par Hibernate au premier
démarrage (profil `dev`), et les données de référence (zones, quartiers) sont chargées par
`data-reference.sql`.

### Secrets (`.env`)

Le backend a besoin d'un fichier `.env`, jamais versionné, à côté de `pom.xml` :

cd backend
cp .env.example .env


Éditez `.env` et renseignez au minimum :
- `DB_PASSWORD` — le mot de passe de votre utilisateur PostgreSQL
- `JWT_SECRET` — une chaîne aléatoire d'au moins 32 caractères (voir plus bas comment en générer une)

Sans ce fichier, ou sans ces deux valeurs, le backend refuse de démarrer avec un message
explicite plutôt que de tourner avec un secret par défaut non sécurisé.

### Frontend

cd frontend
npm install


## Lancement

**Backend** (depuis `backend/`) :

./mvnw spring-boot:run


Démarre sur `http://localhost:8080`, en profil `dev` par défaut (aucune variable à définir).

**Frontend** (depuis `frontend/`) :

ng serve


Démarre sur `http://localhost:4200`.

Au premier démarrage du backend en `dev`, Hibernate crée le schéma, puis `data-reference.sql`
(zones et quartiers) et `data-dev.sql` (comptes de test et jeu de démonstration) s'exécutent
automatiquement. Ces deux scripts sont rejouables sans créer de doublon.

## Comptes de démonstration

Mot de passe commun à tous les comptes ci-dessous : **`password123`**.

| Email | Rôle | Portée |
|---|---|---|
| `admin@pharmagarde.sn` | ADMIN | — |
| `superviseur@pharmagarde.sn` | SUPERVISEUR | Toutes les zones |
| `superviseur.plateau@pharmagarde.sn` | SUPERVISEUR | Zone Plateau uniquement |
| `superviseur.fannpointe@pharmagarde.sn` | SUPERVISEUR | Zone Fann - Point E uniquement |
| `pharmacien@pharmagarde.sn` | PHARMACIEN | Pharmacie du Plateau (fiche validée) |
| `pharmacien2@pharmagarde.sn` | PHARMACIEN | Aucune fiche pharmacie (utile pour tester le rejet RG-01) |

En plus de ces comptes de base, `data-dev.sql` crée un **jeu de démonstration** de 11
pharmacies fictives (comptes `demo.*@pharmagarde.sn`, même mot de passe), réparties dans
plusieurs zones, avec des gardes calculées automatiquement pour couvrir la date du jour à
chaque démarrage. Il inclut volontairement une fiche manifestement frauduleuse
(« Pharmacie Bidon ») pour démontrer la suppression par un superviseur (RG-15), et une fiche
en attente pour démontrer la validation. Toute modification faite sur ces comptes `demo.*`
pendant une session est réinitialisée au redémarrage suivant du backend ; les comptes de base
du tableau ci-dessus ne le sont pas.

## Variables d'environnement

Toutes définies dans `backend/.env` (voir `backend/.env.example` pour le modèle complet).

| Variable | Obligatoire | Défaut (dev) | Rôle |
|---|---|---|---|
| `DB_PASSWORD` | oui | — | Mot de passe PostgreSQL |
| `JWT_SECRET` | oui | — | Clé de signature des jetons de connexion (HS256, 256 bits mini.) |
| `DB_HOST` | non | `localhost` | Hôte PostgreSQL |
| `DB_PORT` | non | `5432` | Port PostgreSQL |
| `DB_NAME` | non | `pharmagarde_db` | Nom de la base |
| `DB_USERNAME` | non | `postgres` | Utilisateur PostgreSQL |
| `JWT_EXPIRATION_MS` | non | `86400000` (24 h) | Durée de validité d'un jeton |
| `CORS_ALLOWED_ORIGINS` | oui en prod | `http://localhost:4200` | Origines autorisées à appeler l'API, séparées par des virgules |
| `OLLAMA_BASE_URL` | non | `http://localhost:11434` | Adresse du serveur Ollama |
| `OLLAMA_MODEL` | non | `llama3.2:3b` | Modèle utilisé par l'assistant |
| `DDL_AUTO` | non | — | En profil `prod` uniquement, pour le tout premier déploiement (voir plus bas) |

**Générer une clé JWT** (PowerShell) :

```powershell
$caracteres = (48..57) + (65..90) + (97..122) | ForEach-Object { [char]$_ }
$octets = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($octets)
-join ($octets | ForEach-Object { $caracteres[$_ % $caracteres.Length] })
```

Changer `DB_PASSWORD` ou `JWT_SECRET` exige un redémarrage **complet** du backend (pas un
simple rechargement à chaud) : `spring-boot-devtools` ne surveille pas le fichier `.env`.
Changer la clé JWT invalide immédiatement tous les jetons déjà distribués.

## Profils Spring (dev / prod)

| | `dev` (par défaut) | `prod` |
|---|---|---|
| SQL affiché | oui | non |
| Schéma (`ddl-auto`) | `update` (automatique) | `validate` (strict, ne modifie rien) |
| Données chargées | référence + démonstration | référence uniquement |
| Swagger UI | activé (`/swagger-ui.html`) | désactivé |
| Comptes créés au démarrage | oui (mot de passe connu) | aucun |

Basculer en production :

SPRING_PROFILES_ACTIVE=prod


**Premier déploiement sur une base vide en production** : lancez une seule fois avec
`DDL_AUTO=update` pour laisser Hibernate créer les tables, puis retirez cette variable pour
tous les démarrages suivants (le profil `prod` repasse alors en vérification stricte du
schéma).

Comme aucun compte n'existe en production, le tout premier compte ADMIN doit être créé
manuellement en base, avec un mot de passe haché en BCrypt.

## Structure du dépôt

PharmaGard/
├── backend/ # API Spring Boot
│ ├── src/main/java/com/pharmagarde/backend/
│ │ ├── domain/ # Modèles métier purs, aucune dépendance externe
│ │ ├── application/ # Services et DTO, dépend uniquement de domain
│ │ ├── infrastructure/ # JPA, sécurité, config CORS, assistant IA
│ │ └── adapter/web/ # Contrôleurs REST et gestion globale des erreurs
│ ├── src/main/resources/
│ │ ├── application*.properties
│ │ ├── data-reference.sql # Zones et quartiers — tous profils
│ │ └── data-dev.sql # Comptes de test et démonstration — dev uniquement
│ └── .env.example
└── frontend/ # Application Angular
└── src/app/
├── core/ # Services transverses, modèles, garde de routes
├── features/ # Une feature par domaine (auth, pharmacies, gardes, public, dashboard)
└── shared/ # Composants et styles réutilisés entre features


## Contrat d'API

Documentation interactive complète disponible une fois le backend lancé :
`http://localhost:8080/swagger-ui.html`.

### Routes publiques (sans authentification)

| Méthode | Route | Description |
|---|---|---|
| POST | `/api/auth/login` | Connexion, renvoie un jeton JWT |
| POST | `/api/auth/register` | Auto-inscription d'un pharmacien (compte + fiche, atomique) |
| GET | `/api/public/pharmacies?quartier=` | Recherche de pharmacies actives, par quartier ou toutes |
| GET | `/api/public/gardes?date=` | Pharmacies de garde à une date (par défaut aujourd'hui) |
| GET | `/api/public/quartiers` | Référentiel des zones et quartiers |
| POST | `/api/assistant` | Assistant conversationnel (limité à 10 requêtes/minute/IP) |

### Routes protégées (jeton JWT requis)

| Méthode | Route | Rôle | Description |
|---|---|---|---|
| GET | `/api/pharmacies` | PHARMACIEN / SUPERVISEUR | Liste scopée par rôle (RG-01/RG-02) |
| POST | `/api/pharmacies` | PHARMACIEN | Crée ou met à jour sa fiche (upsert) |
| PATCH | `/api/pharmacies/{id}/validation` | SUPERVISEUR | Valide ou refuse une fiche |
| DELETE | `/api/pharmacies/{id}` | SUPERVISEUR | Supprime une fiche (et le compte associé) |
| GET | `/api/gardes` | PHARMACIEN | Son propre planning |
| POST | `/api/gardes` | PHARMACIEN | Déclare une période de garde |
| PUT | `/api/gardes/{id}` | PHARMACIEN | Modifie une période (la sienne uniquement) |
| DELETE | `/api/gardes/{id}` | PHARMACIEN | Supprime une période (la sienne uniquement) |

## Règles de gestion principales

- **RG-01** — un pharmacien ne voit et ne modifie que sa propre pharmacie et son propre planning.
- **RG-02** — un superviseur de zone est restreint à sa zone ; seul le superviseur général
  (sans zone assignée) accède à tout.
- **RG-03** — deux périodes de garde d'une même pharmacie ne peuvent jamais se chevaucher.
- **RG-04** — l'espace public est accessible sans authentification.
- **RG-05** — une pharmacie inactive (non validée) n'apparaît jamais publiquement.
- **RG-11** — changer la zone d'une fiche déjà validée annule sa validation : le superviseur
  précédent n'a plus autorité une fois la fiche sortie de sa zone.
- **RG-12** — une fiche doit être validée pour que son titulaire puisse déclarer une garde.
- **RG-13** — anti-antidatage : impossible de déclarer une garde dans le passé, ni de reculer
  la date de début d'une garde déjà commencée.
- **RG-14** — la position GPS d'une pharmacie est obligatoire, choisie sur une carte.
- **RG-15** — un superviseur peut supprimer définitivement une fiche frauduleuse ; le compte
  du pharmacien est supprimé avec elle.
- **RG-16** — modifier son numéro d'agrément après validation remet la fiche en attente,
  comme un changement de zone (RG-11).

## Limites connues

- **`spring.jpa.open-in-view=true`** est volontairement conservé : certains services
  (`ReferentielService` notamment) lisent des relations chargées à la demande en dehors d'une
  transaction explicite. Le désactiver casserait ces lectures sans réécriture préalable.
- **L'encodage `UTF-8` n'est explicite que sur `POST /api/assistant`.** Les autres routes
  fonctionnent correctement avec un navigateur ou Angular (qui supposent l'UTF-8 par défaut
  pour le JSON), mais un client HTTP strict pourrait mal interpréter les accents en l'absence
  du paramètre `charset` dans l'en-tête `Content-Type`.
- **Le premier appel à l'assistant après le démarrage d'Ollama est lent** (jusqu'à 50 secondes
  sur une machine sans carte graphique dédiée, le temps que le modèle se charge en mémoire) ;
  les appels suivants prennent 5 à 10 secondes.
- **Un pharmacien sans fiche existante** ne peut être créé que par auto-inscription
  (`POST /api/auth/register`) ou directement en base ; l'écran « Ma fiche » ne gère que la
  création ou la modification de la fiche d'un compte déjà existant.