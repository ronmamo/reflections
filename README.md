*Released `org.reflections:reflections:0.10.1`*

*Reflections library has ~4 million downloads per month from Maven Central, and is being used by thousands of [projects](https://github.com/ronmamo/reflections/network/dependents) and [libraries](https://mvnrepository.com/artifact/org.reflections/reflections/usages).  
Thank you for your continuous support! And apologize for the issues. We're looking for community collaborators to assist in reviewing pull requests and issues, please reach out.*

# Java runtime metadata analysis

[![Build Status](https://travis-ci.org/ronmamo/reflections.svg?branch=master)](https://travis-ci.org/ronmamo/reflections)

Reflections scans and indexes your project's classpath metadata, allowing reverse transitive query of the type system on runtime.

Using Reflections you can query for example:
  * Subtypes of a type
  * Types annotated with an annotation
  * Methods with annotation, parameters, return type
  * Resources found in classpath  
And more...

*Reflections was written in the spirit of [Scannotations](http://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/) library*

## Usage
Add Reflections dependency to your project:
```xml
# Maven
<dependency>
    <groupId>org.reflections</groupId>
    <artifactId>reflections</artifactId>
    <version>0.10.1</version>
</dependency>

# Gradle
implementation 'org.reflections:reflections:0.10.1'
```

Create Reflections instance and use the query functions: 
```java
Reflections reflections = new Reflections("com.my.project");

Set<Class<?>> subTypes =
  reflections.get(SubTypes.of(SomeType.class).asClass());

Set<Class<?>> annotated = 
  reflections.get(TypesAnnotated.with(SomeAnnotation.class).asClass());
```

*Note that there are some breaking changes with Reflections 0.10+, along with performance improvements and more functional API. Migration is encouraged and should be easy though.*

### Scan
Creating Reflections instance requires providing scanning configuration:

```java
// scan for:
//   urls in classpath that contain 'com.my.project' package
//   filter types starting with 'com.my.project'
//   use the default scanners SubTypes and TypesAnnotated
Reflections reflections = new Reflections(
  new ConfigurationBuilder()
    .forPackage("com.my.project")
    .filterInputsBy(new FilterBuilder().includePackage("com.my.project")));

// or similarly
Reflections reflections = new Reflections("com.my.project");

// another example
Reflections reflections = new Reflections(
  new ConfigurationBuilder()
    .addUrls(ClasspathHelper.forPackage("com.my.project")) // same as forPackage
    .setScanners(Scanners.values())     // all standard scanners
    .filterInputsBy(new FilterBuilder() // optionally include/exclude packages 
      .includePackage("com.my.project")
      .excludePackage("com.my.project.exclude")));
```

*See more in [ConfigurationBuilder](https://ronmamo.github.io/reflections/org/reflections/util/ConfigurationBuilder.html).*

Note that:
* **Scanners must be configured in order to be queried, otherwise an empty result is returned.**  
If not specified, default scanners are `SubTypes` and `TypesAnnotated`. For all standard [Scanners](https://ronmamo.github.io/reflections/org/reflections/scanners/Scanners.html) use `Scanners.values()` [(src)](src/main/java/org/reflections/scanners/Scanners.java).
* **All relevant URLs should be configured.**   
If required, Reflections will [expand super types](https://ronmamo.github.io/reflections/org/reflections/Reflections.html#expandSuperTypes(java.util.Map)) in order to get the transitive closure metadata without scanning large 3rd party urls.  
Consider adding inputs filter in case too many classes are scanned.  
* Classloader can optionally be used for resolving runtime classes from names.

### Query
Once Reflections was instantiated and scanning was successful, it can be used for querying the indexed metadata.  
Standard [Scanners](https://ronmamo.github.io/reflections/org/reflections/scanners/Scanners.html) are provided for query using `reflections.get()`, for example:  

```java
import static org.reflections.scanners.Scanners.*;

// SubTypes
Set<Class<?>> modules = 
  reflections.get(SubTypes.of(Module.class).asClass());

// TypesAnnotated
Set<Class<?>> singletons = 
  reflections.get(TypesAnnotated.with(Singleton.class).asClass());

// MethodAnnotated
Set<Method> resources =
  reflections.get(MethodsAnnotated.with(GetMapping.class).as(Method.class));

// ConstructorsAnnotated
Set<Constructor> injectables = 
  reflections.get(ConstructorsAnnotated.with(Inject.class).as(Constructor.class));

// FieldsAnnotated
Set<Field> ids = 
  reflections.get(FieldsAnnotated.with(Id.class).as(Field.class));

// Resources
Set<String> properties = 
  reflections.get(Resources.with(".*\\.properties"));
```

Member scanners:

```java
// MethodsReturn
Set<Method> voidMethods = 
  reflections.get(MethodsReturn.with(void.class).as(Method.class));

// MethodsSignature
Set<Method> someMethods = 
  reflections.get(MethodsSignature.of(long.class, int.class).as(Method.class));

// MethodsParameter
Set<Method> pathParam = 
  reflections.get(MethodsParameter.of(PathParam.class).as(Method.class));

// ConstructorsSignature
Set<Constructor> someConstructors = 
  reflections.get(ConstructorsSignature.of(String.class).as(Constructor.class));

// ConstructorsParameter
Set<Constructor> pathParam = 
  reflections.get(ConstructorsParameter.of(PathParam.class).as(Constructor.class));
```

*See more examples in [ReflectionsQueryTest](src/test/java/org/reflections/ReflectionsQueryTest.java).*

Scanner queries return `Set<String>` by default, if not using `as() / asClass()` mappers:
```java
Set<String> moduleNames = 
  reflections.get(SubTypes.of(Module.class));

Set<String> singleNames = 
  reflections.get(TypesAnnotated.with(Singleton.class));
```
Note that previous 0.9.x API is still supported, for example:
```java
Set<Class<?>> modules = 
  reflections.getSubTypesOf(Module.class);

Set<Class<?>> singletons = 
  reflections.getTypesAnnotatedWith(Singleton.class);
```
<details>
  <summary>Compare Scanners and previous 0.9.x API</summary>

| Scanners | previous 0.9.x API |
| -------- | ------------------ |
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

*Note: `asClass()` and `as()` mappings were omitted*
</details>

## ReflectionUtils
Apart from scanning classpath metadata using [Javassist](https://github.com/jboss-javassist/javassist), 
Java Reflection convenient methods are available using 
[ReflectionsUtils](https://ronmamo.github.io/reflections/org/reflections/ReflectionUtils.html):

```java
import static org.reflections.ReflectionUtils.*;

Set<Class<?>>    superTypes   = get(SuperTypes.of(T));
Set<Field>       fields       = get(Fields.of(T));
Set<Constructor> constructors = get(Constructors.of(T));
Set<Methods>     methods      = get(Methods.of(T));
Set<Annotation>  annotations  = get(Annotations.of(T));
Set<Class<? extends Annotation>> annotationTypes = get(AnnotationTypess.of(T));
```

*Previous ReflectionUtils 0.9.x API is still supported though marked for removal, more info in the javadocs.*

## QueryBuilder and QueryFunction
Each Scanner and ReflectionUtils function implements [QueryBuilder](https://ronmamo.github.io/reflections/org/reflections/util/QueryBuilder.html), and supports:
* `get()` - function returns direct values 
* `with()` or `of()` - function returns all transitive values

*For example, `Scanners.SubTypes.get(T)` return direct subtypes, 
while `Scanners.SubTypes.of(T)` return transitive subtypes hierarchy. 
Same goes for `Scanners.TypesAnnotated` and `ReflectionUtils.SuperTypes` etc.*

Next, each function implements [QueryFunction](https://ronmamo.github.io/reflections/org/reflections/util/QueryFunction.html), 
and provides fluent functional interface for composing `filter()`, `map()`, `flatMap()`, `as()` and more, such that:

```java
QueryFunction<Store, Method> getters =
  Methods.of(C1.class)
    .filter(withModifier(Modifier.PUBLIC))
    .filter(withPrefix("get").and(withParametersCount(0)))
    .as(Method.class);
```

Query functions can be composed, for example:
```java
// compose Scanner and ReflectionUtils functions 
QueryFunction<Store, Method> methods = 
  SubTypes.of(type).asClass()  // <-- classpath scanned metadata
    .flatMap(Methods::of);     // <-- java reflection api

// compose function of function
QueryFunction<Store, Class<? extends Annotation>> queryAnnotations = 
  Annotations.of(Methods.of(C4.class))
    .map(Annotation::annotationType);
```

See more in [ReflectionUtilsQueryTest](https://github.com/ronmamo/reflections/tree/master/src/test/java/org/reflections/ReflectionUtilsQueryTest.java)  

A more complex example demonstrates getting merged annotations of rest controllers endpoints:
```java
// get all annotations of RequestMapping hierarchy (GetMapping, PostMapping, ...)
Set<Class<?>> metaAnnotations =
  reflections.get(TypesAnnotated.getAllIncluding(RequestMapping.class.getName()).asClass());

QueryFunction<Store, Map<String, Object>> queryAnnotations =
  // get all controller endpoint methods      
  MethodsAnnotated.with(metaAnnotations).as(Method.class)
    .map(method ->
      // get both method's + declaring class's RequestMapping annotations   
      get(Annotations.of(method.getDeclaringClass())
        .add(Annotations.of(method))
        .filter(a -> metaAnnotations.contains(a.annotationType())))
        .stream()
        // merge annotations' member values into a single hash map
        .collect(new AnnotationMergeCollector(method)));

// apply query and map merged hashmap into java annotation proxy
Set<RequestMapping> mergedAnnotations = 
  reflections.get(mergedAnnotation
    .map(map -> ReflectionUtils.toAnnotation(map, metaAnnotation)));
```

Check the [tests](src/test/java/org/reflections) folder for more examples and API usage

### What else?
- **Integrating with build lifecycle**  
It is sometime useful to save the scanned metadata into xml/json as part of the build lifecycle for generating resources, 
and then collect it on bootstrap with `Reflections.collect()` and avoid scanning. *See [reflections-maven](https://github.com/ronmamo/reflections-maven/) for example*.
- [JavaCodeSerializer](https://ronmamo.github.io/reflections/org/reflections/serializers/JavaCodeSerializer.html) - scanned metadata can be persisted into a generated Java source code. 
Although less common, it can be useful for accessing types and members in a strongly typed manner. *(see [example](src/test/java/org/reflections/MyTestModelStore.java))*
- [AnnotationMergeCollector](https://ronmamo.github.io/reflections/org/reflections/util/AnnotationMergeCollector.html) - can be used to merge similar annotations, for example for finding effective REST controller endpoints. *(see [test](src/test/java/org/reflections/ReflectionUtilsQueryTest.java#L216))*
- `MemberUsageScanner` - experimental scanner allow querying for member usages `getMemberUsages()` of packages/types/elements in the classpath.
Can be used for finding usages between packages, layers, modules, types etc.  

### Contribute
Pull requests are welcomed!!  
Here are some issues labeled with [please contribute :heart:](https://github.com/ronmamo/reflections/issues?q=is%3Aissue+is%3Aopen+label%3A%22please+contribute+%E2%9D%A4%EF%B8%8F%22+label%3A%22good+first+issue%22)  
*We're looking for community collaborators to assist in reviewing pull requests and issues, please reach out.*

Dual licenced with Apache 2 and [WTFPL](http://www.wtfpl.net/), just do what the fuck you want to.  

*This library is published as an act of giving and generosity, from developers to developers,
to promote knowledge sharing and a--hole free working environments.  
Please feel free to use it, and to contribute to the developers' community in the same manner.*  
[PayPal](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=WLN75KYSR6HAY) / [Patreon](https://www.patreon.com/ronma)  

_Cheers_
