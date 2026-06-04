
 Pi4J :: Drivers
==========================================================================

GitHub Actions: 
![Maven build](https://github.com/pi4j/pi4j-drivers/workflows/Build/badge.svg)

**THIS IS THE VERY START OF A NEW PROJECT. NO USABLE CODE IS YET AVAILABLE.**

This project contains driver implementations for various electronic components, using Pi4J V4+. Full description will be available on the Pi4J website at [Documentation > Using the Drivers Library](https://pi4j.com/documentation/drivers).

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
   <!-- Pi4J Drivers snapshots -->
   <repository>
      <id>sonatype-snapshots</id>
      <name>Maven Central Snapshots</name>
      <url>https://central.sonatype.com/repository/maven-snapshots</url>
      <releases>
         <enabled>false</enabled>
      </releases>
      <snapshots>
         <enabled>true</enabled>
      </snapshots>
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

This project can be built with Maven:

```bash
./mvnw verify
```

## Release Flow

Releases are automated via [JReleaser](https://jreleaser.org) and GitHub Actions, but the workflow can also be started manually from the GitHub Actions tab.  
The workflow is defined in [`.github/workflows/release.yml`](.github/workflows/release.yml).

### How it works

The release pipeline triggers automatically whenever `pom.xml` is pushed to `main` **and** the version does **not** end in `-SNAPSHOT`.  
You can also run it manually if you already pushed a version change earlier and want to finish the release later.  
No manual tag creation is needed — JReleaser creates the Git tag, GitHub Release, and publishes to Maven Central.

```
push pom.xml to main (non-SNAPSHOT version)
        │
        ▼
 check-version job
  └─ version is SNAPSHOT? → skip, do nothing
  └─ version is release?  → continue
        │
        ▼
 release job
  ├─ Build JAR + sources JAR + javadoc JAR (-Prelease)
  ├─ Stage artifacts to target/staging-deploy
  ├─ Validate JReleaser config (jreleaser:config)
  ├─ Open GitHub issue for manual approval (FDelporte / stefanhaustein)
  │     └─ Approver comments "approved" on the issue
  └─ JReleaser full-release
        ├─ GPG-sign all artifacts
        ├─ Publish bundle to Maven Central Portal
        └─ Create GitHub Release with auto-generated changelog
```

### Step-by-step for maintainers

**1. Remove the `-SNAPSHOT` suffix and push:**

```bash
# Set the release version (e.g. 1.0.0)
./mvnw versions:set -DnewVersion=1.0.0 -DgenerateBackupPoms=false

git add pom.xml
git commit -m "chore: release 1.0.0"
git push origin main
```

The workflow starts automatically. A GitHub issue is opened asking for approval.

**2. Approve the release:**

One of the listed approvers (`FDelporte`, `stefanhaustein`) comments **`approved`** on the auto-created issue.  
The pipeline resumes and publishes to Maven Central.

**3. Bump to the next development snapshot:**

```bash
./mvnw versions:set -DnewVersion=1.0.1-SNAPSHOT -DgenerateBackupPoms=false

git add pom.xml
git commit -m "chore: prepare next development iteration 1.0.1-SNAPSHOT"
git push origin main
# workflow triggers but immediately skips (SNAPSHOT detected)
```

### Required GitHub secrets

| Secret | Description |
|--------|-------------|
| `JRELEASER_MAVENCENTRAL_USERNAME` | User Token username from [central.sonatype.com/account](https://central.sonatype.com/account) |
| `JRELEASER_MAVENCENTRAL_TOKEN` | User Token password from [central.sonatype.com/account](https://central.sonatype.com/account) |
| `JRELEASER_GPG_PASSPHRASE` | Passphrase for the GPG signing key |
| `JRELEASER_GPG_SECRET_KEY` | ASCII-armored GPG secret key |
| `JRELEASER_GPG_PUBLIC_KEY` | ASCII-armored GPG public key |
| `GITHUB_TOKEN` | Automatically provided by GitHub Actions |

> **Note:** The old `JRELEASER_NEXUS2_*` secrets (legacy Sonatype OSSRH) are no longer used.  
> Generate fresh User Tokens at [central.sonatype.com/account](https://central.sonatype.com/account).

## LICENSE

 Pi4J is licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
