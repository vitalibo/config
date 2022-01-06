# Config

Configuration library for JVM languages with support YAML syntax.
This library extends functionality of [config](https://github.com/lightbend/config) developed by Lightbend (ex Typesafe).

### Usage

You can find published releases on my public maven repository.

```xml
<dependency>
  <groupId>com.github.vitalibo</groupId>
  <artifactId>config_1.4.1</artifactId>
  <version>1.0.0</version>
</dependency>

<repositories>
  <repository>
    <id>com.github.vitalibo.mvn</id>
    <url>https://raw.github.com/vitalibo/public-maven-repository/release/</url>
    <snapshots>
      <enabled>true</enabled>
      <updatePolicy>always</updatePolicy>
    </snapshots>
  </repository>
</repositories>
```

### API example

Yaml syntax currently supports only `load(String)`, `parseResourcesAnySyntax(String)` and `parseResources(String)` methods.

```java
import com.github.vitalibo.config.ConfigFactory;

Config conf = ConfigFactory.load("application.yaml");
int bar1 = conf.getInt("foo.bar");
Config foo = conf.getConfig("foo");
int bar2 = foo.getInt("bar");
```
