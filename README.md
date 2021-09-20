*Released `org.reflections:reflections:0.10`*

*Reflections library has ~4 million downloads per month from Maven Central, and is being used by thousands of projects and libraries.  
Thank you for your continuous support! And apologize for the issues. We're looking for community collaborators to assist in reviewing pull requests and issues, please reach out.*

# Java runtime metadata analysis, in the spirit of [Scannotations](http://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/)

Reflections scans and indexes your project's classpath, allowing reverse query of the type system metadata on runtime.

Using Reflections you can query for example:
  * Subtypes of a type
  * Types annotated with an annotation
  * Methods with annotation, parameters, return type
  * Resources found in classpath  

While traditional Java Reflection API provides the ability to query direct super types, interfaces, annotations, members etc - 
this library allows reverse and transitive query for sub types, annotated with, methods by signature and return type etc.

## Usage
Add Reflections dependency to your project. 
```xml
<dependency>
    <groupId>org.reflections</groupId>
    <artifactId>reflections</artifactId>
    <version>0.10</version>
</dependency>
```

```groovy
// or Gradle
implementation 'org.reflections:reflections:0.10'
```

First, create Reflections instance with scanning configuration:
```java
Reflections reflections = new Reflections("com.my.project");
```

Then query the indexed metadata, such as:
```java
Set<Class<? extends SomeType>> subTypes =
  reflections.get(SubTypes.of(SomeType.class));

Set<Class<?>> annotated = 
  reflections.get(TypesAnnotated.with(SomeAnnotation.class));
```

### Scan
Creating Reflections instance requires providing scanning configuration:

```java
// scan for:
//   urls in classpath that contain 'com.my.project' package
//   filter types starting with 'com.my.project'
//   use the default scanners SubTypes and TypesAnnotated
Reflections reflections = new Reflections("com.my.project");

// this is equivalent to using ConfigurationBuilder:
Reflections reflections = new Reflections(
  new ConfigurationBuilder()
    .setUrls(ClasspathHelper.forPackage("com.my.project"))
    .filterInputsBy(new FilterBuilder().includePackage("com.my.project"))
    .setScanners(Scanners.SubTypes, Scanners.TypesAnnotated));

// or another example
new Reflections("com.my.project", Scanners.values()) // all standard scanners
```

*See more in [Configuration](https://github.com/ronmamo/reflections/tree/master/src/main/java/org/reflections/Configuration.java)*  

Note that:
* If no scanners are configured, the default will be used - `SubTypes` and `TypeAnnotated`.
* Classloader can also be configured, which will be used for resolving runtime classes from names.
* Reflections [expands super types](http://ronmamo.github.io/reflections/org/reflections/Reflections.html#expandSuperTypes(Map)) by default. This solves some [problems](https://github.com/ronmamo/reflections/issues/65#issuecomment-95036047) with transitive urls are not scanned.

### Query
Once Reflections was instantiated and scanning was successful, it can be used for querying the indexed metadata.  
Standard [Scanners](https://github.com/ronmamo/reflections/tree/master/src/main/java/org/reflections/scanners/Scanners.java) are provided for query using `reflections.get(scanner)`  

```java
import static org.reflections.scanners.Scanners.*;

// SubTypes
Set<Class<? extends Module>> modules = 
    reflections.get(SubTypes.of(com.google.inject.Module.class));

// TypesAnnotated
Set<Class<?>> singletons = 
    reflections.get(TypesAnnotated.with(javax.inject.Singleton.class));

// MethodAnnotated
Set<Method> resources =
    reflections.get(MethodsAnnotated.with(javax.ws.rs.Path.class));

Set<Constructor> injectables = 
    reflections.get(ConstructorsAnnotated.with(javax.inject.Inject.class));

// FieldsAnnotated
Set<Field> ids = 
    reflections.get(FieldsAnnotated.with(javax.persistence.Id.class));

// Resources
Set<String> properties = 
    reflections.get(Resources.with(".*\\.properties"));
```

Member scanners:

```java
// MethodsReturn
Set<Method> voidMethods = reflections.get(MethodsReturn.with(void.class));

// MethodsSignature
Set<Method> someMethods = reflections.get(MethodsSignature.of(long.class, int.class));

// MethodsParameter
Set<Method> pathParam = reflections.get(MethodsParameter.of(PathParam.class));

// ConstructorsSignature
Set<Constructor> someConstructors = reflections.get(ConstructorsSignature.of(String.class));

// ConstructorsParameter
Set<Constructor> pathParam = reflections.get(ConstructorsParameter.of(PathParam.class));
```

See more examples in [ReflectionsQueryTest](https://github.com/ronmamo/reflections/tree/master/src/test/java/org/reflections/ReflectionsQueryTest.java)

Other scanners included in [scanners](https://github.com/ronmamo/reflections/tree/master/src/main/java/org/reflections/scanners) package: 
[MemberUsageScanner](https://github.com/ronmamo/reflections/tree/master/src/main/java/org/reflections/scanners/MemberUsageScanner.java), 
[MethodParameterNamesScanner](https://github.com/ronmamo/reflections/tree/master/src/main/java/org/reflections/scanners/MethodParameterNamesScanner.java) 

*Note that previous 0.9.x API is still supported though marked for removal*
<details>
  <summary>Compare Scanners and previous 0.9.x API</summary>

| Scanners | previous 0.9.x API
| -------- | -----------|
| `get(SubType.of(T))` | getSubTypesOf(T) |
| `get(TypesAnnotated.with(A))` | getTypesAnnotatedWith(A) |
| `get(MethodsAnnotated.with(A))` | getMethodsAnnotatedWith(A) |
| `get(ConstructorsAnnotated.with(A))` | getConstructorsAnnotatedWith(A) |
| `get(FieldsAnnotated.with(A))` | getFieldsAnnotatedWith(A) |
| `get(Resources.with(regex))` | getResources(regex) |
| `get(MethodsParameter.with(P))` | getMethodsWithParameter(P) |
| `get(MethodsSignature.of(P, ...))` | getMethodsWithSignature(P, ...) |
| `get(MethodsReturn.of(T))` | getMethodsReturn(T) |
| `get(ConstructorsParameter.with(P))` | getConstructorsWithParameter(P) |
| `get(ConstructorsSignature.of(P, ...))` | getConstructorsWithSignature(P, ...) |

</details>

## ReflectionUtils
Apart from scanned classpath metadata, ReflectionsUtils provides convenient way to query Java Reflection metadata using `ReflectionUtils.get()`:

```java
import static org.reflections.ReflectionUtils.*;

// SuperTypes
Set<Class<?>> superTypes = get(SuperTypes.of(T));

// Annotations
Set<Class<?>> superTypes = get(Annotations.of(T));
    
// Methods
Set<Class<?>> superTypes = get(Methods.of(T));

// Constructors
Set<Class<?>> superTypes = get(Constructors.of(T));

// Fields
Set<Class<?>> superTypes = get(Fields.of(T));
```

See more examples in [ReflectionUtilsQueryTest](https://github.com/ronmamo/reflections/tree/master/src/test/java/org/reflections/ReflectionUtilsQueryTest.java)

## QueryBuilder and QueryFunction
Each Scanner and ReflectionUtil helper implements
[QueryBuilder](https://github.com/ronmamo/reflections/tree/master/src/main/java/org/reflections/util/QueryBuilder.java), and provides function for:
* `get()` - function returns direct values
* `with()` or `of()` - function returns transitive closure values

For example, `SubTypes.get(T)` will return direct subtypes of T, while `SubTypes.of(T)` will return all subtypes of subtypes hierarchy of T.  
Same goes for `TypesAnnotated.get(A)` and `TypesAnnotated.with(A)`.

Next, each function implements [QueryFunction](https://github.com/ronmamo/reflections/tree/master/src/main/java/org/reflections/util/QueryFunction.java), 
and provides fluent functional interface for composing `filter()`, `map()`, `flatMap()`, `as()`, such that:

```java
Set<Method> getters = ReflectionUtils.get(
  Methods.of(someClass)
    .filter(withModifier(Modifier.PUBLIC))
    .filter(withPrefix("get").and(withParametersCount.of(0)))
    .as(Method.class));

Set<Class<?>> annotations = ReflectionUtils.get(
  Annotations.of(annotationClass)
    .map(Annotation::annotationType)
    .filter(a -> !a.getName().startsWith("java.")));
```

Further more advanced queries can compose Scanner functions and ReflectionUtils functions.  
For example this test from [ReflectionUtilsQueryTest](https://github.com/ronmamo/reflections/tree/master/src/test/java/org/reflections/ReflectionUtilsQueryTest.java#216), for getting merged annotations of rest controller endpoints: 

```java
// get all annotations of RequestMapping hierarchy (GetMapping, PostMapping, ...)
Set<Class<?>> metaAnnotations =
    reflections.get(TypesAnnotated.getAllIncluding(RequestMapping.class.getName()).asClass());

QueryFunction<Store, RequestMapping> function =
  // get all controller endpoint methods      
  MethodsAnnotated.with(metaAnnotations).as(Method.class)
    .map(method ->
      // get method + declaring class RequestMapping annotations   
      get(Annotations.of(method.getDeclaringClass())
        .add(Annotations.of(method))
        .filter(a -> metaAnnotations.contains(a.annotationType())))
        .stream()
        // merge annotations member values into a single hash map
        .collect(new AnnotationMergeCollector(method)))
    // map to Java annotation proxy    
    .map(map -> ReflectionUtils.toAnnotation(map, metaAnnotation));

Set<RequestMapping> mergedAnnotations = 
  reflections.get(mergedAnnotation);
```

Check the [tests](https://github.com/ronmamo/reflections/tree/master/src/test/java/org/reflections) folder for more examples and API usage

### Integrating into your build lifecycle
Although scanning can be done quickly on bootstrap time, it is sometime useful to persist the scanned metadata into xml/json file as part of the build lifecycle for generating resources.  
Later on, `Reflections.collect()` can be used to collect the pre-scanned resources and rebuild the Store metadata without scanning.   

*For Maven, see example using gmavenplus in the [reflections-maven](https://github.com/ronmamo/reflections-maven/) repository*

### Contribute
Pull requests are welcomed!!

The license is [WTFPL](http://www.wtfpl.net/), just do what the fuck you want to.  
Apache 2 license was added as well for your convenience.

This library is published as an act of giving and generosity, from developers to developers,
to promote true knowledge sharing, and a-hole free working environments. Not cooperating with douchebags!

Please feel free to use it, and to contribute to the developers community in the same manner. [Dāna](http://en.wikipedia.org/wiki/D%C4%81na)
[![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=WLN75KYSR6HAY) 

_Cheers_
