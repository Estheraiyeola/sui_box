````
# testSui

A Java project demonstrating **Sui Box** usage: scaffolding, publishing, and interacting with Sui Move contracts directly from Java. This README walks you through:

- Project overview  
- Prerequisites  
- Cloning / downloading  
- Dependency setup (Maven)  
- Move package configuration  
- Building & running Java code  
- Folder structure  
- Troubleshooting tips  

---

## 📦 Project Overview

This repository contains:

1. A **Maven-based Java application** (`testSui`) that uses **Sui Box** (an annotation processor and runtime library) to generate Move modules from annotated Java entities, compile/publish them to Sui Testnet, and invoke entry functions.  
2. A **`move/` directory** where Move sources (`.move` files) and `Move.toml` reside. These are generated and managed by `Sui Box`.  
3. A sample `Main.java` showing how to:
   - Build Move bytecode  
   - Publish the Move package to Testnet  
   - Create a registry object in your User module  
   - Call an entry function (`create`) on that module  

---

## 🛠️ Prerequisites

1. **JDK 17+** (Java·OpenJDK, Oracle JDK, or equivalent) installed and on your `PATH`.  
2. **Maven 3.6+** installed and on your `PATH`.  
3. **Sui CLI** (Testnet-aligned) installed and accessible from your shell:
   - **Linux/macOS** (recommended):  
     ```bash
     curl -sL https://install.sui.io | bash
     ```
     After installation, restart your shell and verify:  
     ```bash
     sui --version
     # Expect something like: sui CLI version 1.49.x (branch: framework/testnet)
     ```

   - **Windows**:  
     - **Via Scoop** (if you use Scoop):  
       ```powershell
       scoop bucket add mystery https://github.com/MystenLabs/sui.git
       scoop install sui
       ```  
     - **Via Chocolatey**:  
       ```powershell
       choco install sui
       ```  
     - Or run the same official install script in Git Bash / WSL:  
       ```bash
       curl -sL https://install.sui.io | bash
       ```  
     After installing, open a new CMD/Powershell and run:  
     ```powershell
     sui --version
     ```

4. **(Optional)** **Rust Nightly + Cargo** if you ever need to build Sui CLI from source—_not_ required if you use the install script or packages above.

---

## 📥 Clone or Download

```bash
git clone https://github.com/Estheraiyeola/sui_box.git
cd testSui
````

> If you only have the `sui-box-1.0.0.jar`, ensure it lives under `testSui/lib/sui-box-1.0.0.jar`.

---

## 🔗 Dependency Setup

### 1. Install `sui-box-1.0.0.jar` into Local Maven Repo

```bash
mvn install:install-file \
  -Dfile=lib/sui-box-1.0.0.jar \
  -DgroupId=org.example \
  -DartifactId=sui-box \
  -Dversion=1.0.0 \
  -Dpackaging=jar
```

This allows Maven to resolve `org.example:sui-box:1.0.0` by coordinates (no `<systemPath>` needed).

### 2. `pom.xml` Highlights

Below is the essential portion of `pom.xml` that makes everything work:

```xml
<project …>
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.example</groupId>
  <artifactId>testSui</artifactId>
  <version>1.0-SNAPSHOT</version>

  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencies>
    <!-- 1) SuiBox (installed locally) -->
    <dependency>
      <groupId>org.example</groupId>
      <artifactId>sui-box</artifactId>
      <version>1.0.0</version>
    </dependency>

    <!-- 2) Sui4j (Java client for Sui) -->
    <dependency>
      <groupId>me.grapebaba</groupId>
      <artifactId>sui4j</artifactId>
      <version>1.0.0-alpha</version>
    </dependency>

    <!-- 3) Jackson for JSON parsing (Move call RPC) -->
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-core</artifactId>
      <version>2.14.0</version>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.14.0</version>
    </dependency>

    <!-- 4) SLF4J binding (avoids “No SLF4J providers” warn) -->
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
      <version>2.0.5</version>
    </dependency>

    <!-- 5) JUnit for tests (optional) -->
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>5.8.1</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <!-- A) Compiler Plugin: configures annotation processing -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.13.0</version>
        <configuration>
          <fork>true</fork>
          <compilerArgs>
            <!-- Where generated Move modules go -->
            <arg>-J-Dsui.move.dir=${project.basedir}/move</arg>
          </compilerArgs>
          <annotationProcessorPaths>
            <!-- SuiBox annotation processor -->
            <path>
              <groupId>org.example</groupId>
              <artifactId>sui-box</artifactId>
              <version>1.0.0</version>
            </path>
            <!-- FreeMarker (used by SuiBox) -->
            <path>
              <groupId>org.freemarker</groupId>
              <artifactId>freemarker</artifactId>
              <version>2.3.31</version>
            </path>
          </annotationProcessorPaths>
        </configuration>
      </plugin>

      <!-- B) Exec Maven Plugin: runs Main.java with working dir = move/ -->
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.1.0</version>
        <configuration>
          <!-- Ensures “sui move build” / “sui client publish” sees Move.toml -->
          <workingDirectory>move</workingDirectory>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

**Key Points**:

* **`<annotationProcessorPaths>`**:

  * Only `<groupId>`, `<artifactId>`, `<version>` (no `<systemPath>`, no `<scope>`).
  * Includes `sui-box` and `freemarker`.
* **`<workingDirectory>move</workingDirectory>`**:

  * Makes `exec:java` run from inside `move/` so CLI commands find `Move.toml`.

---

## ⚙️ Move Package Configuration (`move/Move.toml`)

Create or update `move/Move.toml` with:

```toml
[package]
name    = "testSui"
version = "0.0.1"

[addresses]
user = "0x0"   # Replace with your Sui address or leave as 0x0 for testing
sui  = "0x2"
std  = "0x1"
```

> **Important**: Remove any `[dependencies]` for Sui or MoveStdlib. Starting with Sui v1.45+, the CLI auto-injects and verifies the on-chain versions of those modules.

Your `move/` directory should look like:

```
move/
├── Move.toml
└── sources/       ← Populated by SuiBox at compile-time
    └── User.move  ← Example generated module
```

---

## 🚀 Build & Run

1. **Generate Move modules via annotation processing**
   From the project root:

   ```bash
   mvn clean compile
   ```

   * This triggers SuiBox’s annotation processor.
   * Java classes annotated with `@BlockchainEntity` (e.g., `org.example.model.User`) produce `.move` files under `move/sources/`.
   * You should see logs indicating “BUILDING user” (or similar) and that `User.move` was generated.

2. **Publish Move package to Testnet & run Main**
   Still from project root:

   ```bash
   mvn exec:java -Dexec.mainClass=org.example.Main
   ```

   * Because `exec-maven-plugin`’s `workingDirectory` is `move/`, this runs your `Main` in that folder.
   * `Main.java` calls:

     1. `buildMoveCode(projectRoot)` → `sui move build --path move`
     2. `publish(projectRoot)`       → `sui client publish move … --json`
     3. `createRegistry(...)`        → uses `sui client ptb --move-call ...`
     4. `moveCall(...)`              → invokes your `create(...)` entry function.
   * On success, you’ll see:

     ```
     Deployed pkg: <PACKAGE_ID>
     Registry ID:  <NEW_REGISTRY_OBJECT_ID>
     Tx digest:    <TX_DIGEST>
     ```

> **Note**: If you prefer to run from your IDE (IntelliJ / VSCode), ensure the “Working Directory” for your run configuration is set to `${PROJECT_ROOT}/move`. Otherwise, the CLI will not find `Move.toml`.

---

## 📁 Folder Structure

```
testSui/
├── lib/
│   └── sui-box-1.0.0.jar        ← Copied locally and installed to ~/.m2
├── move/
│   ├── Move.toml                ← Package and address declarations
│   └── sources/                 ← Populated by annotation processor
│       └── User.move            ← Generated file (example)
├── src/
│   └── main/
│       └── java/
│           └── org/example/
│               ├── Main.java              ← Demonstrates build, publish, calls
│               ├── model/
│               │   └── User.java           ← Annotated with @BlockchainEntity
│               └── blockchain/
│                   └── SuiContractManager.java
├── pom.xml                      ← Configured as shown above
└── README.md                    ← (You are here)
```

* **`lib/`**: Contains `sui-box-1.0.0.jar` before installing to local repo.
* **`move/`**:

  * `Move.toml` (no dependencies section)
  * `sources/` (where SuiBox writes generated `.move` files)
* **`src/main/java/org/example/…`**:

  * `model/User.java` annotated with `@BlockchainEntity(module="User", struct="User")`.
  * `blockchain/SuiContractManager.java` handles `sui` CLI calls: build, publish, ptb, etc.
  * `Main.java` ties it all together.

---

## 🛠️ Troubleshooting

### “Cannot find ‘scope’ in class org.apache.maven.plugin.compiler.DependencyCoordinate”

* Do **not** put `<scope>` or `<systemPath>` in `<annotationProcessorPaths>`.
* Use only `<groupId>`, `<artifactId>`, `<version>`.

### “NoClassDefFoundError: freemarker/template/Configuration”

* Add FreeMarker under `<annotationProcessorPaths>`:

  ```xml
  <path>
    <groupId>org.freemarker</groupId>
    <artifactId>freemarker</artifactId>
    <version>2.3.31</version>
  </path>
  ```

### “Client/Server api version mismatch”

* Install a Testnet-aligned CLI. Running `sui --version` should output something like:

  ```
  sui CLI version 1.49.x (branch: framework/testnet)
  ```
* If you still see a mismatch warning, reinstall via the official script:

  ```bash
  curl -sL https://install.sui.io | bash
  ```

### “Multiple source verification errors” (during `publish`)

* Remove any `[dependencies]` for `Sui` or `MoveStdlib` from `Move.toml`.
* The Sui CLI v≥1.45 auto-injects and verifies on-chain versions; you do not need to list them manually.

### “Publish failed – No such file or directory”

* Ensure the process is running **inside** the `move/` directory (so it finds `Move.toml`).
* If running from the command line:

  ```bash
  cd move
  sui client publish --gas <GAS_OBJ> --gas-budget 50000000 --json
  ```
* If running via Maven’s `exec:java`, make sure `<workingDirectory>move</workingDirectory>` is set.

---

## 🎓 License

This project is distributed under the MIT License. © Esther Aiyeola

---

### ✔️ Quick Recap

1. **Install** `sui-box-1.0.0.jar` to your local Maven repo.
2. **Ensure** `pom.xml` references `sui-box`, `sui4j`, `jackson-core/databind`, `slf4j-simple`, and FreeMarker properly.
3. **Remove** any `[dependencies]` for Sui or MoveStdlib from `move/Move.toml`.
4. Run:

   ```bash
   mvn clean compile
   mvn exec:java -Dexec.mainClass=org.example.Main
   ```
5. Watch your Move modules compile, publish, and execute on Testnet!

You’re now fully set up to scaffold, deploy, and interact with Sui Move contracts directly from Java. Happy coding!

```
```
