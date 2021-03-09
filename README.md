# Fluent API sentence end check
[![Released version](https://img.shields.io/maven-central/v/foundation.fluent.api/fluent-api-end-check.svg)](https://search.maven.org/#search%7Cga%7C1%7Cfluent-api-end-check)
[![Build Status](https://travis-ci.org/c0stra/fluent-api-end-check.svg?branch=master)](https://travis-ci.org/c0stra/fluent-api-end-check)

Compile time check for end of the method chain in fluent API.

With fluent Java API, you are describing a complex action to be done, using a chain of methods. It may be, that the
action only happens, if you call some terminal method, like send(), store(), etc.

```java
config
    // Set method sets the property in memory.
    .set("url", "http://github.com/c0stra/fluent-api-end-check")
    // Store method does really store to a file!
    .store();
```


If by accident such method is forgotten, important things may not get executed, which may have dramatical
consequences.

This module comes with simple compiler extension, driven by annotation processing, that helps avoiding this.

Once you annotate some method with an annotation @End, the annotation processor will check every
statement, if this method wasn't forgotten:

See example of a builder, which needs to be terminated by method store():

```java

public interface Builder {

    Builder set(String name, String value);

    @End
    void store();

}
```

If you annotate a method like in the example above, then you'll get compilation error when you forget to use it:

See wrong code example:
```java
builder.set("key1", "value1")
       .set("key2", "value2");
```
You'll get following compilation error:
```text
error: Method chain must end with the method: store()
```

## User guide
### 1. Mark sentence ending methods
We'll refer to any Java statement, which consists of a chain of methods using fluent API, as _"fluent API sentence"_,
_"fluent sentence"_, or simply _"sentence"_.

_"Sentence ending method"_ is then a method, which needs to end the sentence. On the example from above a sentence with
ending method `store()` can look like this:
```java
config.set("url", "http://github.com/c0stra/fluent-api-end-check/").store();
```

In order to enforce compile time check of the ending method, we need first to be able to mark it.
#### 1.1 Mark using `@End` annotation
In order to be able to mark ending method using `@End` annotation, following dependency need to be used:
```xml
<dependency>
    <groupId>foundation.fluent.api</groupId>
    <artifactId>fluent-api-end-check</artifactId>
    <version>1.22</version>
</dependency>
```
To figure out, what's the latest available version, use following search link in maven central:
https://search.maven.org/#search%7Cga%7C1%7Cfluent-api-end-check

**For JAVA 8 and `fluent-api-end-check` version >= 1.23**
Starting in version 1.23 the project was updated to use Java 9+ as default supported version. Therefore, the legacy
dependency on separate java tools was removed from project's dependencies.
If you want to use the check with java 8, then add the dependency in your project:

```xml
<dependency>
    <groupId>com.sun</groupId>
    <artifactId>tools</artifactId>
    <scope>system</scope>
    <systemPath>${java.home}/../lib/tools.jar</systemPath>
    <version>${java.version}</version>
</dependency>
```

For older version of `fluent-api-end-check` (<= 1.22) use opposite approach. For use with java 9+ exclude this dependency.

The annotation can then be used to mark the ending method:
```java
public interface Config {

    Config set(String name, String value);

    @End
    void store();
}
```

#### 1.2 Mark using provided list of ending methods
The project may use 3rd party builders / fluent API, which is not under our control, and therefore ending methods
cannot be annotated. For such case it's possible to provide simple plain text file containing list of fully
qualified methods, which are the ending methods to check.

The file with the method names, the processor searches for on class path, is:
```text
fluent-api-check-methods.txt
```
It has to list the methods including argument types (without parameter names), same as the javac would represent it.

See example:
```text
java.lang.StringBuilder.toString()
foundation.fluent.api.Config.store()
fluent.api.GenericDsl.end(T)
fluent.api.GenericDsl.<U>genericEnd(U)
```
Last 2 examples are:
- Generic class with a method accepting argument of the generic parameter of the class
- Generic method (see the generic argument in the signature) accepting argument of the method generic parameter type.

It will use all such files found on the class path.

If the processor encounters entry, which it cannot map to real method, it emits a compilation warning.
In case of class not being available, it says simply, that the class was not found. In case of no method
found in the class based on the entry in the file, it lists all available methods, including parameters, described
exactly as it expects the method to be present in the file. So you may see there if:
1. Your method is present, but there is any issue in format, how it is written (e.g. parameters not included properly)
2. Your method is not present in the class. In that case you may included method, which is inherited, and you need to
 update the entry with class / interface, where the method is declared.
#### 1.3 Mark multiple ending methods
It is possible to mark multiple methods of one interface / class as ending methods. That effectively means, that it has
to end with one of them.

In fact, functionally there is no benefit of marking more methods, only the compile time error may be hinting on
all options, how to end the sentence.
```java
public interface FluentAction {

    FluentAction parameter(String value);

    @End
    void perform();
    
    @End
    void cancel();
}
```
In such case if we call neither `perform()` nor `cancel()`, then the error will mention them both:
```text
error: Method chain must end with one of the following methods: [perform(), cancel()]
```
### 2. Configure maven project to use compile time end check
In order to use the compile time check for fluent API ending methods, you have to activate the annotation processor
in the target project, that should use (not necessarily define) the ending methods. So you may have project A defining
the fluent `Config` class, and then project B, which uses it. The project B is the one, that needs to trigger the
compile time check.

For that reason you can notice, that the `EndProcessor` is not triggered by occurrence of the `@End`
annotation, but used all the time.

#### 2.1 Using standard annotation processor resolution on class-path
The module comes with the annotation processor, and also standard Java service binding of it, so the Java compiler
will find and use the processor.

By default, the Java compiler searches for annotation processors (and such service bindings) on the class path. So
having the dependency mentioned above is sufficient, even as transitive dependency, and you have the compile time check
activated.

In terms of our projects A and B, B depends on A, and A depends on `fluent-api-end-check`, so B has it in transitive
dependencies, and therefore on class-path and the check will work.
#### 2.2 Using compiler annotation processor path
As annotation processors may have their own dependencies (it's not our case), which are solely compile time, and
shouldn't be propagated as transitive dependencies further, Java compiler allows to specify them on annotation processor
path instead of class path.

!__Such configuration will effectively disable annotation processors present on the
class-path__! (in our case as transitive dependency)

See Oracle documentation for `-processorpath` option at:
https://docs.oracle.com/javase/7/docs/technotes/tools/solaris/javac.html#options

If the project, that should be checked for ending methods, uses other processors configured using this option,
__you have to include explicitly the end check processor__ too.

It can be done e.g. using maven compiler plugin:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.7.0</version>
            <configuration>
                <annotationProcessorPaths>
                    <annotationProcessorPath>
                        <!-- Your original annotation processor -->
                    </annotationProcessorPath>
                    <annotationProcessorPath>
                        <groupId>foundation.fluent.api</groupId>
                        <artifactId>fluent-api-end-check</artifactId>
                        <version>1.22</version>
                    </annotationProcessorPath>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 3. Compile check of fluent sentence end in action.
Once the annotation processor is active, and there are ending methods configured (marked), compilation may throw
errors mentioned above.

#### 3.1 When is the check applied
It's not desired always to perform the check. So let's have a look at the situations, when it
would apply.

| Situation             | Example                      | Applies or not           |
| --------------------- | ---------------------------- | ------------------------ |
| Expression statement  | config.set("", "");          | __YES__                  |
| Assignment            | config = config.set("", ""); | __NO__ - may end later   |
| Passed as argument    | method(config.set("", ""));  | __NO__ - may end inside  |
| Return statement      | return config.set("", "");   | __NO__ - may end outside |

#### 3.2 Custom compilation error
Since version `1.11` it is possible to customize the compilation error message using parameter
`message` of the `@End` annotation:

```java
public interface FluentApi {
    @End("Method end() must be called.")
    void end();

    FluentApi fiend();
}
```

If during fluent sentence analysis multiple methods with custom message are detected,
then only the last message is used.


#### 3.3 How to bypass the check using `@IgnoreMissingEndMethod`
Although the check itself tries to recognize situations, when it shouldn't apply the check, there might
be situations, when it would apply it, but it's still not desired. For such cases an annotation
`@IgnoreMissingEndMethod` can be used on a method, to bypass it's statements for such check.

Typical example would be unit tests:
```java
public class TestFluentApi {

    @Test
    @IgnoreMissingEndMethod
    public void test() {
        // Mock something
        new Config().set("url", "http://github.com/c0stra/fluent-api-end-check/");
        // Perform verifications of the set() method
    }

}
```
Without ignoring the end method check, this test method would throw compilation error.

### 4. Fluent sentence end check for structured DSL - `@Start`
Examples above covered rather simple DSL patterns like simple builder, or hierarchical builder with a need or option
to end the sentence at any level, or require to get back to initial level.

For those examples the trigger of the check in the sentence was point, where the expression (method return value)
evaluated to type, which contains some end method and therefore only marking the `@End` method is sufficient.

There are other DSL patterns, e.g. "named parameters" or other sequential invocation, where the sentence gets to states,
where we need to make sure, that sentence must continue, but the terminal (`@End`) method is not yet reachable.

Let's see an example of "named parameters":

```java
public interface Parameter1 {
    Parameter2 parameter1(String value);
}
public interface Parameter2 {
    Parameter3 parameter2(int value);
}
public interface Parameter3 {
    @End
    void parameter3(LocalDate value);
}

@Start("Parameter1, parameter2 and parameter3 need to be provided.")
public static Parameter1 callWith() {
    return parameter1 -> parameter2 -> parameter3 -> {};
}

public void test() {
    callWith().parameter1("string").parameter2(5).parameter3(now());
}
```

In the example above, we can see, that only the very final method `parameter3(LocalDate)` is the terminal `@End` method,
which can terminate the chain. But this one is not available on any other interface, than `Parameter3`. So the check
wouldn't be triggered until `Parameter3` interface is hit, and chain terminated with `callWith()` or `parameter1(String)`
would pass the compilation, which is miss of the terminal method.

For that in version 1.15, additional annotation `@Start` got introduced. That allows to enforce search for the terminal
method although it's not yet reachable.

You can see an example of it's usage in the code above. It needs to specify an error message, that would be reported by
the compiler if the check fails.

### 5. Detection of misconfiguration of the end check
In large projects, it may become business critical to avoid missed end methods. But there are situations, that the check
may get disabled by accidental misconfiguration.

Simple example can be, that initially the check is achieved by standard class-path annotation processor resolution
(see paragraph 2.1), and by introducing another annotation processor using compiler annotation processor path, the class-path
ones get disabled.

Such situation can lead to potentially missed ending methods, and that in turn in not triggering the actions, which may
have significant impact.

In order to prevent that, it is possible since version 1.10 to detect such situation e.g. using unit test. It is based
on new feature of the annotation processor, to generate files, when it finds `@EndMethodCheckFile` annotation.

Such test need to do 2 steps:
1. Request to generate the end method check file with unique file name within the current class-path
2. Check for the requested file on class-path.

The library now supports these two actions, so such test can look like this:

```java
@Test
@EndMethodCheckFile(uniqueFileName = "my-module-name.file")
public void failIfRequiredCheckNotInvoked() {
    EndProcessor.assertThatEndMethodCheckFileExists("my-module-name.file");
}
```

Such test will fail if
* either no such file was found on class-path with the error message, that either creation wasn't requested, or the processor isn't enabled
* or if more than one file was found on class-path, so the filename is not unique.

## Release notes

#### Version 1.23 (March 10th 2021)
- Switched by default to JAVA 9 approach (use bundled tools with Javac tree API instead of using system dependency)

[Test evidence for 1.23](reports/TEST-REPORT-1.23.md)

#### Version 1.22 (March 9th 2021)
- Adopted to IntelliJ IDEA proxy of javac ProcessingEnvironment (thanks to @ava1ar)

[Test evidence for 1.22](reports/TEST-REPORT-1.22.md)

#### Version 1.19 (August 11th 2019)
- Fixed [#14: Check fails on lambda with BLOCK body](https://github.com/c0stra/fluent-api-end-check/issues/14)

[Test evidence for 1.19](reports/TEST-REPORT-1.19.md)

#### Version 1.18 (August 5th 2019)
- `@End` method check is not silently disabled any more in case of it's implementation problems. It fails the compilation
  providing more details and instructions for reporting a bug instead, forcing the client to disable the check
  explicitly.

[Test evidence for 1.18](reports/TEST-REPORT-1.18.md)

#### Version 1.17 (August 5th 2019)
- Fixed [#13: Check fails if expression contains field selector](https://github.com/c0stra/fluent-api-end-check/issues/13)

[Test evidence for 1.17](reports/TEST-REPORT-1.17.md)

#### Version 1.16 (February 1st 2019)
- Fixed infinite loop in analysis causing the check to hang compilation.

[Test evidence for 1.16](reports/TEST-REPORT-1.16.md)

#### Version 1.15 (December 11th 2018)
- Introduced `@Start` annotation used to mark beginning of a fluent sentence, that needs to finish with
  `@End` for use cases, where the sentence may be composed by chaining of different interfaces, some of them not containing the ending method.

[Test evidence for 1.15](reports/TEST-REPORT-1.15.md)

#### Version 1.14 (November 27th 2018)
- Fixed issue with missed externally defined end method in method reference.

[Test evidence for 1.14](reports/TEST-REPORT-1.14.md)

#### Version 1.13 (November 26th 2018)
- Simplified implementation.
- No separate scanner for end method collecting. It's now responsibility of `EndScanner`.
- Visitors testing void lambda, and member reference merged and parametrized.
- `EndScanner` now implements TaskListener, so no extra class needed.

[Test evidence for 1.13](reports/TEST-REPORT-1.13.md)

#### Version 1.12 (November 9th 2018)
- Fixed issue introduced on static method analysis

[Test evidence for 1.12](reports/TEST-REPORT-1.12.md)

#### Version 1.11 (November 8th 2018)
- Fixed compilation failure when qualified static method called on class, which contains `@End` annotated method.
- Added optional parameter of the annotation to specify custom error message.

[Test evidence for 1.11](reports/TEST-REPORT-1.11.md)

#### Version 1.10 (September 21st 2018)
- Delivered [#9: Implement support for detection of missing end method check.](https://github.com/c0stra/fluent-api-end-check/issues/9)

[Test evidence for 1.10](reports/TEST-REPORT-1.10.md)

#### Version 1.9 (September 18th 2018)
- Ignore end methods on `this`. That indicates, usage inside implementation of the fluent API, not by clients.

[Test evidence for 1.9](reports/TEST-REPORT-1.9.md)

#### Version 1.8 (September 11th 2018)
- Fixed [#8: Check is not catching properly missing end method in lambda expression or method reference.](https://github.com/c0stra/fluent-api-end-check/issues/8)
- Removed unused dependencies.

[Test evidence for 1.8](reports/TEST-REPORT-1.8.md)

#### Version 1.7 (June 21st 2018)
- Delivered [#7: Include generation and commit of test evidence in the release process.](https://github.com/c0stra/fluent-api-end-check/issues/7)
- Test evidence is now automatically generated during release process.

[Test evidence for 1.7](reports/TEST-REPORT-1.7.md)

#### Version 1.6 (June 21st 2018)
- Improved readability of test evidence

[Test evidence for 1.6](reports/TEST-REPORT-1.6.md)

#### Version 1.5 (June 12th 2018)
- Some tweaks to the release process in the pom.xml

[Test evidence for 1.5](reports/TEST-REPORT-1.5.md)

#### Version 1.4 (June 12th 2018)
- Fixed [#5: BUG: Check doesn't recognize missing ending method after constructor](https://github.com/c0stra/fluent-api-end-check/issues/5)

[Test evidence for 1.4](reports/TEST-REPORT-1.4.md)

#### Version 1.3 (June 10th 2018)
- Fixed [#3: Issue with "immediate" ending method requirement](https://github.com/c0stra/fluent-api-end-check/issues/3)
- [#4: Added test cases to verify proper behavior for generic classes and generic ending methods](https://github.com/c0stra/fluent-api-end-check/issues/4)
- Improved implementation, so it doesn't drill down the sentence if a chain ends with the ending method.

[Test evidence for 1.3](reports/TEST-REPORT-1.3.md)

#### Version 1.2 (June 9th 2018)
- [#1: Added support to load external definition of ending methods](https://github.com/c0stra/fluent-api-end-check/issues/1)

[Test evidence for 1.2](reports/TEST-REPORT-1.2.md)

#### Version 1.1 (June 8th 2018)
- Improved analysis of the fluent sentence.
- It can detect ending method for interfaces / classes even if the sentence uses nesting.
- [#2: It can detect ending method also in case of the "pass through" ending method (method allowing chaining because it can](https://github.com/c0stra/fluent-api-end-check/issues/2)
 be used multiple times within the chain).
 
[Test evidence for 1.1](reports/TEST-REPORT-1.1.md)

 #### Version 1.0 (June 5th 2018)
 - Initial naive implementation using simple check of the expression statement return type.
 
[Test evidence for 1.0](reports/TEST-REPORT-1.0.md)
