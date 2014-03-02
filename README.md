Spring HTTP Client Cache [![Build Status](https://travis-ci.org/jirutka/spring-http-client-cache.png)](https://travis-ci.org/jirutka/spring-http-client-cache) [![Coverage Status](https://coveralls.io/repos/jirutka/spring-http-client-cache/badge.png)](https://coveralls.io/r/jirutka/spring-http-client-cache)
========================

The aim of this project is to provide a _lightweight_ client HTTP cache for the [Spring Framework](http://projects.spring.io/spring-framework/), specifically [RestTemplate](http://docs.spring.io/spring/docs/3.2.x/javadoc-api/org/springframework/web/client/RestTemplate.html). It’s implemented purely on top of Spring’s interfaces and doesn’t use any “heavy” caching library.

This code is inspired by the [Apache HTTP Components](https://hc.apache.org/).


TODO
----

*  Add support for conditional requests.
*  Add support for response revalidation.
*  ...


Maven
-----

Released versions are available in The Central Repository. Just add this artifact to your project:

```xml
<dependency>
    <groupId>cz.jirutka.spring</groupId>
    <artifactId>spring-http-client-cache</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

However if you want to use the last snapshot version, you have to add the Sonatype OSS repository:

```xml
<repository>
    <id>sonatype-snapshots</id>
    <name>Sonatype repository for deploying snapshots</name>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```


License
-------

This project is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
