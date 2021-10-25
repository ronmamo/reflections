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
  reflections.get(SubTypes.of(TypesAnnotated.with(SomeAnnotation.class)).asClass());
```

Or using previous 0.9.x APIs, for example:

```java
Set<Class<? extends SomeType>> subTypes =
  reflections.getSubTypesOf(SomeType.class);

Set<Class<?>> annotated = 
  reflections.getTypesAnnotatedWith(SomeAnnotation.class);
```

*Note that there are some breaking changes with Reflections 0.10+, along with performance improvements and more functional API, see below.*

### Scan
Creating Reflections instance requires [ConfigurationBuilder](https://ronmamo.github.io/reflections/org/reflections/util/ConfigurationBuilder.html), typically configured with packages and [Scanners](https://ronmamo.github.io/reflections/org/reflections/scanners/Scanners.html) to use: 

```java
// typical usage: scan package with the default scanners SubTypes, TypesAnnotated
Reflections reflections = new Reflections(
  new ConfigurationBuilder()
    .forPackage("com.my.project")
    .filterInputsBy(new FilterBuilder().includePackage("com.my.project")));

// or similarly using the convenient constructor
Reflections reflections = new Reflections("com.my.project");
```

Other examples:
```java
import static org.reflections.scanners.Scanners.*;

// scan package with specific scanners
Reflections reflections = new Reflections(
  new ConfigurationBuilder()
    .forPackage("com.my.project")
    .filterInputsBy(new FilterBuilder().includePackage("com.my.project").excludePackage("com.my.project.exclude"))
    .setScanners(TypesAnnotated, MethodsAnnotated, MethodsReturn));

// scan package with all standard scanners
Reflections reflections = new Reflections("com.my.project", Scanners.values());
```

Note that:
* **Scanner must be configured in order to be queried, otherwise an empty result is returned**  
If not specified, default scanners will be used SubTypes, TypesAnnotated.  
For all standard scanners use `Scanners.values()`. See more scanners in the source [package](https://ronmamo.github.io/reflections/org/reflections/scanners).
* **All relevant URLs should be configured**   
Consider `.filterInputsBy()` in case too many classes are scanned.  
If required, Reflections will [expand super types](https://ronmamo.github.io/reflections/org/reflections/Reflections.html#expandSuperTypes(java.util.Map)) in order to get the transitive closure metadata without scanning large 3rd party urls.  
* Classloader can optionally be used for resolving runtime classes from names.

### Query
Once Reflections was instantiated and scanning was successful, it can be used for querying the indexed metadata.  

```java
import static org.reflections.scanners.Scanners.*;

// SubTypes
Set<Class<?>> modules = 
  reflections.get(SubTypes.of(Module.class).asClass());

// TypesAnnotated (*1)
Set<Class<?>> singletons = 
  reflections.get(TypesAnnotated.with(Singleton.class).asClass());

// MethodsAnnotated
Set<Method> resources =
  reflections.get(MethodsAnnotated.with(GetMapping.class).as(Method.class));

// FieldsAnnotated
Set<Field> ids = 
  reflections.get(FieldsAnnotated.with(Id.class).as(Field.class));

// Resources
Set<String> properties = 
  reflections.get(Resources.with(".*\\.properties"));
```

More scanners:

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

// ConstructorsAnnotated
Set<Constructor> injectables =
  reflections.get(ConstructorsAnnotated.with(Inject.class).as(Constructor.class));

// ConstructorsSignature
Set<Constructor> someConstructors = 
  reflections.get(ConstructorsSignature.of(String.class).as(Constructor.class));

// MethodParameterNamesScanner
List<String> parameterNames =
  reflections.getMemberParameterNames(member);

// MemberUsageScanner
Set<Member> usages =
  reflections.getMemberUsages(member)
```

*See more examples in [ReflectionsQueryTest](src/test/java/org/reflections/ReflectionsQueryTest.java).*

*Note that previous 0.9.x APIs are still supported*

<details>
  <summary><i>Compare Scanners and previous 0.9.x API (*)</i></summary>

| Scanners | previous 0.9.x API | previous Scanner |
| -------- | ------------------ | ------ |
| `get(SubType.of(T))` | getSubTypesOf(T) | ~~SubTypesScanner~~ |
| `get(SubTypes.of(`<br>&nbsp;&nbsp;&nbsp;&nbsp;`TypesAnnotated.with(A)))` | getTypesAnnotatedWith(A) *(1)*| ~~TypeAnnotationsScanner~~ | 
| `get(MethodsAnnotated.with(A))` | getMethodsAnnotatedWith(A) | ~~MethodAnnotationsScanner~~ | 
| `get(ConstructorsAnnotated.with(A))` | getConstructorsAnnotatedWith(A) *(2)*| ~~MethodAnnotationsScanner~~ | 
| `get(FieldsAnnotated.with(A))` | getFieldsAnnotatedWith(A) | ~~FieldAnnotationsScanner~~ | 
| `get(Resources.with(regex))` | getResources(regex) | ~~ResourcesScanner~~ | 
| `get(MethodsParameter.with(P))` | getMethodsWithParameter(P) *(3)*<br>~~getMethodsWithAnyParamAnnotated(P)~~| ~~MethodParameterScanner~~<br>*obsolete* | 
| `get(MethodsSignature.of(P, ...))` | getMethodsWithSignature(P, ...) *(3)<br>~~getMethodsMatchParams(P, ...)~~*| " | 
| `get(MethodsReturn.of(T))` | getMethodsReturn(T) *(3)*| " | 
| `get(ConstructorsParameter.with(P))` | getConstructorsWithParameter(P) *(3)<br>~~getConstructorsWithAnyParamAnnotated(P)~~*| " | 
| `get(ConstructorsSignature.of(P, ...))` | getConstructorsWithSignature(P, ...) *(3)<br>~~getConstructorsMatchParams(P, ...)~~*| " | 

*Note: `asClass()` and `as()` mappings were omitted*

*(1): The equivalent of `getTypesAnnotatedWith(A)` is `get(SubTypes.of(TypesAnnotated.with(A)))`, including SubTypes*  

*(2): MethodsAnnotatedScanner does not include constructor annotation scanning, use instead Scanners.ConstructorsAnnotated*  

*(3): MethodParameterScanner is obsolete, use instead as required:  
Scanners.MethodsParameter, Scanners.MethodsSignature, Scanners.MethodsReturn, Scanners.ConstructorsParameter, Scanners.ConstructorsSignature*
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
Set<URL>         resources    = get(Resources.with(T));

Set<Annotation>  annotations  = get(Annotations.of(T));
Set<Class<? extends Annotation>> annotationTypes = get(AnnotationTypes.of(T));
```

*Previous ReflectionUtils 0.9.x API is still supported though marked for removal, more info in the javadocs.*

## Query API
Each Scanner and ReflectionUtils function implements [QueryBuilder](https://ronmamo.github.io/reflections/org/reflections/util/QueryBuilder.html), and supports:
* `get()` - function returns direct values 
* `with()` or `of()` - function returns all transitive values

*For example, `Scanners.SubTypes.get(T)` return direct subtypes, 
while `Scanners.SubTypes.of(T)` return transitive subtypes hierarchy. 
Same goes for `Scanners.TypesAnnotated` and `ReflectionUtils.SuperTypes` etc.*

Next, each function implements [QueryFunction](https://ronmamo.github.io/reflections/org/reflections/util/QueryFunction.html), 
and provides fluent functional interface for composing `filter()`, `map()`, `flatMap()`, `as()` and more, such that:

```java
// filter, as/map
QueryFunction<Store, Method> getters =
  Methods.of(C1.class)
    .filter(withModifier(Modifier.PUBLIC))
    .filter(withPrefix("get").and(withParametersCount(0)))
    .as(Method.class);

// compose Scanners and ReflectionUtils functions 
QueryFunction<Store, Method> methods = 
  SubTypes.of(type).asClass()  // <-- classpath scanned metadata
    .flatMap(Methods::of);     // <-- java reflection api

// function of function
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
It is sometime useful to `Reflections.save()` the scanned metadata into xml/json as part of the build lifecycle for generating resources, 
and then collect it on bootstrap with `Reflections.collect()` and avoid scanning. *See [reflections-maven](https://github.com/ronmamo/reflections-maven/) for example*.
- [JavaCodeSerializer](https://ronmamo.github.io/reflections/org/reflections/serializers/JavaCodeSerializer.html) - scanned metadata can be persisted into a generated Java source code. 
Although less common, it can be useful for accessing types and members in a strongly typed manner. *(see [example](src/test/java/org/reflections/MyTestModelStore.java))*
- [AnnotationMergeCollector](https://ronmamo.github.io/reflections/org/reflections/util/AnnotationMergeCollector.html) - can be used to merge similar annotations. *(see [test](src/test/java/org/reflections/ReflectionUtilsQueryTest.java#L216))*
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
