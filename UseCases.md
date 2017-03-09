Below are some useful use cases for using Reflections: 

* Bootstrap in a multi module environment 
* Collect pre scanned metadata 
* Serialize Reflections into a java source file, and use it to statically reference java elements 
* Query the store directly, avoid definition of types in class loader 
* Find resources in your classpath (for example all properties files) 

###Bootstrap in a multi module environment

In a multi module project, where each module is responsible for it's properties, jpa entities and maybe guice modules, use Reflections to collect that metadata and bootstrap the application 
``` 
    Reflections reflections = new Reflections(new ConfigurationBuilder() 
        .addUrls(ClasspathHelper.forPackage("your.package.here"), ClasspathHelper.forClass(Entity.class), ClasspathHelper.forClass(Module.class)) 
        .setScanners(new ResourcesScanner(), new TypeAnnotationsScanner(), new SubTypesScanner()));

    Set<String> propertiesFiles = reflections.getResources(Pattern.compile(".*\\.properties"));
    Properties allProperties = createOneBigProperties(propertiesFiles);

    Set<Class<?>> jpaEntities = reflections.getTypesAnnotatedWith(Entity.class);
    SessionFactory sessionFactory = createOneBigSessionFactory(jpaEntities, allProperties);

    Set<Class<? extends Module>> guiceModules = reflections.getSubTypesOf(Module.class);
    Injector injector = createOneBigInjector(guiceModules);
```

###Collect pre scanned metadata

Although scanning can be easily done on bootstrap time of your application - and shouldn't take long, 
it is sometime a good idea to save all scanned metadata into xml/json files just after compile time, 
and later on, when your project is bootstrapping you can let Reflections collect all those resources and avoid scanning.

So first make sure Reflections scanned metadata is saved into a file in your source/resources folder. 
This can be done using your [build tool](https://github.com/ronmamo/reflections-maven#reflections-maven-plugin) (preferred), 
or [programatically](http://ronmamo.github.io/reflections/org/reflections/Reflections.html#save(java.lang.String)):
```
reflections.save("src/main/resources/META-INF/reflections/resource1-reflections.xml");
```

Then, on runtime, collect these pre saved metadata and instantiate Reflections 
```
Reflections reflections = isProduction() ? Reflections.collect() : new Reflections("your.package.here");
```

###Serialize Reflections into a Java source file, and use it to statically reference java elements

Reflections can serializes types and types elements into interfaces respectively to fully qualified name.
First, save Reflections metadata using 
```
  reflections.save(filename, new JavaCodeSerializer());
```
(filename should be in the pattern: `path/path/path/package.package.classname`)

The saved file should look like:
```
 public interface MyModel {
  public interface my {
   public interface package1 {
    public interface MyClass1 {
     public interface fields {
      public interface f1 {}
      public interface f2 {}
     }
     public interface methods {
      public interface m1 {}
      public interface m2 {}
     }
...
}
```

Then you can reference a method/field/annotation descriptor in a statically typed manner:
```
  Class m1Ref = MyModel.my.package1.MyClass1.methods.m1.class;
```
And use the helper methods in `JavaCodeSerializer` to resolve into the runtime elements:
```
 Method method = JavaCodeSerializer.resolve(m1Ref);
```

###Query the store directly, avoid definition of types in class loader

Querying through Reflections results in classes defined by the class loader. this is usually not a problem, but in cases class definition is not desirable, you can query the store directly using strings only Reflections reflections = new Reflections(...); //see in other use cases Set<String> serializableFqns = reflections.getStore().getSubTypesOf("java.io.Serializable"); plus, you can create your specialized query methods by querying the store directly Map<String, Multimap<String, String>> storeMap = reflections.getStore().getStoreMap(); //or Multimap<String, String> scannerMap = reflections.getStore().get(ResourcesScanner.class);

find resources in your classpath

``` Reflections reflections = new Reflections(new ConfigurationBuilder() .setUrls(ClasspathHelper.forPackage("your.package.here")) .setScanners(new ResourcesScanner());

    Set<String> propertiesFiles = reflections.getResources(Pattern.compile(".*\\.properties"));
    Set<String> hibernateCfgFiles = reflections.getResources(Pattern.compile(".*\\.cfg\\.xml"));
```

###Find resources in your classpath

``` 
  Reflections reflections = new Reflections(new ConfigurationBuilder() 
    .setUrls(ClasspathHelper.forPackage("your.package.here")) 
    .setScanners(new ResourcesScanner());

  Set<String> propertiesFiles = reflections.getResources(Pattern.compile(".*\\.properties"));
  Set<String> hibernateCfgFiles = reflections.getResources(Pattern.compile(".*\\.cfg\\.xml"));
```
