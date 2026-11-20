<div align="center">

<img src="assets/logo.png" alt="SkyHand" width="140">

# SHItemSigns

*Poser un objet sur un panneau, en 3D, et le reprendre d'un clic (remplace InteractiveSigns).*

<img src="https://img.shields.io/badge/Paper-26.3-f2a900?style=for-the-badge" alt="Paper 26.3"> <img src="https://img.shields.io/badge/Java-25-e76f00?style=for-the-badge" alt="Java 25"> <img src="https://img.shields.io/badge/SkyHand-Opale-3aa655?style=for-the-badge" alt="SkyHand Opale">

</div>

---

## Présentation

| Action | Comment |
|---|---|
| **Poser** | clic droit sur la face vide d'un panneau avec un objet en main : un exemplaire est posé, en 3D |
| **Reprendre** | clic gauche sur la face : l'objet revient dans l'inventaire (à ses pieds si l'inventaire est plein) |
| **Cirer** | rayon de miel sur une face occupée : plus personne n'y pose ni n'y reprend d'objet |
| **Retirer la cire** | clic droit avec une hache sur un panneau ciré (la hache perd un point de solidité) |

Panneaux sur pied, muraux et suspendus (au plafond ou au mur), les deux faces. La face visée est celle que le jeu choisit pour écrire : le côté où se tient le joueur. Un panneau mural ne prend d'objet que devant ; un objet resté derrière (côté mur) se reprend depuis l'avant. Une face qui porte du texte ne prend pas d'objet, et accroupi le clic droit garde son effet normal.

Quand le panneau est cassé (à la main, par une explosion, le feu, un piston, décroché parce que le bloc qui le tient est retiré, recouvert par un arbre qui pousse), ses objets tombent au sol, une seule fois. Un panneau qui disparaît sans être cassé (WorldEdit, plugin qui change le bloc) emporte son objet : il est supprimé au chargement de son chunk et, chunk chargé, à la vérification suivante (toutes les 5 s par défaut). Aucun objet ne reste en l'air sans panneau.

Utilisé sur : **Opale**. Minecraft 26.3, aucune dépendance obligatoire.

## Protections

Poser demande de pouvoir construire, reprendre de pouvoir casser (casser le panneau rendrait l'objet de toute façon).

- **SuperiorSkyblock2** : permission d'île `BUILD` pour poser, `BREAK` pour reprendre (membres et coop par défaut, réglable par l'île). Dans le monde des îles, hors de toute île : refusé. L'île du spawn suit les régions WorldGuard.
- **WorldGuard** : il faut pouvoir construire dans la région (membre, flag `build`, contournement WorldGuard). Un clic déjà refusé par une région ou une île garde le message de celle-ci.
- **Mode aventure** : refusé.
- `shitemsigns.bypass` passe outre.

## Comment c'est fait

- L'objet est un **`item_display` vanilla** au centre du bloc du panneau, sauvegardé avec le chunk comme un cadre : rien en base, rien dans un fichier, rien à perdre en cas d'arrêt brutal. Ses données (PDC) ne portent que la face occupée (`shitemsigns:side`) ; le panneau et l'objet se retrouvent par leur position.
- **Serveur** : aucun parcours de monde. Chaque événement sort à la première ligne si le bloc n'est pas un panneau (test de type). Un `item_display` ne fait rien à chaque tick. Seule tâche répétée : la vérification des objets des chunks chargés (liste tenue au chargement et au déchargement des chunks, un test de type de bloc par objet).
- **Réseau** : l'entité est entièrement réglée avant son ajout au monde, chaque joueur la reçoit en une fois à son arrivée à portée ; rien n'est renvoyé ensuite tant que personne n'y touche. Un objet trop lourd (livre écrit, boîte de shulker remplie...) est refusé (`taille-max-objet`), puisque chaque joueur à portée le recevrait en entier.
- **Client** : chaque objet a une boîte de culling (1 × 1 bloc, que le client agrandit encore d'un demi-bloc) et une distance d'affichage (32 blocs par défaut). Il n'est dessiné que s'il est à l'écran.
- **Doublons impossibles** : poser, reprendre et lâcher se font dans l'événement, sur le thread du monde, dans le même tick. L'entité est créée avant de retirer l'objet de la main (si le monde la refuse, l'objet reste en main) ; elle est retirée avant de rendre l'objet. Un double clic ou deux joueurs sur la même face voient la face déjà occupée, ou déjà vide.
- **InteractiveSigns** : ses objets (marque `minecraft:interactivesigns.front` / `.back`) sont repris au chargement de leur chunk, même des jours plus tard : marque convertie, boîte de culling et distance ajoutées, objet et position inchangés. Si InteractiveSigns est encore installé, SHItemSigns ne démarre pas (les deux ensemble lâcheraient chaque objet deux fois).

## Commandes

| Commande | Description | Permission |
|---|---|---|
| `/shitemsigns reload` | Recharge `config.yml` et `messages.yml` ; si un fichier est illisible, l'ancienne configuration reste active. | `shitemsigns.admin.reload` |
| `/shitemsigns clear [rayon]` | Reprend les objets des panneaux autour de soi (rayon 1 par défaut, 16 au plus) et les met dans son inventaire. | `shitemsigns.admin.clear` |

Alias : `/interactivesings`, `/ins` (les noms d'InteractiveSigns). `reload` fonctionne depuis la console.

## Permissions

| Permission | Description | Par défaut |
|---|---|---|
| `shitemsigns.use` | Poser et reprendre des objets. | `true` |
| `shitemsigns.bypass` | Poser et reprendre malgré les protections d'île et de région. | `op` |
| `shitemsigns.admin` | Toutes les sous-commandes de `/shitemsigns` (parent de `shitemsigns.admin.<sous-commande>`). | `op` |
| `shitemsigns.admin.reload` `.clear` | Une sous-commande. | `op` |

## Configuration

Fichiers par défaut du plugin (valeurs d'exemple). Cliquez sur un fichier pour l'ouvrir :

<pre>
plugins/SHItemSigns/
├── <a href="src/main/resources/config.yml">config.yml</a>
└── <a href="src/main/resources/messages.yml">messages.yml</a>
</pre>

`config.yml` contient la distance d'affichage, l'intervalle de vérification des objets sans panneau, la taille maximale d'un objet et les sons ; `messages.yml` tous les textes. Les clés sont expliquées en commentaire.

## Compilation

Java 25 et Maven :

```bash
mvn clean package
```

Le jar est généré dans `target/`. WorldGuard 7.0.20 et SuperiorSkyblock2 sont facultatifs.

---

<div align="center">

<sub>Développé par <b>Cry_Legende</b> pour le serveur <b>SkyHand</b> · Tous droits réservés</sub>

</div>
