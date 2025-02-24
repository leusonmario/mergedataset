## 1. Clone the project:  
   `git clone https://github.com/elastic/elasticsearch`

## 2. Checkout to merge commit hash:  
   `git checkout 59cb67c7bd0ab6311115b20954e013412b676b29`

## 3. Replace the **pom.xml** at the **core** folder of the project:

```maven
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <version>2.1.0</version>
    
    <groupId>org.elasticsearch</groupId>
    <artifactId>elasticsearch</artifactId>

    <name>Elasticsearch: Core</name>
    <description>Elasticsearch - Open Source, Distributed, RESTful Search Engine</description>

    <properties>
        <maven.compiler.source>1.7</maven.compiler.source>
        <maven.compiler.target>1.7</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.hamcrest</groupId>
            <artifactId>hamcrest-all</artifactId>
            <scope>test</scope>
            <version>1.3</version>
        </dependency>
        <dependency>
            <groupId>com.carrotsearch.randomizedtesting</groupId>
            <artifactId>randomizedtesting-runner</artifactId>
            <scope>test</scope>
            <version>2.1.16</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-test-framework</artifactId>
            <version>5.2.1</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
            <scope>test</scope>
            <version>4.3.6</version>
        </dependency>
        <dependency>
            <groupId>com.google.jimfs</groupId>
            <artifactId>jimfs</artifactId>
            <scope>test</scope>
            <version>1.0</version>
        </dependency>

        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-core</artifactId>
            <version>5.2.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-backward-codecs</artifactId>
            <version>5.2.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-analyzers-common</artifactId>
            <version>5.2.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-queries</artifactId>
            <version>5.2.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-memory</artifactId>
            <version>5.2.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-highlighter</artifactId>
            <version>5.2.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-queryparser</artifactId>
            <version>5.2.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-suggest</artifactId>
            <version>5.2.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-join</artifactId>
            <version>5.2.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-spatial</artifactId>
            <version>5.2.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-expressions</artifactId>
            <version>5.2.1</version>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.spatial4j</groupId>
            <artifactId>spatial4j</artifactId>
            <optional>true</optional>
            <version>0.4.1</version>
        </dependency>
        <dependency>
            <groupId>com.vividsolutions</groupId>
            <artifactId>jts</artifactId>
            <optional>true</optional>
            <version>1.13</version>
        </dependency>
        <!-- needed for templating -->
        <dependency>
            <groupId>com.github.spullara.mustache.java</groupId>
            <artifactId>compiler</artifactId>
            <optional>true</optional>
            <version>0.8.13</version>
        </dependency>
        <!-- Lucene spatial -->


        <!-- START: dependencies that might be shaded -->
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>18.0</version>
        </dependency>
        <dependency>
            <groupId>com.carrotsearch</groupId>
            <artifactId>hppc</artifactId>
            <version>0.7.1</version>
        </dependency>
        <dependency>
            <groupId>joda-time</groupId>
            <artifactId>joda-time</artifactId>
            <version>2.8.2</version>
        </dependency>
        <dependency>
            <groupId>org.joda</groupId>
            <artifactId>joda-convert</artifactId>
            <version>1.2</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-core</artifactId>
            <version>2.5.3</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.dataformat</groupId>
            <artifactId>jackson-dataformat-smile</artifactId>
            <version>2.5.3</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.dataformat</groupId>
            <artifactId>jackson-dataformat-yaml</artifactId>
            <version>2.5.3</version>
            <exclusions>
                <exclusion>
                    <groupId>com.fasterxml.jackson.core</groupId>
                    <artifactId>jackson-databind</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.dataformat</groupId>
            <artifactId>jackson-dataformat-cbor</artifactId>
            <version>2.5.3</version>
        </dependency>
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty</artifactId>
            <version>3.10.3.Final</version>
        </dependency>
        <dependency>
            <groupId>com.ning</groupId>
            <artifactId>compress-lzf</artifactId>
            <version>1.0.2</version>
        </dependency>
        <dependency>
            <groupId>com.tdunning</groupId>
            <artifactId>t-digest</artifactId>
            <version>3.0</version>
        </dependency>
        <dependency>
            <groupId>org.hdrhistogram</groupId>
            <artifactId>HdrHistogram</artifactId>
            <version>2.1.6</version>
        </dependency>
        <dependency>
            <groupId>commons-cli</groupId>
            <artifactId>commons-cli</artifactId>
            <version>1.3.1</version>
        </dependency>
        <!-- END: dependencies that might be shaded -->

        <dependency>
            <groupId>org.codehaus.groovy</groupId>
            <artifactId>groovy-all</artifactId>
            <classifier>indy</classifier>
            <optional>true</optional>
            <version>2.4.4</version>
        </dependency>
        <dependency>
            <groupId>log4j</groupId>
            <artifactId>log4j</artifactId>
            <optional>true</optional>
            <version>1.2.17</version>
        </dependency>
        <dependency>
            <groupId>log4j</groupId>
            <artifactId>apache-log4j-extras</artifactId>
            <optional>true</optional>
            <version>1.2.17</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <optional>true</optional>
            <version>1.6.2</version>
        </dependency>
        <dependency>
            <groupId>net.java.dev.jna</groupId>
            <artifactId>jna</artifactId>
            <optional>true</optional>
            <version>5.5.0</version>
        </dependency>

        <!-- remove this for java 8 -->
        <dependency>
            <groupId>com.twitter</groupId>
            <artifactId>jsr166e</artifactId>
            <version>1.1.0</version>
        </dependency>

    </dependencies>

    <build>

        <resources>
            <resource>
                <directory>${project.basedir}/src/main/java</directory>
                <includes>
                    <include>**/*.json</include>
                    <include>**/*.yml</include>
                </includes>
            </resource>
            <resource>
                <directory>${project.basedir}/src/main/resources</directory>
                <includes>
                    <include>**/*.*</include>
                </includes>
                <filtering>true</filtering>
            </resource>
        </resources>

        <testResources>
            <testResource>
                <directory>${project.basedir}/src/test/java</directory>
                <includes>
                    <include>**/*.json</include>
                    <include>**/*.yml</include>
                    <include>**/*.txt</include>
                    <include>**/*.properties</include>
                </includes>
                <filtering>true</filtering>
            </testResource>
            <testResource>
                <directory>${project.basedir}/src/test/java</directory>
                <includes>
                    <include>**/*.gz</include>
                </includes>
            </testResource>
            <testResource>
                <directory>${project.basedir}/src/test/resources</directory>
                <includes>
                    <include>**/*.*</include>
                </includes>
            </testResource>
            <testResource>
                <directory>${elasticsearch.tools.directory}/rest-api-spec</directory>
                <targetPath>rest-api-spec</targetPath>
                <includes>
                    <include>api/*.json</include>
                    <include>test/**/*.yaml</include>
                </includes>
            </testResource>
             <!-- shared test resources like log4j.properties -->
            <testResource>
                <directory>${elasticsearch.tools.directory}/shared-test-resources</directory>
                <filtering>false</filtering>
            </testResource>
        </testResources>

        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <executions>
                    <execution>
                        <id>attach-test-sources</id>
                        <goals>
                            <goal>test-jar</goal>
                        </goals>
                        <configuration>
                            <includes>
                                <include>org/elasticsearch/test/**/*</include>
                                <include>org/elasticsearch/bootstrap/BootstrapForTesting.class</include>
                                <include>org/elasticsearch/common/cli/CliToolTestCase.class</include>
                                <include>org/elasticsearch/common/cli/CliToolTestCase$*.class</include>
                            </includes>
                            <excludes>
                                <!-- unit tests for yaml suite parser & rest spec parser need to be excluded -->
                                <exclude>org/elasticsearch/test/rest/test/**/*</exclude>
                                <!-- unit tests for test framework classes-->
                                <exclude>org/elasticsearch/test/test/**/*</exclude>
                            </excludes>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <executions>
                    <execution>
                        <phase>prepare-package</phase>
                        <goals>
                            <goal>test-jar</goal>
                        </goals>
                        <configuration>
                            <includes>
                                <include>rest-api-spec/**/*</include>
                                <include>org/elasticsearch/test/**/*</include>
                                <include>org/elasticsearch/bootstrap/BootstrapForTesting.class</include>
                                <include>org/elasticsearch/common/cli/CliToolTestCase.class</include>
                                <include>org/elasticsearch/common/cli/CliToolTestCase$*.class</include>
                                <include>org/elasticsearch/cluster/MockInternalClusterInfoService.class</include>
                                <include>org/elasticsearch/cluster/MockInternalClusterInfoService$*.class</include>
                                <include>org/elasticsearch/index/shard/MockEngineFactoryPlugin.class</include>
                                <include>org/elasticsearch/search/MockSearchService.class</include>
                                <include>org/elasticsearch/search/MockSearchService$*.class</include>
                                <include>org/elasticsearch/cache/recycler/MockPageCacheRecycler.class</include>
                                <include>org/elasticsearch/cache/recycler/MockPageCacheRecycler$*.class</include>
                                <include>org/elasticsearch/common/util/MockBigArrays.class</include>
                                <include>org/elasticsearch/common/util/MockBigArrays$*.class</include>
                                <include>org/elasticsearch/node/NodeMocksPlugin.class</include>
                            </includes>
                            <excludes>
                                <!-- unit tests for yaml suite parser & rest spec parser need to be excluded -->
                                <exclude>org/elasticsearch/test/rest/test/**/*</exclude>
                                <!-- unit tests for test framework classes-->
                                <exclude>org/elasticsearch/test/test/**/*</exclude>
                            </excludes>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-antrun-plugin</artifactId>
                <executions>
                    <execution>
                        <!-- Don't run the license checker in core -->
                        <id>check-license</id>
                        <phase>none</phase>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <artifactId>maven-assembly-plugin</artifactId> 
                <configuration> 
                <archive> 
                <manifest> 
                    <mainClass>fully.qualified.MainClass</mainClass> 
                </manifest> 
                </archive> 
                <descriptorRefs> 
                    <descriptorRef>jar-with-dependencies</descriptorRef> 
                </descriptorRefs> 
                </configuration> 
                </plugin> 
        </plugins>
      <pluginManagement>
        <plugins>
            <plugin>
               <groupId>org.jacoco</groupId>
               <artifactId>jacoco-maven-plugin</artifactId>
               <configuration>
                 <excludes>
                   <exclude>org/apache/lucene/**</exclude>
                 </excludes>
               </configuration>
            </plugin>
            <plugin>
                <groupId>com.mycila</groupId>
                <artifactId>license-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <!-- Guice -->
                        <exclude>src/main/java/org/elasticsearch/common/inject/**</exclude>
                        <exclude>src/main/java/org/elasticsearch/common/geo/GeoHashUtils.java</exclude>
                        <exclude>src/main/java/org/apache/lucene/**/X*.java</exclude>
                        <!-- t-digest -->
                        <exclude>src/main/java/org/elasticsearch/search/aggregations/metrics/percentiles/tdigest/TDigestState.java</exclude>
                        <!-- netty pipelining -->
                        <exclude>src/main/java/org/elasticsearch/http/netty/pipelining/**</exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
      </pluginManagement>
    </build>
    <profiles>
        <!-- license profile, to generate third party license file -->
        <profile>
            <id>license</id>
            <activation>
                <property>
                    <name>license.generation</name>
                    <value>true</value>
                </property>
            </activation>
            <!-- not including license-maven-plugin is sufficent to expose default license -->
        </profile>
    </profiles>
</project>
```

## 4. After applying the testability transformations, you must perform some manual steps:
    - Update the modifier public for private: 
    ```
          private Type(  MatchQuery.Type matchQueryType,  float tieBreaker,  ParseField parseField){
            this.matchQueryType=matchQueryType;
            this.tieBreaker=tieBreaker;
            this.parseField=parseField;
          }
    ```

## 5. Inside the folder **core** run the command:
   `mvn clean compile assembly:single`

## 6. check the contents folder **elasticsearch/core/target**.

## 7. Identify the left and right commit hash. (**git log --pretty=%P -n 1 <merge_commit_hash>**)  
   Run: `git log --pretty=%P -n 1 59cb67c7bd0ab6311115b20954e013412b676b29`.  
   Receive the output: `8757af2d928355a799290207d9128adae4c78fa1 8ff1efbcf05fa54262b3f6d0ab12f30516d1b52a`

## 8. Checkout to left commit hash and repeat steps 3-6:  
   `git checkout 8757af2d928355a799290207d9128adae4c78fa1`

## 9. Checkout to right commit hash and repeat steps 3-6:  
   `git checkout 8ff1efbcf05fa54262b3f6d0ab12f30516d1b52a`

## 10. Identify the base commit hash. (**git merge-base <left_commit_hash> <right_commit_hash>**)  
   Run: `git merge-base 8757af2d928355a799290207d9128adae4c78fa1 8ff1efbcf05fa54262b3f6d0ab12f30516d1b52a`.  
   Receive the output: `2336da1704627c236e6eca214614e748d195794d`

## 11. Checkout to base commit hash and repeat steps 3-6:  
    `git checkout 2336da1704627c236e6eca214614e748d195794d`
