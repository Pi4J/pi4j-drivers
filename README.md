
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
      <version>${pi4j.drivers.version}</version>
   </dependency>
   ...
</dependencies>

<!-- Add the following if you want to use a SNAPSHOT version -->
<repositories>
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

## CONTRIBUTING

Please refer to [CONTRIBUTING.md](CONTRIBUTING.md) and [CONTRIBUTING_REPO.md](CONTRIBUTING_REPO.md).

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

## LICENSE

 Pi4J is licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
