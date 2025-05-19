# Sui Box

A Java library and annotation processor for scaffolding, publishing & interacting with Sui Move contracts directly from Java.

---

## 📦 Download

Grab the latest `sui-box-1.0.0.jar` from our GitHub Releases:

* [Download `sui-box-1.0.0.jar`](https://github.com/Estheraiyeola/sui-box/releases/download/v1.0.0/sui-box-1.0.0.jar)

---

## 🏗️ Setup in your Java project

1. **Place the JAR**

   * Copy `sui-box-1.0.0.jar` into your project, e.g. a `lib/` folder.

2. **Add as dependency**

   * **Maven** (`pom.xml`):

     ```xml
     <dependency>
       <groupId>org.example</groupId>
       <artifactId>sui-box</artifactId>
       <version>1.0.0</version>
       <scope>system</scope>
       <systemPath>${project.basedir}/lib/sui-box-1.0.0.jar</systemPath>
     </dependency>
     ```

   * **Gradle** (`build.gradle`):

     ```groovy
     dependencies {
       implementation files("lib/sui-box-1.0.0.jar")
     }
     ```

3. **Annotate your entities** *(if you want codegen)*

   ```java
   package org.example.model;

   import org.example.annotation.BlockchainEntity;

   @BlockchainEntity(module = "User", struct = "User")
   public class User {
       public String name;
       public long age;
       public String email;
   }
   ```

4. **Configure annotation‑processor** (Maven Compiler Plugin example):

   ```xml
   <build>
     <plugins>
       <plugin>
         <groupId>org.apache.maven.plugins</groupId>
         <artifactId>maven-compiler-plugin</artifactId>
         <version>3.13.0</version>
         <configuration>
           <fork>true</fork>
           <compilerArgs>
             <!-- where to write generated Move modules -->
             <arg>-J-Dsui.move.dir=${project.basedir}/move</arg>
           </compilerArgs>
           <annotationProcessorPaths>
             <path>
               <groupId>org.example</groupId>
               <artifactId>sui-box</artifactId>
               <version>1.0.0</version>
               <systemPath>${project.basedir}/lib/sui-box-1.0.0.jar</systemPath>
             </path>
           </annotationProcessorPaths>
         </configuration>
       </plugin>
     </plugins>
   </build>
   ```

   After `mvn compile`, you’ll get:

   ```text
   move/
   ├── Move.toml
   └── sources/
       └── User.move    # generated from @BlockchainEntity
   ```

---

## 🚀 Runtime Usage

Below is a self‑contained `Main.java` that shows how to compile/publish Move, create a registry, and invoke an entry function:

```java
package org.example;

import io.sui.Sui;
import org.example.blockchain.SuiContractManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.io.File;

import static org.example.blockchain.SuiContractManager.buildMoveCode;

public class Main {
    public static void main(String[] args) throws Exception {
        // 1) Sui config
        String sender   = "0x...";
        String gasObj   = "0x...";
        String fullnode = "https://rpc.testnet.sui.io:443";
        String faucet   = "https://faucet.testnet.sui.io:443";
        String keystore = "/home/user/.sui/sui_config/sui.keystore";

        Sui sui = new Sui(fullnode, faucet, keystore);
        SuiContractManager mgr = new SuiContractManager(
            sui, sender, gasObj, 50_000_000L, 1000L
        );

        // 2) Point to Move project root
        Path projectRoot = Paths.get("move");
        Files.createDirectories(projectRoot.resolve("sources"));
        // ensure Move.toml exists in projectRoot

        // 3) Build Move bytecode
        File[] bytecode = buildMoveCode(projectRoot);

        // 4) Publish package
        String pkgId = mgr.publish(projectRoot);
        System.out.println("Deployed pkg: " + pkgId);

        // 5) Create registry (module = "User")
        String regId = mgr.createRegistry("User", projectRoot, pkgId);
        System.out.println("Registry ID: " + regId);

        // 6) Call entry function
        String tx = mgr.moveCall(
            "User",             // module
            "create",           // function
            List.of("Alice", 42L, "alice@example.com", regId),
            projectRoot,
            pkgId,
            true,                // assign & transfer
            sender               // recipient of the new object
        );
        System.out.println("Tx digest: " + tx);
    }
}
```

---


## 📄 License

MIT © Esther Aiyeola
