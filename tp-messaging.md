# TP messaging #

Ce TP est la suite logique du TD. Vous allez implémenter un système de messaging fiable avec des files de messages persistentes.

## Démarrage du projet ##

Pour vous faire gagner du temps, un projet *message-queue* de démarrage vous est fourni. Commencez par l'ajouter à votre projet précédent de conteneurs au même niveau que le module *ioc-container*.

Dans le POM principal, ajoutez la référence au nouveau sous-module :

    <modules>
        <module>ioc-container</module>
        <module>message-queue</module>
    </modules>

Nous vous avons fourni les classes suivantes :

* *Message*
* *MessageQueue*
* *MqException*
* *Operand*

ainsi qu'un test pour *MessageQueue* : *MessageQueueTest*.

Vous disposez ainsi d'une implémentation opérationelle de file de message persistente, certe imparfaite.

## Implémentation du protocole applicatif ##

Les classes *MessageBroker* et *MessageBrokerClient* contiennent pour l'instant des implémentations vides, mais leurs méthodes publiques sont spécifiées.

De même, la classe *MessageBrokingTest* contient les tests que votre implémentation doit passer. Pour l'instant, les tests sont mis en commentaires.

Implémentez un protocole applicatif sur TCP au sein de ces classes. Servez-vous de *MessageBrokingTest* comme spécification, et décommentez au fur et à mesure ses méthodes pour valider votre implémentation.

## Améliorer *MessageQueue* ##

*MessageQueue* gaspille du stockage disque puisque les messages délivrés ne sont pas effacés.

Proposez un correctif qui ne casse pas les tests existants. Ajoutez un test dédié à cette évolution.
