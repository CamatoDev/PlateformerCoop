package com.vortexmakers.plateformer.systems;


// IMPORTATIONS ===========================================
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.vortexmakers.plateformer.entities.Collectible;
import com.vortexmakers.plateformer.entities.Player;
import com.vortexmakers.plateformer.entities.Platform;

/**
 * PHYSICSSYSTEM - GÈRE LES COLLISIONS ENTRE LE JOUEUR ET LES PLATEFORMES
 *
 * Responsabilités :
 * - Détecter les collisions entre le joueur et les plateformes
 * - Résoudre les collisions (empêcher le joueur de traverser)
 * - Mettre à jour l'état du joueur (grounded) en fonction des collisions
 *
 * Ce système est appelé à chaque frame par GameScreen.
 */
public class PhysicsSystem {

    /**
     * VÉRIFIE ET RÉSOUT LES COLLISIONS ENTRE LE JOUEUR ET LES PLATEFORMES
     *
     * @param player Le joueur
     * @param platforms La liste des plateformes
     */
    public void checkCollisions(Player player, Array<Platform> platforms) {
        // On commence par supposer que le joueur n'est sur aucune plateforme
        boolean grounded = false;

        // On récupère la hitbox du joueur
        Rectangle playerBounds = player.getBounds();

        // On parcourt toutes les plateformes
        for (Platform platform : platforms) {
            Rectangle platformBounds = platform.getBounds();

            // Si le joueur entre en collision avec cette plateforme
            if (playerBounds.overlaps(platformBounds)) {
                // On résoud la collision et on met à jour l'état grounded
                grounded = resolveCollision(player, platform) || grounded;
                // Note: on utilise OR pour que si au moins une plateforme
                // rend le joueur grounded, il reste grounded
            }
        }

        // Met à jour l'état grounded du joueur
        // Si aucune plateforme ne le supporte, il n'est pas grounded
        player.setGrounded(grounded);
    }

    /**
     * RÉSOUT UNE COLLISION ENTRE LE JOUEUR ET UNE PLATEFORME
     *
     * @param player Le joueur
     * @param platform La plateforme
     * @return true si le joueur est maintenant sur cette plateforme (grounded), false sinon
     */
    private boolean resolveCollision(Player player, Platform platform) {
        // On récupère les hitboxes
        Rectangle playerBounds = player.getBounds();
        Rectangle platformBounds = platform.getBounds();

        // CALCUL DES CHEVAUCHEMENTS DANS LES 4 DIRECTIONS
        // Ces valeurs représentent "combien" le joueur pénètre dans la plateforme
        float overlapLeft = playerBounds.x + playerBounds.width - platformBounds.x;
        float overlapRight = platformBounds.x + platformBounds.width - playerBounds.x;
        float overlapBottom = playerBounds.y + playerBounds.height - platformBounds.y;  // CORRIGÉ
        float overlapTop = platformBounds.y + platformBounds.height - playerBounds.y;   // CORRIGÉ

        // TROUVER LA PLUS PETITE PÉNÉTRATION
        // Cela nous indique par quel côté le joueur est principalement entré
        float minOverlap = Math.min(Math.min(overlapLeft, overlapRight),
            Math.min(overlapTop, overlapBottom));

        // RÉSOLUTION DE LA COLLISION SELON LA DIRECTION
        if (minOverlap == overlapTop) {
            // COLLISION PAR LE HAUT : le joueur atterrit sur la plateforme
            // C'est le cas le plus courant : le joueur tombe sur une plateforme

            // On positionne le joueur juste au-dessus de la plateforme
            player.getPosition().y = platformBounds.y + platformBounds.height;

            // On arrête la chute (vitesse verticale à 0)
            player.setVelocityY(0);

            // Le joueur est maintenant grounded
            return true;

        } else if (minOverlap == overlapBottom) {
            // COLLISION PAR LE BAS : le joueur heurte le plafond
            // Ex: en sautant et heurtant une plateforme au-dessus

            // On positionne le joueur juste en-dessous de la plateforme
            player.getPosition().y = platformBounds.y - platformBounds.height;

            // On arrête le mouvement vers le haut
            player.setVelocityY(0);

            // Le joueur n'est pas grounded (il est "collé" au plafond)
            return false;

        } else if (minOverlap == overlapLeft) {
            // COLLISION PAR LA GAUCHE : le joueur heurte le côté droit d'une plateforme
            // Ex: en se déplaçant vers la droite

            // On positionne le joueur juste à gauche de la plateforme
            player.getPosition().x = platformBounds.x - playerBounds.width;

            // On pourrait arrêter le mouvement horizontal ici si on veut
            // player.getVelocity().x = 0;

            return false;

        } else if (minOverlap == overlapRight) {
            // COLLISION PAR LA DROITE : le joueur heurte le côté gauche d'une plateforme
            // Ex: en se déplaçant vers la gauche

            // On positionne le joueur juste à droite de la plateforme
            player.getPosition().x = platformBounds.x + platformBounds.width;

            // On pourrait arrêter le mouvement horizontal ici si on veut
            // player.getVelocity().x = 0;

            return false;
        }

        // Par défaut, le joueur n'est pas grounded
        return false;
    }

    /**
     * VÉRIFICATION DES COLLISIONS AVEC LES COLLECTIBLES
     *
     * @param player Le joueur
     * @param collectibles Liste des collectibles
     * @return Le nombre de collectibles ramassés pendant cette frame
     *
     * Pourquoi retourner un count :
     * → Permet à GameScreen de mettre à jour le score
     * → Donne du feedback sur l'efficacité des collisions
     */
    public int checkCollectibleCollisions(Player player, Array<Collectible> collectibles) {
        int collectedCount = 0;
        Rectangle playerBounds = player.getBounds();

        for (Collectible collectible : collectibles) {
            // Vérifier seulement les collectibles disponibles
            if (collectible.isAvailable() && playerBounds.overlaps(collectible.getBounds())) {
                collectible.collect();
                collectedCount++;

                System.out.println("Collision avec collectible détectée !");
            }
        }

        return collectedCount;
    }
}
