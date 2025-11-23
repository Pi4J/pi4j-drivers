
 Pi4J :: Drivers
==========================================================================

GitHub Actions: 
![Maven build](https://github.com/pi4j/pi4j-drivers/workflows/Build/badge.svg)

**THIS IS THE VERY START OF A NEW PROJECT. NO USABLE CODE IS YET AVAILABLE.**

This project contains driver implementations for various electronic components, using Pi4J V4+. Full description will be available on the Pi4J website at [Documentation > Using Drivers](https://pi4j.com/documentation/using-drivers).

For the current coverage state and plans, please refer to https://docs.google.com/document/d/1Gyeaq6xCbvmE-ZH675zyYz2Jhhc-76IA5tvJtxvjmbE/edit?usp=sharing

## Using this Library

As this is library is still in early stage, you can only get a SNAPSHOT-version. To be able to use it in your project, you'll need to add both the dependency, and allow SNAPSHOTs:

```xml
<dependencies>
   ...
   <dependency>
      <groupId>com.pi4j</groupId>
      <artifactId>pi4j-drivers</artifactId>
      <version>0.0.1-SNAPSHOT</version>
   </dependency>
   ...
</dependencies>

<repositories>
   <!-- Non-snapshot Maven libraries + Pi4J Core snapshots -->
   <repository>
      <id>oss-snapshots-repo</id>
      <name>Sonatype OSS Maven Repository</name>
      <url>https://oss.sonatype.org/content/groups/public</url>
      <releases>
         <enabled>false</enabled>
      </releases>
      <snapshots>
         <enabled>true</enabled>
      </snapshots>
   </repository>
   
   <!-- Pi4J Drivers snapshots -->
   <repository>
      <id>oss-snapshots-M2-repo</id>
      <name>Sonatype OSS Maven2 Repository</name>
      <url>https://central.sonatype.com/repository/maven-snapshots</url>
   </repository>
</repositories>
```

## The Case for A Pi4j “driver” Subproject

Document created by **Stefan Haustein**.

### Background

Pi4J used to have a set of iot hardware drivers, but they have been removed from the main project due to maintenance issues. Currently, there is the [pi4j-example-devices](https://github.com/Pi4J/pi4j-example-devices/) subproject containing drivers.

The examples cover several common devices and show how to use Pi4J to communicate with devices, but they are not suitable for external projects to “just” depend on.

### Proposal

The proposal here is to insert a new subproject `pi4j-drivers`, so the dependency structure for `pi4j-example-devices` is as follows:

```text
pi4j
|
pi4j-drivers
|
pi4j-example-devices
```

Ideally, this would not lead to an expansion of code size. Instead, drivers that are converted by the `pi4j-drivers` subproject will be deleted in the example-devices project – keeping the examples in place.

Examples for the `pi4j-drivers` project will be contained in the `pi4j-example-devices` project.

The drivers project will allow users to use covered hardware in a straightforward way, taking advantage of modern build / dependency management to keep them up to date.

### Volunteers

* @eitch
  * Proper release management
* @stefanhaustein
  * Contribute a set of drivers (currently have [Bmx280Driver.java](https://github.com/stefanhaustein/tablecraft/blob/main/src/main/java/org/kobjects/pi4jdriver/sensor/bmx280/Bmx280Driver.java) and [Scd4xDriver.java](https://github.com/stefanhaustein/tablecraft/blob/main/src/main/java/org/kobjects/pi4jdriver/sensor/scd4x/Scd4xDriver.java) and can offer a PiXtendDriver if there is interest, but it’s a bit more “esoteric”; would also like to add a character lcd driver after some more cleanup). 
  * Would be willing to take part in ownership, i.e guide contributors / do code review
  * Would be willing to write PRs for removing redundant code from pi4j-example-devices
* @fdelporte
  * Documentation on the Pi4J website
* Many others volunteered to contribute their existing driver implementations
  * See this [Pi4J Discussion](https://github.com/Pi4J/pi4j/discussions/378)

## CONTRIBUTING

Please refer to [CONTRIBUTING.md](CONTRIBUTING.md).

## BUILD DEPENDENCIES & INSTRUCTIONS

This project can be built with Maven.

## LICENSE

 Pi4J is licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
