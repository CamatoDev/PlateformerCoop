# 🎮 Kawaii Verse Coop

> Jeu de plateforme 2D multijoueur coopératif développé en Java avec LibGDX et KryoNet.

---

## 📊 Informations Générales

- **Nom** : Kawaii Verse Coop
- **Type** : Platformer 2D Coopératif (LAN)
- **Langage** : Java
- **Framework** : LibGDX 1.14.0
- **Réseau** : KryoNet 2.22 (P2P avec Server Authority)
- **Capacité** : 2 à 4 joueurs (réseau local)
- **Résolution fenêtre** : 1080 × 720
- **Résolution logique jeu** : 800 × 480

---

## 🎯 Présentation du Projet

Kawaii Verse Coop est un jeu de plateforme multijoueur coopératif conçu pour approfondir :

- La physique 2D
- L’architecture ECS adaptée
- La synchronisation réseau temps réel
- Le modèle Host Authority
- La gestion complète d’un projet jeu vidéo (prototype → version jouable)

Le projet est structuré en 3 grandes phases : prototype local, intégration réseau, fonctionnalités avancées.

---

## 🕹 Gameplay

Les joueurs doivent coopérer pour terminer un niveau avant la fin du timer.

### Fonctionnalités principales

- Mouvement horizontal fluide (accélération/décélération)
- Saut avec gravité réaliste
- Contrôle aérien réduit
- Caméra dynamique avec anticipation
- 60-80 collectibles par niveau
- Pièges (spikes)
- Système de mort et respawn
- Invincibilité temporaire après respawn
- Drapeau de fin de niveau
- Timer synchronisé (3 minutes)
- Classement final des joueurs

---

## 🌐 Multijoueur

### Architecture réseau

- Modèle P2P avec Server Authority
- Client-side prediction
- Server reconciliation
- Interpolation des joueurs distants
- Synchronisation à 20 Hz
- Mise à jour serveur à 30 Hz

### Synchronisation

- Positions et vélocités
- Animations (grounded state)
- Collectibles (état dynamique)
- Spikes
- Plateformes
- Timer
- Fin de niveau
- Respawn

### Messages réseau (11 types)

- PlayerJoinMessage
- PlayerLeaveMessage
- PlayerInputMessage
- GameStateMessage
- PlatformStateMessage
- CollectibleStateMessage
- SpikeStateMessage
- PlayerRespawnMessage
- GameTimerMessage
- FinishFlagStateMessage
- PlayerCharacterMessage

---

## 🏗 Architecture

### Pattern ECS Adapté

#### Entités
- Player
- Platform
- Collectible
- Spike
- FinishFlag

#### Systèmes
- PhysicsSystem (gravité, collisions, résolution)

#### Écrans
- LobbyScreen
- GameScreen
- LevelWinScreen
- GameOverScreen

#### Réseau
- NetworkManager
- ServerPlayer
- NetworkListener
- Messages réseau

#### Utilitaires
- AssetManager (Singleton)
- PlayerTextures
- Constants

---

## 🎨 Assets

Assets utilisés : Kenney Pixel Platformer Pack

- 5 personnages jouables (Beige, Green, Pink, Purple, Yellow)
- 25 animations au total
- Background multi-couches avec parallaxe
- Collectibles animés
- Pièges
- Drapeau animé

---

## ⚙️ Constantes clés

- Gravité : -900 unités/s²
- Jump Force : 400
- Vitesse max joueur : 230
- Largeur monde : 5000 unités
- Invincibilité : 2 secondes
- Temps limite : 180 secondes

---

## 🚀 Optimisations

### Graphiques
- Pixel perfect (filtre Nearest)
- Batch rendering
- Culling des entités hors écran

### Réseau
- TCP / UDP selon priorité
- Envoi différentiel des états
- Séquençage des inputs

### Mémoire
- Dispose propre
- Nettoyage joueurs déconnectés
- Gestion centralisée des assets

---

## 🧪 Tests

### Solo
- Mouvement
- Collisions
- Mort / respawn
- Victoire / défaite

### Multijoueur (2-4 joueurs)
- Synchronisation positions
- Synchronisation animations
- Respawn synchronisé
- Collectibles synchronisés
- Timer synchronisé
- Victoire collective

Testé avec succès sur 2 PC différents en LAN.

---

## 📊 Métriques

- ~6500 lignes de code
- ~25 fichiers Java
- 11 types de messages réseau
- ~30 assets

---

## 🎯 Objectifs atteints

✔ Réseau P2P fonctionnel  
✔ Synchronisation temps réel stable  
✔ Gameplay complet  
✔ Architecture modulaire  
✔ Écrans de fin fonctionnels  
✔ Projet jouable et présentable  

---

## 🔮 Améliorations futures

### Court terme
- Liste joueurs en lobby
- Ready / Not Ready
- Sons et musique
- Second niveau

### Moyen terme
- Plateformes mobiles
- Checkpoints
- Menu sélection niveau

### Long terme
- Ennemis avec IA
- Power-ups
- Boss
- Éditeur de niveaux
- Progression complète

---

## 📌 Conclusion

Kawaii Verse Coop est un projet multijoueur complet démontrant :

- Maîtrise de la physique 2D
- Gestion avancée du réseau
- Architecture propre et maintenable
- Capacité à mener un projet de bout en bout

Projet finalisé à 95% — prêt pour démonstration académique ou professionnelle.

# libGDX

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and an `ApplicationAdapter` extension that draws libGDX logo.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
