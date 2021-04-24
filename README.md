# Logstash Java Plugin

[![Travis Build Status](https://travis-ci.com/logstash-plugins/logstash-filter-java_filter_example.svg)](https://travis-ci.com/logstash-plugins/logstash-filter-java_filter_example)

This is a Java plugin for [Logstash](https://github.com/elastic/logstash).

It is fully free and fully open source. The license is Apache 2.0, meaning you are free to use it however you want.

add `gradle.properties` to your project root path and configurate the logstash-core position:
```
LOGSTASH_CORE_PATH=E:/coding/IdeaProjects/git/logstash/logstash-core
```

modify the build.gradle file,change to your right config:
```
dependencies {
    ...
    implementation fileTree(dir: LOGSTASH_CORE_PATH, include: "build/libs/logstash-core-[to be change].jar")
    ...
}

...
```

after build success,execute in project root command terminal:
```
>  gradlew.bat gem
```
and then you will see `logstash-filter-ip2region-1.0.2.gem` file was generated under project root path.

Just install it and set the logstash config:
```
filter{
 ip2region {
   source => "ip"
   database => "path/to/ip2region.db"
 }

}
```
