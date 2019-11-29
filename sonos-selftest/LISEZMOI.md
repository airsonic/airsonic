Airsonic Sonos integration Self Test 
===

Ensemble de tests fournis par Sonos afin de validé l'implémentation de Sonos Music API (SMAPI). 

Cet ensemble de tests est écrit en Python et peut être téléchargé à l'adresse suivante : 

    https://musicpartners.sonos.com/sites/default/files/PythonSelfTest.tar.gz

Voir la documentation à l'adresse suivante : 

    https://musicpartners.sonos.com/?q=node/134

Intégration dans l'environnement Airsonic 
===

La structure de sonos, (music, db, ect...) se trouve dans le répertoire : 

    main/resources/airsonic.

La source des tests téléchargés en .tar.gz de la version 1.2.0.697, se trouve dans main/resources et est décompressé dans main/python/PythonSelfTest

Avant de lancer les test, Python2 doit être installé sur la machine et les dépendances des tests aussi, voir : 
    main/python/PythonSelfTest/README

Structure des données de test 
===
La description des tests pour sonos est dans le fichier : 

    main/python/PythonSelfTest/smapi/service_configs/airsonic.cfg

Dans la BD il y a deux utilisateur/mot de passe :  

    admin/admin et test/test. l'utilisateur "test" est utilisé pour les tests.

Il y a un enregistrement des données nécessaires pour le lien avec Sonos.

La BD contient quelques albums au format flac. 

Les formats supportés par Sonos peuvent être retrouvés ici :

    https://developer.sonos.com/build/content-service-add-features/supported-audio-formats/

Exécution des tests 
===

1) Copier sonos-selftest/src/main/resources/airsonic vers /var/airsonic ou faire un un lien symbolique 

    ```shell script
    sudo ln -s /home/moi/workspace/airsonic/sonos-selftest/src/main/resources/airsonic /var/airsonic    
    ```

2) Démarrer airsonic sous tomcat (ou autre)  déployé au root "/", avec l'adresse 4040. Sinon vous pouvez changer dans airsonic.cfg. Utiliser les paramètres suivants :

    ```properties
    -Dairsonic.home=/var/airsonic/data
    -Dairsonic.defaultMusicFolder=/var/airsonic/music
    -Dairsonic.defaultPodcastFolder=/var/airsonic/podcasts
    -Dairsonic.defaultPlaylistFolder=/var/airsonic/playlists
    -Djava.awt.headless=true
    ```

3) lancer les tests pythons dans un terminal dans le répertoire : sonos-selftest/src/main/python/PythonSelfTest/smapi/content_workflow, comme suit :

    ```shell script
    python2 suite_selftest.py --config airsonic.cfg
    ```
    
4) Le résultat se trouve dans : 

    sonos-selftest/src/main/python/PythonSelfTest/smapi/content_workflow/log_files/airsonic

Résultat en erreur 
===

Pour l'instant il y a un certain normbre de fail du a :

1) Test "ssl_validation test_use_secure_endpoint"

    Pourrais être corriger par la mise en place de https sur le déploiement de test

2) Warning sur le test "Search test_..."

    Non investigué

3) Test "HTTPBasedTests test_byte_range_seeking"

    Ces tests sont basés sur la lecture Stream, voir plus bas "authentification"

Authentification 
===

Lorsque sonos communique avec airsonic, l'authentification se passe par le token. Ce dernier contient le nom d'utilisateur.
Pour l'instant ce token n'expire pas, mais nous devrions mettre en place son renouvellement. 

Pour la lecture d'un morceau de musique, Sonos demande le lien de lecture de ce dernier via la requête getMediaUrl. Ce lien est un lien qui est standard pour les API de airsonic et contient un JWTToken. Ce dernier ne contient pas le nom de l'utilisateur et  Sonos n'est pas authentifié à Airsonic, il fonctionne avec le token sonos normalement pour s'authentifié. 

Alors sur le Get "ext/stream", airsonic authentifie la requête sous anonymous. Outre le fait que nous ne sommes pas authentifiés sur  l'utilisateur qui demande la requête via Sonos, airsonic permet quand même la lecture de la musique demandée.

Donc pour finaliser l'intégration correctement avec Sonos nous devrions :

1) Permettre une authentification lors de l'appel à l'API de airsonic

2) Limité l'accès à la musique qu'à l'utilisateur authentifié.

