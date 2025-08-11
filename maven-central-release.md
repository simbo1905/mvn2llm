# Maven Central Release Setup for mvn2llm

## Prerequisites

1. **OSSRH Account**
   - Sign up at https://issues.sonatype.org
   - Create a New Project ticket for `io.github.simbo1905` groupId
   - Wait for approval (usually 1-2 business days)

2. **GPG Key Setup**
   ```bash
   # Generate GPG key
   gpg --gen-key
   
   # List keys
   gpg --list-secret-keys --keyid-format LONG
   
   # Export public key
   gpg --armor --export YOUR_KEY_ID
   
   # Upload to key server
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   ```

3. **Maven Settings** (~/.m2/settings.xml)
   ```xml
   <settings>
     <servers>
       <server>
         <id>ossrh</id>
         <username>your-sonatype-username</username>
         <password>your-sonatype-password</password>
       </server>
     </servers>
     <profiles>
       <profile>
         <id>ossrh</id>
         <activation>
           <activeByDefault>true</activeByDefault>
         </activation>
         <properties>
           <gpg.keyname>YOUR_KEY_ID</gpg.keyname>
           <gpg.passphrase>your-gpg-passphrase</gpg.passphrase>
         </properties>
       </profile>
     </profiles>
   </settings>
   ```

## POM.xml Updates Required

1. **Add project metadata**
   ```xml
   <name>mvn2llm</name>
   <url>https://github.com/simbo1905/mvn2llm</url>
   
   <licenses>
     <license>
       <name>MIT License</name>
       <url>https://opensource.org/licenses/MIT</url>
     </license>
   </licenses>
   
   <developers>
     <developer>
       <id>simbo1905</id>
       <name>Simon Massey</name>
       <email>your-email@example.com</email>
     </developer>
   </developers>
   
   <scm>
     <connection>scm:git:git://github.com/simbo1905/mvn2llm.git</connection>
     <developerConnection>scm:git:ssh://github.com:simbo1905/mvn2llm.git</developerConnection>
     <url>https://github.com/simbo1905/mvn2llm/tree/main</url>
   </scm>
   ```

2. **Add distribution management**
   ```xml
   <distributionManagement>
     <snapshotRepository>
       <id>ossrh</id>
       <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
     </snapshotRepository>
     <repository>
       <id>ossrh</id>
       <url>https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/</url>
     </repository>
   </distributionManagement>
   ```

3. **Add release plugins**
   ```xml
   <build>
     <plugins>
       <!-- Existing plugins... -->
       
       <plugin>
         <groupId>org.apache.maven.plugins</groupId>
         <artifactId>maven-source-plugin</artifactId>
         <version>3.3.0</version>
         <executions>
           <execution>
             <id>attach-sources</id>
             <goals>
               <goal>jar-no-fork</goal>
             </goals>
           </execution>
         </executions>
       </plugin>
       
       <plugin>
         <groupId>org.apache.maven.plugins</groupId>
         <artifactId>maven-javadoc-plugin</artifactId>
         <version>3.6.3</version>
         <executions>
           <execution>
             <id>attach-javadocs</id>
             <goals>
               <goal>jar</goal>
             </goals>
           </execution>
         </executions>
       </plugin>
       
       <plugin>
         <groupId>org.apache.maven.plugins</groupId>
         <artifactId>maven-gpg-plugin</artifactId>
         <version>3.1.0</version>
         <executions>
           <execution>
             <id>sign-artifacts</id>
             <phase>verify</phase>
             <goals>
               <goal>sign</goal>
             </goals>
           </execution>
         </executions>
       </plugin>
       
       <plugin>
         <groupId>org.sonatype.plugins</groupId>
         <artifactId>nexus-staging-maven-plugin</artifactId>
         <version>1.6.13</version>
         <extensions>true</extensions>
         <configuration>
           <serverId>ossrh</serverId>
           <nexusUrl>https://s01.oss.sonatype.org/</nexusUrl>
           <autoReleaseAfterClose>true</autoReleaseAfterClose>
         </configuration>
       </plugin>
     </plugins>
   </build>
   ```

## Release Process

1. **Update version to release version**
   ```bash
   # Change from 0.10-SNAPSHOT to 0.10
   mvn versions:set -DnewVersion=0.10
   ```

2. **Deploy to staging**
   ```bash
   mvn clean deploy -P release
   ```

3. **Release from staging** (if autoReleaseAfterClose is false)
   ```bash
   mvn nexus-staging:release
   ```

4. **Update to next SNAPSHOT**
   ```bash
   mvn versions:set -DnewVersion=0.11-SNAPSHOT
   git add pom.xml
   git commit -m "Prepare for next development iteration"
   ```

## GitHub Actions Integration

Create `.github/workflows/maven-central-release.yml`:

```yaml
name: Release to Maven Central

on:
  workflow_dispatch:
    inputs:
      releaseVersion:
        description: 'Release version'
        required: true
        default: '0.10'
      nextVersion:
        description: 'Next development version'
        required: true
        default: '0.11-SNAPSHOT'

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '23'
          distribution: 'temurin'
          cache: 'maven'
          server-id: ossrh
          server-username: MAVEN_USERNAME
          server-password: MAVEN_PASSWORD
          gpg-private-key: ${{ secrets.GPG_PRIVATE_KEY }}
          gpg-passphrase: GPG_PASSPHRASE
      
      - name: Configure Git
        run: |
          git config user.name "GitHub Actions"
          git config user.email "actions@github.com"
      
      - name: Release to Maven Central
        env:
          MAVEN_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          MAVEN_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}
          GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
        run: |
          mvn versions:set -DnewVersion=${{ github.event.inputs.releaseVersion }}
          mvn clean deploy -P release
          
          git add pom.xml
          git commit -m "Release version ${{ github.event.inputs.releaseVersion }}"
          git tag -a v${{ github.event.inputs.releaseVersion }} -m "Release version ${{ github.event.inputs.releaseVersion }}"
          
          mvn versions:set -DnewVersion=${{ github.event.inputs.nextVersion }}
          git add pom.xml
          git commit -m "Prepare for next development iteration"
          
          git push origin main --tags
```

## Required GitHub Secrets

- `OSSRH_USERNAME`: Your Sonatype username
- `OSSRH_PASSWORD`: Your Sonatype password
- `GPG_PRIVATE_KEY`: Your GPG private key (exported with `gpg --armor --export-secret-keys`)
- `GPG_PASSPHRASE`: Your GPG key passphrase

## Verification

After release, artifacts will be available at:
- https://repo1.maven.org/maven2/io/github/simbo1905/mvn2llm/
- https://mvnrepository.com/artifact/io.github.simbo1905/mvn2llm

Sync to Maven Central typically takes 10-30 minutes.