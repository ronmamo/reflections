*Released `org.reflections:reflections:0.10`*

*Reflections library has ~4 million downloads per month from Maven Central, and is being used by thousands of [projects](https://github.com/ronmamo/reflections/network/dependents) and [libraries](https://mvnrepository.com/artifact/org.reflections/reflections/usages).  
Thank you for your continuous support! And apologize for the issues. We're looking for community collaborators to assist in reviewing pull requests and issues, please reach out.*

# Java runtime metadata analysis, in the spirit of [Scannotations](http://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/)

Reflections scans and indexes your project's classpath, allowing reverse transitive query of the type system metadata on runtime.

Using Reflections you can query for example:
  * Subtypes of a type
  * Types annotated with an annotation
  * Methods with annotation, parameters, return type
  * Resources found in classpath  

## Usage
Add Reflections dependency to your project:
```xml
# Maven
<dependency>
    <groupId>org.reflections</groupId>
    <artifactId>reflections</artifactId>
    <version>0.10</version>
</dependency>

# Gradle
implementation 'org.reflections:reflections:0.10'
```

Create Reflections instance and use the query functions: 
```java
Reflections reflections = new Reflections("com.my.project");

Set<Class<? extends SomeType>> subTypes =
  reflections.get(SubTypes.of(SomeType.class).asClass());

Set<Class<?>> annotated = 
  reflections.get(TypesAnnotated.with(SomeAnnotation.class).asClass());
```

*Note that there are some breaking changes with Reflections 0.10, along with performance improvements and more functional API. Migration is encouraged and should be easy though.*

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

*See more in [ConfigurationBuilder](src/main/java/org/reflections/util/ConfigurationBuilder.java).*

Note that:
* **Scanners must be configured in order to be queried, otherwise an empty result is returned.**  
If not specified, default scanners are `SubTypes` and `TypesAnnotated`. For all standard [Scanners](src/main/java/org/reflections/scanners/Scanners.java) use `Scanners.values()`.
* **All relevant URLs should be configured.**   
If required, Reflections will [expand super types](http://ronmamo.github.io/reflections/org/reflections/Reflections.html#expandSuperTypes(Map)) in order to get the transitive closure metadata without scanning large 3rd party urls.  
Consider adding inputs filter in case too many classes are scanned.  
* Classloader can optionally be used for resolving runtime classes from names.

### Query
Once Reflections was instantiated and scanning was successful, it can be used for querying the indexed metadata.  
Standard [Scanners](src/main/java/org/reflections/scanners/Scanners.java) are provided for query using `reflections.get()`, for example:  

```java
import static org.reflections.scanners.Scanners.*;

// SubTypes
Set<Class<? extends Module>> modules = 
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
Set<Class<? extends Module>> modules = 
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

</details>

## ReflectionUtils
Apart from scanning classpath metadata using [Javassist](https://github.com/jboss-javassist/javassist), 
Java Reflection convenient methods are available using 
[ReflectionsUtils](src/main/java/org/reflections/ReflectionUtils.java):

```java
import static org.reflections.ReflectionUtils.*;

Set<Class<?>>    superTypes   = get(SuperTypes.of(T));
Set<Annotation>  annotations  = get(Annotations.of(T));
Set<Methods>     methods      = get(Methods.of(T));
Set<Constructor> constructors = get(Constructors.of(T));
Set<Field>       fields       = get(Fields.of(T));
```

*Previous ReflectionUtils 0.9.x API is still supported though marked for removal, more info in the javadocs.*

## QueryBuilder and QueryFunction
Each Scanner and ReflectionUtils helper implements
[QueryBuilder](src/main/java/org/reflections/util/QueryBuilder.java), and provides function for:
* `get()` - function returns direct values 
* `with()` or `of()` - function returns all transitive values

For example, `Scanners.SubTypes.get(T)` return direct subtypes of T, 
while `Scanners.SubTypes.of(T)` return all subtypes hierarchy of T. 
Same goes for `Scanners.TypesAnnotated` and `ReflectionUtils.SuperTypes` etc.

Next, each function implements [QueryFunction](src/main/java/org/reflections/util/QueryFunction.java), 
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
  SubTypes.of(type).asClass()
    .flatMap(Methods::of);

// compose function of function
QueryFunction<Store, Class<? extends Annotation>> queryAnnotations = 
  Annotations.of(Methods.of(C4.class))
    .map(Annotation::annotationType);
```

See more in [ReflectionUtilsQueryTest](https://github.com/ronmamo/reflections/tree/master/src/test/java/org/reflections/ReflectionUtilsQueryTest.java)  

A more complex example demonstrates how to get merged annotations of rest controllers endpoint:
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

Set<RequestMapping> mergedAnnotations = 
  reflections.get(mergedAnnotation
    // map to Java annotation proxy    
    .map(map -> ReflectionUtils.toAnnotation(map, metaAnnotation)));
```

Check the [tests](src/test/java/org/reflections) folder for more examples and API usage

### What else?
- **Integrating with build lifecycle**  
It is sometime useful to save the scanned metadata into xml/json as part of the build lifecycle for generating resources, 
and then collect it on bootstrap with `Reflections.collect()` and avoid scanning. *See [reflections-maven](https://github.com/ronmamo/reflections-maven/) for example*.

- [JavaCodeSerializer](src/main/java/org/reflections/scanners/JavaCodeSerializer.java) - scanned metadata can be persisted into a generated Java source code. Can be used Although less common, it might be useful for accessing type members in a strongly typed manner.

- [AnnotationMergeCollector](src/main/java/org/reflections/util/AnnotationMergeCollector.java) - can be used to merge similar annotations.

- [MemberUsageScanner](src/main/java/org/reflections/scanners/MemberUsageScanner.java) - experimental scanner allow querying for static usages of packages/types/elements within the project.  

### Contribute
Pull requests are welcomed!!

Dual licenced with Apache 2 and [WTFPL](http://www.wtfpl.net/), just do what the fuck you want to.  

*This library is published as an act of giving and generosity, from developers to developers,
to promote knowledge sharing and a--hole free working environments.  
Please feel free to use it, and to contribute to the developers' community in the same manner.*  

[![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=WLN75KYSR6HAY) 

_Cheers_
