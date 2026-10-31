# Astra Launcher

Lanceur Android écrit de zéro, sans aucune dépendance tierce : pas d’AndroidX, pas de Kotlin, pas de
moteur de rendu externe. Une seule activité, une seule classe, et l’APK tient en quelques dizaines
de kilo-octets.

## Ce qu’il fait

- grille d’applications alimentée par `PackageManager`, triée par nom ;
- recherche instantanée (les favoris remontent en tête) ;
- appui court : lance l’application ; appui long : ajoute ou retire des favoris, persistés dans les
  `SharedPreferences` ;
- en-tête avec date et heure, rafraîchi toutes les 30 s ;
- boîte de réglages : rappel des gestes et remise à zéro des favoris ;
- le bouton retour ramène à l’écran d’accueil au lieu de fermer le lanceur ;
- icône adaptative (API 26+) générée par `scripts/make-icon.py`.

## Compiler

Prérequis : JDK 17 et le SDK Android (`platforms;android-35`, `build-tools;35.0.0`). Le wrapper
Gradle fixe la version de l’outil (8.9), donc la machine et la CI compilent à l’identique.

```bash
export ANDROID_HOME=$HOME/Android/Sdk     # ou local.properties
./gradlew assembleDebug                   # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease                 # -> app/build/outputs/apk/release/app-release.apk
```

`assembleRelease` minifie avec R8 et signe. Sans `keystore.properties`, il retombe sur la clé de
debug : l’APK s’installe mais ne se publie pas. Pour publier, copier `keystore.properties.example`
vers `keystore.properties`, puis :

```bash
keytool -genkeypair -v -keystore astra.jks -keyalg RSA -keysize 2048 -validity 10000 -alias astra
```

La clé et le fichier de signature ne sont jamais versionnés. La CI (`.github/workflows/`)
exécute les deux variantes à chaque push et publie les APK en artefacts.

## Pourquoi un si petit APK

Mesuré sur `app-release.apk` (64,3 Ko au total) :

| Part | Taille | % |
|---|---:|---:|
| Icône (10 mipmaps PNG + vecteurs) | 40,4 Ko | 63 % |
| Code (dex unique) | 10,4 Ko | 16 % |
| Signature et métadonnées | 4,8 Ko | 7 % |
| Ressources XML | 3,2 Ko | 5 % |
| Table des ressources | 3,1 Ko | 5 % |
| Manifeste | 2,4 Ko | 4 % |

Autrement dit : **les deux tiers du fichier sont l’icône**. Le code lui-même tient en 10 Ko parce
qu’il n’embarque rien — aucune bibliothèque, une seule classe, et une interface construite
entièrement en Java plutôt qu’en layouts XML. À titre de comparaison, un lanceur du commerce pèse
30 Mo : bibliothèques publicitaires et d’analyse, moteur d’icônes, ressources pour toutes les
densités, souvent du code natif. Astra ne fait pas la même chose, il ne fait que l’essentiel.

## Icône

L’icône est générée, pas dessinée à la main : `scripts/make-icon.py` écrit le vecteur adaptatif et
les mipmaps depuis le même tracé, pour que la forme soit identique avant et après l’API 26.

```bash
python3 scripts/make-icon.py      # nécessite Pillow
```

## Licence

Aucune licence n’est encore choisie : par défaut, tous droits réservés. Ce dépôt contient
uniquement du code écrit pour ce projet ; il ne réutilise aucun code tiers ni aucun code décompilé.
