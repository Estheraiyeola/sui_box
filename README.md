# testSui

A Java project demonstrating **Sui Box**: generate, publish, and interact with Sui Move contracts from Java.

---

## Overview

- **Java + Maven** app uses **Sui Box** (annotation processor) to emit Move modules from `@BlockchainEntity`-annotated classes.
- Move sources live under `move/`; `Move.toml` declares package and addresses.
- A sample `Main.java` builds/publishes the Move package to Testnet, creates a registry, and invokes an entry function.

---

## Prerequisites

1. **JDK 17+ & Maven 3.6+** on `PATH`.  
2. **Sui CLI (Testnet-aligned)**:
   - **Linux/macOS**:  
     ```bash
     curl -sL https://install.sui.io | bash
     ```
     Verify:  
     ```bash
     sui --version  # e.g. “1.49.x (branch: framework/testnet)”
     ```
   - **Windows** (Scoop/Chocolatey or Git Bash install script):
     ```powershell
     scoop install sui  # —or—
     choco install sui
     # —or in WSL/Git Bash:
     curl -sL https://install.sui.io | bash
     ```

---

## Setup

1. **Clone the repo**  
   ```bash
   git clone https://github.com/Estheraiyeola/sui_box.git
   cd testSui
