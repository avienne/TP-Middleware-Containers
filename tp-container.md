# TP Conteneur #

Ce TP est la mise en oeuvre d'un conteneur à composants comme détaillé en TD.

## Démarrage ##

Un projet Maven vous est fourni pour démarrer. **Prenez le temps de l'explorer !**

Une vue simplifiée de l'interface d'un conteneur est la suivante :

    public interface Container {
        
        Container declare(Binding binding);
        Container define(String name, Object value);

        <T> T obtainReference(Class<T> interfaceClass);
        <T> T obtainReference(Class<T> interfaceClass, String qualifier);

        Object definitionValue(String name);

        boolean hasReferenceDeclaredFor(Class<?> interfaceClass);
        boolean hasReferenceDeclaredFor(Class<?> interfaceClass, String qualifier);
        boolean hasValueDefinedFor(String name);

        Container delegateTo(Container container);
    }
    
Le lien interface - implémentation se fait via la classe *Binding* (simplifiée) suivante ainsi que le type énuméré *Policy* et la classe *Key* :

    public final class Binding {

        public static enum Policy {
            SINGLETON, NEW
        }

        public static final class Key {
            private final Class<?> interfaceClass;
            private final String qualifier;

            public Key(Class<?> interfaceClass, String qualifier) {
                this.interfaceClass = interfaceClass;
                this.qualifier = qualifier;
            }

            public Class<?> getInterfaceClass() {
                return interfaceClass;
            }

            public String getQualifier() {
                return qualifier;
            }
        }

        private final Class<?> interfaceClass;
        private final Class<?> implementationClass;
        private final String qualifier;
        private final Policy policy;

        public Binding(Class<?> interfaceClass, Class<?> implementationClass, String qualifier, Policy policy) {
            this.interfaceClass = interfaceClass;
            this.implementationClass = implementationClass;
            this.qualifier = qualifier;
            this.policy = policy;
        }

        public Class<?> getInterfaceClass() {
            return interfaceClass;
        }

        public Class<?> getImplementationClass() {
            return implementationClass;
        }

        public String getQualifier() {
            return qualifier;
        }

        public Policy getPolicy() {
            return policy;
        }

        public Key getKey() {
            return new Key(interfaceClass, qualifier);
        }
    }

La définition de l'annotation *@Named* est la suivante :

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Named {
        String value();
    }

Enfin, pour traiter les cas d'erreurs nous vous proposons de lever une *unchecked exception*, c'est à dire une exception pour laquelle le compilateur ne vous force pas de les gérer et déclarer :

    public class ContainerException extends RuntimeException {
        public ContainerException() {
        }

        public ContainerException(String s) {
            super(s);
        }

        public ContainerException(String s, Throwable throwable) {
            super(s, throwable);
        }

        public ContainerException(Throwable throwable) {
            super(throwable);
        }
    }

Pour implémenter le conteneur, nous vous proposons la classe *LocalContainer* du package *midcontainers.local* que vous devrez implémenter :

    public class LocalContainer implements Container {
        // TODO
    }

## Tests ##

Nous vous donné une série de tests unitaires et des composants pour ceux-ci.

Le package *midcontainers.components* de *src/test/java* contient quelques composants simples comme :

    public class SomeCounter implements Counter {

        private int value = 0;

        @Override
        public int get() {
            return value;
        }

        @Override
        public int increment() {
            value = value + 1;
            return value;
        }
    }

La classe *midcontainers.local.LocalContainerTest* contient l'ensemble des tests unitaires. Ils utilisent ces composants pour verifier que votre implémentation de conteneur est correcte.

Nous les avons tous mis en commentaires car initialement votre conteneur n'est pas implémenter. Maven exécute ces tests automatiquement. Par exemple le test suivant vérifie que l'injection fonctionne :

    @Test
    public void check_injection() {
        Container container = new LocalContainer();
        container
                .declare(new Binding(Echo.class, SomeEcho.class, null, NEW))
                .declare(new Binding(EchoClient.class, SomeEchoClient.class, null, NEW))
                .define("prefix", "[ ")
                .define("suffix", " ]")
                .define("message", "hello");

        EchoClient client = container.obtainReference(EchoClient.class);
        assertThat(client.run(), is("[ hello ]"));
    }


**Nous vous invitons à les décommenter au fur et à mesure. Ces tests sont une spécification : votre travail sera correct si et seulement si les tests passent sans que vous les modifiez.**

## Conseils ##

Votre conteneur devra gérer des collections d'objets diverses.

Lorsque nous avons mis en oeuvre ce TP, nous avons utilisé les définitions suivantes :

    private final Map<Binding.Key, Binding> bindings = new HashMap<Binding.Key, Binding>();
    private final Map<String, Object> definitions = new HashMap<String, Object>();
    private final Map<Binding.Key, Object> singletons = new HashMap<Binding.Key, Object>();
    private final List<Container> delegates = new LinkedList<Container>();

Bien entendu vous êtes libres de faire autrement.

Les APIs d'instrospection en Java sont dans le package *java.util.reflect*.

Voici quelques extraits de code faisant de l'introspection, à vous de jouer pour implémenter correctement votre mécanisme d'assemblage.

Récupérer les constructeurs d'une classe :

    Constructor<?>[] constructors = binding.getImplementationClass().getConstructors();

Récupérer les types de paramètres d'un constructeur et les éventuelles annotations qui ont peu être déclarées sur ceux-ci :

    Class<?>[] parameterTypes = constructor.getParameterTypes();
    Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();

Instancier un objet via son constructeur (*parameterValues* est un *Object[]*):

    constructor.newInstance(parameterValues);

Récupérer la valeur d'un attribut *@Named* pour un paramètre :

    private String qualifierNameFor(Annotation[] parameterAnnotations) {
        for (Annotation annotation : parameterAnnotations) {
            if (annotation instanceof Named) {
                return ((Named) annotation).value();
            }
        }
        return null;
    }
