# TD Messaging fiable #

L'objectif de ce TD est de vous faire étudier les mécanismes permettant de réaliser un système de communication fiable par messages asynchrones.

Nous allons mettre en oeuvre une solution de messaging simple en point-à-point. Les messages seront envoyés et consommmés dans une file, aussi appellée *"destination"*, et qui est simplement identifiée par un chaine de caractères.

Le serveur de messages se nomme *"le broker"* auqel se connectent des *"clients"*.

## APIs ##

Commençons par travailler sur la définition d'APIs pour ce système de messaging.

1. Donner une définition abrégée d'une classe Java pour représenter un message comportant une destination et un contenu, aussi appellé *payload*.
2. Même question pour une classe *MessageBroker* permettant de :
    * le démarrer sur un port TCP,
    * l'arrêter.
3. Même question de nouveau pour une classe *MessageBrokerClient* permettant de :
    * se connecter à un broker via le constructeur,
    * envoyer un message dans une file,
    * vérifier si un message est disponible depuis une file,
    * recevoir un message depuis une file,
    * fermer la connection au broker.

## File de messages ##

Un *MessageBroker* utilise un nombre variable de files, chacune portant un nom distinct. On se propose de représenter une file par une classe *MessageQueue*. Les instances de *MessageBroker* les utiliseront en interne, mais ne seront pas visibles au niveau du client (*MessageBrokerClient*).

1. Quelle structure de donnée utiliser pour gérer une file ? Quelle interface Java ? Quelle implémentation ? Justifiez vos choix.
2. Proposer une API pour une classe *MessageQueue* qui délègue à une structure interne de file choisie en (1).
3. Pourquoi de simples envois / réceptions de messages TCP ne sont-ils pas suffisants pour garantir la fiabilité au niveau applicatif des échanges de messages entre un broker et ses clients ? Justifier votre réponse en exposant les failles d'une implémentation naive où un message est considéré comme étant reçu à partir du moment où il a été envoyé ou reçu depuis une connection TCP.
4. Concevoir un protocole applicatif basé sur TCP pour une émission et une livraison fiable de messages. Discutez des limites de sa robustesse en fonction des choix d'implémentation.

## File de messages persistente ##

Dans la section précédente, nous avons supposé que les messages étaient stockés en mémoire dans les files. Ainsi, l'arrêt du broker entraine la perte des messages.

Nous souhaitons rendre les files *persistentes*, c'est à dire que :

* les messages sont stockés physiquement, et
* les files de messages supportent la reprise après panne.

1. Proposez un mécanisme de stockage basé sur la sérialization Java vers un fichier en ajout seul.
2. Quelles sont les faiblesses en cas de panne ?
3. Proposer un mécanisme qui les corrige.
4. Proposer un algorithme de reprise après panne.
5. Risquez-vous d'avoir des problèmes de stockage disque manquant à terme ? Comment y remédier ?
