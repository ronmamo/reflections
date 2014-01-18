Java runtime metadata analysis, in the spirit of [Scannotations](http://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/)
==============================================================================================================================================

Reflections scans your classpath, indexes the metadata, allows you to query it on runtime and may save and collect that information for many modules within your project.

Using Reflections you can query your metadata such as:
  * get all subtypes of some type
  * get all types/constructos/methods/fields annotated with some annotation, optionally with annotation parameters matching
  * get all resources matching matching a regular expression
  * get all methods with specific signature including parameters, parameter annotations and return type

Intro
-----
Add Reflections to your project. for maven projects just add this dependency:

```xml
<dependency>
    <groupId>org.reflections</groupId>
    <artifactId>reflections</artifactId>
    <version>0.9.9-RC1</version>
</dependency>
```

A typical use of Reflections would be:

```java
Reflections reflections = new Reflections("my.project");

Set<Class<? extends SomeType>> subTypes = reflections.getSubTypesOf(SomeType.class);

Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(SomeAnnotation.class);
```

Usage
-----
Basically, to use Reflections first instantiate it with Urls and Scanners

```java
//scan Urls that contain 'my.package', include inputs starting with 'my.package', use the default scanners
Reflections reflections = new Reflections("my.package");

//or using ConfigurationBuilder
new Reflections(new ConfigurationBuilder()
     .setUrls(ClasspathHelper.forPackage("my.project.prefix"))
     .setScanners(new SubTypesScanner(), 
                  new TypeAnnotationsScanner().filterResultsBy(optionalFilter), ...),
     .filterInputsBy(new FilterBuilder().includePackage("my.project.prefix"))
     ...);
```
Then use the convenient query methods: (depending on the scanners configured)

```java
Set<Class<? extends Module>> modules = reflections.getSubTypesOf(com.google.inject.Module.class);
Set<Class<?>> singletons =             reflections.getTypesAnnotatedWith(javax.inject.Singleton.class);

Set<String> properties =       reflections.getResources(Pattern.compile(".*\\.properties"));
Set<Constructor> injectables = reflections.getConstructorsAnnotatedWith(javax.inject.Inject.class);
Set<Method> deprecateds =      reflections.getMethodsAnnotatedWith(javax.ws.rs.Path.class);
Set<Field> ids =               reflections.getFieldsAnnotatedWith(javax.persistence.Id.class);

Set<Method> someMethods =      reflections.getMethodsMatchParams(long.class, int.class);
Set<Method> voidMethods =      reflections.getMethodsReturn(void.class);
Set<Method> pathParamMethods = reflections.getMethodsWithAnyParamAnnotated(PathParam.class);
Set<Method> floatToString =    reflections.getConverters(Float.class, String.class);
```

There are some convenient methods in ClasspathHelper to get URLs for package, for class, for classloader and so.
If no scanners are configured, the default one will be used - SubTypesScanner and TypeAnnotationsScanner.
Other scanners can be configured as well, such as ResourcrsScanner, MethodAnnotationsScanner, ConstructorAnnotationsScanner, FieldAnnotationsScanner, MethodParameterScanner or any custom scanner.
A classloader also can be configured, which will be used for resolving runtime classes from names.

*Browse the [javadoc](http://reflections.googlecode.com/svn/trunk/reflections/javadoc/apidocs/index.html?org/reflections/Reflections.html) for more info. Also, browse the [tests directory](http://code.google.com/p/reflections/source/browse/#svn/trunk/reflections/src/test/java/org/reflections) to see some more examples.*

----

ReflectionUtils
---------------
Reflections also contains some convenient java reflection helper methods for getting types/constructors/methods/fields/annotations matching some predicates, generally in the form of *getAllXXX(type, withYYY)

for example:

```java
import static org.reflections.ReflectionUtils.*;

Set<Method> getters = getAllMethods(someClass,
  withModifier(Modifier.PUBLIC), withPrefix("get"), withParametersCount(0));

//or
Set<Method> listMethods = getAllMethods(List.class,
  withParametersAssignableTo(Collection.class), withReturnType(boolean.class));

Set<Fields> fields = getAllFields(SomeClass.class, withAnnotation(annotation), withTypeAssignableTo(type));
```

*See more in the [ReflectionUtils javadoc](http://reflections.googlecode.com/svn/trunk/reflections/javadoc/apidocs/org/reflections/ReflectionUtils.html)*

Reflections Maven plugin
------------------------
With simple configuration you can save all scanned metadata into xml files on compile time. 
Later on, when your project is bootstrapping you can let Reflections collect all those resources and re-create that metadata for you, 
making it available at runtime without re-scanning the classpath - thus reducing the bootstrapping time.

Use this maven configuration in your pom file:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.reflections</groupId>
            <artifactId>reflections-maven</artifactId>
            <version>the latest version...</version>
            <executions>
                <execution>
                    <goals>
                        <goal>reflections</goal>
                    </goals>
                    <phase>process-classes</phase>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Then, on runtime:

```java
Reflections reflections =
        isProduction() ? Reflections.collect() : new Reflections("your.package.here");
```

*Check the [ReflectionsMojo](http://code.google.com/p/reflections/wiki/ReflectionsMojo) wiki page*

Other use cases
---------------
Reflections can also:
  * scan urls in parallel
  * serialize scanned metadata to xml/json
  * collect saved metadata on bootstrap time for fastest load time without scanning
  * save your model entities metadata as .java file, so you can reference types/fields/methods/annotation in a static manner
  * initial [Spring component scan](https://code.google.com/p/reflections/wiki/ReflectionsSpring)

*See the [UseCases](http://code.google.com/p/reflections/wiki/UseCases) wiki page*

Contribute
----------
You can easily extend Reflections by creating your specialized Scanner class and provide a query method on the Store object

Patches and extension are welcomed!

The license is [WTFPL](http://www.wtfpl.net/), just do what the fuck you want to. this library is given as an act of giving and generosity, [Dāna](http://en.wikipedia.org/wiki/D%C4%81na)
[![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=WLN75KYSR6HAY)

_Cheers_
