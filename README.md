##Java runtime metadata analysis, in the spirit of [Scannotations](http://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/)

Reflections scans your classpath, indexes the metadata, allows you to query it on runtime and may save and collect that information for many modules within your project.

Using Reflections you can query your metadata such as:
  * get all subtypes of some type
  * get all types/members annotated with some annotation
  * get all resources matching a regular expression
  * get all methods with specific signature including parameters, parameter annotations and return type

[![Build Status](https://travis-ci.org/ronmamo/reflections.svg?branch=master)](https://travis-ci.org/ronmamo/reflections)

###Intro
Add Reflections to your project. for maven projects just add this dependency:
```xml
<dependency>
    <groupId>org.reflections</groupId>
    <artifactId>reflections</artifactId>
    <version>0.9.11</version>
</dependency>
```

A typical use of Reflections would be:
```java
Reflections reflections = new Reflections("my.project");

Set<Class<? extends SomeType>> subTypes = reflections.getSubTypesOf(SomeType.class);

Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(SomeAnnotation.class);
```

###Usage
Basically, to use Reflections first instantiate it with urls and scanners

```java
//scan urls that contain 'my.package', include inputs starting with 'my.package', use the default scanners
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
//SubTypesScanner
Set<Class<? extends Module>> modules = 
    reflections.getSubTypesOf(com.google.inject.Module.class);
```
```java
//TypeAnnotationsScanner 
Set<Class<?>> singletons = 
    reflections.getTypesAnnotatedWith(javax.inject.Singleton.class);
```
```java
//ResourcesScanner
Set<String> properties = 
    reflections.getResources(Pattern.compile(".*\\.properties"));
```
```java
//MethodAnnotationsScanner
Set<Method> resources =
    reflections.getMethodsAnnotatedWith(javax.ws.rs.Path.class);
Set<Constructor> injectables = 
    reflections.getConstructorsAnnotatedWith(javax.inject.Inject.class);
```
```java
//FieldAnnotationsScanner
Set<Field> ids = 
    reflections.getFieldsAnnotatedWith(javax.persistence.Id.class);
```
```java
//MethodParameterScanner
Set<Method> someMethods =
    reflections.getMethodsMatchParams(long.class, int.class);
Set<Method> voidMethods =
    reflections.getMethodsReturn(void.class);
Set<Method> pathParamMethods =
    reflections.getMethodsWithAnyParamAnnotated(PathParam.class);
```
```java
//MethodParameterNamesScanner
List<String> parameterNames = 
    reflections.getMethodParamNames(Method.class)
```
```java
//MemberUsageScanner
Set<Member> usages = 
    reflections.getMethodUsages(Method.class)
```

  * If no scanners are configured, the default will be used - `SubTypesScanner` and `TypeAnnotationsScanner`. 
  * Classloader can also be configured, which will be used for resolving runtime classes from names.
  * Reflections [expands super types](http://ronmamo.github.io/reflections/org/reflections/Reflections.html#expandSuperTypes()) by default. This solves some [problems](https://github.com/ronmamo/reflections/issues/65#issuecomment-95036047) with transitive urls are not scanned. 

*Checkout the [javadoc](http://ronmamo.github.io/reflections/index.html?org/reflections/Reflections.html) for more info.* 

*Also, browse the [tests directory](https://github.com/ronmamo/reflections/tree/master/src/test/java/org/reflections) to see some more examples.*

###ReflectionUtils
ReflectionsUtils contains some convenient Java reflection helper methods for getting types/constructors/methods/fields/annotations matching some predicates, generally in the form of *getAllXXX(type, withYYY)

for example:

```java
import static org.reflections.ReflectionUtils.*;

Set<Method> getters = getAllMethods(someClass,
  withModifier(Modifier.PUBLIC), withPrefix("get"), withParametersCount(0));

//or
Set<Method> listMethodsFromCollectionToBoolean = 
  getAllMethods(List.class,
    withParametersAssignableTo(Collection.class), withReturnType(boolean.class));

Set<Fields> fields = getAllFields(SomeClass.class, withAnnotation(annotation), withTypeAssignableTo(type));
```

*See more in the [ReflectionUtils javadoc](http://ronmamo.github.io/reflections/index.html?org/reflections/ReflectionUtils.html)*

###ClasspathHelper
ClasspathHelper contains some convenient methods to get urls for package, for class, for classloader and so.

*See more in the [ClasspathHelper javadoc](http://ronmamo.github.io/reflections/index.html?org/reflections/util/ClasspathHelper.html)*

###Integrating into your build lifecycle
Although scanning can be easily done on bootstrap time of your application - and shouldn't take long, it is sometime a good idea to integrate Reflections into your build lifecyle.
With simple Maven/Gradle/SBT/whatever configuration you can save all scanned metadata into xml/json files just after compile time. 
Later on, when your project is bootstrapping you can let Reflections collect all those resources and re-create that metadata for you, 
making it available at runtime without re-scanning the classpath - thus reducing the bootstrapping time.

*For Maven, see example using gmavenplus in the [reflections-maven](https://github.com/ronmamo/reflections-maven/) repository*

###Other use cases
*See the [UseCases](https://github.com/ronmamo/reflections/blob/gh-pages/UseCases.md) wiki page*

###Contribute
Pull requests are welcomed!!

The license is [WTFPL](http://www.wtfpl.net/), just do what the fuck you want to. 

This library is published as an act of giving and generosity, from developers to developers. 

Please feel free to use it, and to contribute to the developers community in the same manner. [Dāna](http://en.wikipedia.org/wiki/D%C4%81na)
[![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=WLN75KYSR6HAY) 

Welcome to join the [conversation](https://github.com/ronmamo/reflections/issues/159) and share your thoughts on the license issue.

_Cheers_
