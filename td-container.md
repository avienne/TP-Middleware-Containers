# TD Conteneurs #

L'objectif de ce TD est d'étudier les mécanismes à mettre en oeuvre pour la conception d'un conteneur à composants locaux avec injection de dépendances.

## Gestion de dépendances ##

Soit un composant avec l'interface suivante :

    public interface Echo {
        public String echo(String str);
    }

Voici une implémentation simpliste de ce composant :

    public class SimpleEcho implements Echo {
        public String echo(String str) {
            return str;
        }
    }

On considère désormais un composant avec l'interface suivante :

    public interface EchoClient {
        public String run();
    }

et l'implémentation suivante :

    public class SomeEchoClient implements EchoClient {
        private final Echo echo;
        private final String message;

        public SomeEchoClient(Echo echo, String message) {
            this.echo = echo;
            this.message = message;
        }

        public String run() {
            return echo.echo(message);
        }
    }

1. Donner les dépendances de chaque classe concrète.
2. Écrire une classe *Main* permettant d'utiliser une instance de *EchoClient* ayant pour implémentation *SomeEchoClient*.

Nous décidons d'ajouter au système un composant compteur :

    public interface Counter {
        public int get();
        public int increment();
    }

ainsi qu'une implémentation en mémoire non-concurrente :

    public class SomeCounter implements Counter {
        private int value = 0;

        public int get() {
            return value;
        }

        public int increment() {
            value = value + 1;
            return value;
        }
    }

Nous proposons un nouveau composant client :

    public interface EchoCounterClient {
        String echoNextIncrement();
    }
    
avec l'implémentation suivante :

    public class SomeEchoCounterClient implements EchoCounterClient {
        private final Echo echo;
        private final Counter counter;

        public SomeEchoCounterClient(Echo echo, Counter counter) {
            this.echo = echo;
            this.counter = counter;
        }

        public String echoNextIncrement() {
            return echo.echo(valueOf(counter.increment()));
        }
    }

1. Donner les dépendances de chaque classe concrète.
2. Écrire une classe *Main* permettant d'utiliser une instance de *EchoCounterClient* ayant pour implémentation *SomeEchoCounterClient*.
3. Les dépendances peuvent-elles être données autrement que via les constructeurs ? Que penser des différents styles ?

## Inversion de contrôle ##

Si l'assemblage de composants reste aisé avec quelques classes, il peut se révéler plus fastidieux sur de grands graphes de classes.

Nous allons ainsi réaliser une *inversion de controle* pour l'assemblage de nos composants, c'est à dire que nous allons déléguer ce travail à un objet dont c'est le rôle.

Supposons que cet objet existe.

1. Proposez une interface Java pour cet assembleur.
2. Proposez du code client qui configure l'assembleur afin de lui indiquer les correspondances entre interfaces et implémentations.
3. Proposez du code client pour obtenir une instance d'un composant *EchoClient* puis d'un composant *EchoCounterClient* depuis l'assembleur.

## Conteneur à injection de dépendances ##

Ce que nous avons appellé *"assembleur"* dans la section précédante est en réalité un conteneur de composants. Il s'occupe de gérer les composants et leur cycle de vie. De l'extérieur, le code client s'adresse au conteneur pour obtenir des composants, d'où la mise en oeuvre d'une inversion de contrôle. Notez également que le conteneur permet de s'abstraire des classes concrètes qui implémentent ces composants.

### Algorithme d'assemblage ###

Pour assembler les composants, le conteneur va devoir non seulement les instancier, mais aussi les configurer en leur passant leurs dépendances, qui pourront également être des composants gérés par le conteneur.

1. Comment spécifier la liaison entre une interface de composant et son implémentation ? Comment gérer les composants au sein du conteneur ?
2. Sachant que Java supporte des mécanismes de réflexivité (inspection de classes, chargement dynamique, instanciation, invocation de méthodes, etc), proposez un algorithme qui permette d'instancier un composant et ses dépendances.

### Injection au delà des composants ###

Certaines dépendances peuvent ne pas être des composants mais des objets et/ou valeurs quelconques. Prenons la variante suivante du composant *Echo* :

    public class SomeEcho implements Echo {
        private final String prefix;
        private final String suffix;

        public SomeEcho(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        public String echo(String str) {
            return prefix + str + suffix;
        }
    }

Quel est le problème rencontré par le conteneur ?

Sachant que Java supporte les annotations sur les paramètres, proposez les modifications nécessaires à l'interface de celui-ci ainsi qu'à votre algorithme d'assemblage de façon à pouvoir qualifier des valeurs à injecter comme dans cet exemple :

    public class SomeEcho implements Echo {
        private final String prefix;
        private final String suffix;

        public SomeEcho(@Named("prefix") String prefix, @Named("suffix") String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        public String echo(String str) {
            return prefix + str + suffix;
        }
    }

### Politique d'instanciation ###

Quand un client demande un composant au conteneur, ce dernier peut gérer une politique d'instanciation à sa discrétion. Nous allons gérer 2 politiques :

* **NEW** : un nouveau composant est instancié et assemblé à chaque requête, et
* **SINGLETON** : une seule instance existe, instanciée et assemblée pour la première requête puis mise en cache pour les suivantes.

1. Proposez une modification de l'interface du conteneur pour pouvoir déclarer une politique d'instanciation.
2. Modifiez votre algorithme d'assemblage en conséquence.

### Variance d'implémentation ###

On souhaite pouvoir offrir plusieurs implémentations pour la même interface. Pour cela, on se propose de *"qualifier"* un lien interface-implémentation avec une chaîne de caractères et l'annotation *@Named* comme dans cet exemple :

    public class SomeEchoCounterClient implements EchoCounterClient {
        private final Echo echo;
        private final Counter counter;

        public SomeEchoCounterClient(Echo echo, @Named("shared") Counter counter) {
            this.echo = echo;
            this.counter = counter;
        }

        public String echoNextIncrement() {
            return echo.echo(valueOf(counter.increment()));
        }
    }
    
1. Quel est l'impact sur la façon d'associer une interface à une implémentation ?
2. Quel est l'impact sur l'interface du conteneur ?
3. Quel est l'impact sur votre algorithme d'assemblage ?

### Délégation ###

On souhaite mettre en place un mécanisme de délégation entre conteneurs. Ainsi un conteneur doit pouvoir chercher des composants et valeurs dans un autre conteneur s'il ne peut pas toutes les résoudre localement.

Comme dans les questions précédantes, discuter de l'impact sur l'interface du conteneur et sur l'algorithme d'assemblage.
