# TD Réplication de cache #

Un cache peut être vu comme un ensemble de paires clé / valeur.

On les utilise dans de nombreux contextes en middlewares :

* sessions web coté serveur,
* rendu de fragments de HTML dynamiques,
* éviter les accès disque coûteux,
* coopération dans un cluster,
* ...

## API de cache ##

1. Proposer une interface Java pour un service de cache supportant les opérations suivantes :
    * insertion clé / valeur,
    * obtention via clé,
    * suppression via clé.
2. Proposer une implémentation en mémoire, adaptée à un usage local au sein d'une application.
3. On souhaite maintenant que des instances de caches sur un même réseau puissent partager le même état en répliquant leurs opérations. Quel est l'impact sur les valeurs pouvant être stockées dans le cache ? Pourquoi ?

## Protocole de réplication ##

Une API c'est bien ... une implémentation c'est encore mieux !

1. Quel protocole réseau serait adapté pour des communications entre *n* participants ?
2. Quels messages échanger entre ces participants ?
3. Concevez un protocole de réplication / synchronisation de cache qui réponde le plus favorablement possible aux questions suivantes.
    * Votre protocole est-il résistant aux pannes ?
    * Êtes-vous indépendant des horloges physiques des participants ? Les données peuvent-elles devenir incohérentes ?
    * Comment gérez-vous l'arrivée d'un nouveau participant ? Son départ ?
4. Que dire du traffic réseau induit par votre protocole ?

Justifiez vos réponses par des arguments scientifiques : contre-exemples et/ou preuves.
