# Sui Contract Manager

A Java library that simplifies Sui blockchain development by enabling developers to define and interact with smart contracts using Java, without learning Move. Annotate Java classes, and the library automatically generates Move code, deploys it to the Sui testnet, and provides methods to call contract functions.

## Features
- **Annotation-Based Contracts**: Define smart contracts with Java annotations; Move code is generated automatically.
- **Contract Deployment**: Deploy generated Move contracts to the Sui testnet using `SuiContractManager`.
- **Function Calls**: Invoke contract functions like `create`, `update_by_id`, `delete`, and more.
- **Open-Source**: Contribute and collaborate at [github.com/Estheraiyeola/sui-box](https://github.com/Estheraiyeola/sui-box).

## Installation

Add the library to your Maven project:

```xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>sui-box</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Prerequisites
- **Java 11 or higher**

- **Maven 3.6+**

- **Sui CLI installed Sui Installation Guide**

- **Access to a Sui testnet fullnode (e.g., https://fullnode.testnet.sui.io:443)**

- **A Sui wallet with a sender address and gas object ID**

## Usage
### Step 1: Define a Contract
Annotate a Java class to represent a smart contract. The processor generates Move code based on the fields.
java
```
import org.example.blockchain.SuiContract;

@SuiContract(module = "foo")
public class Bar {
private String id;      // Auto-generated Sui object ID
private String name;    // String field
private int value;      // Integer field

    // Constructors, getters, setters
    public Bar() {}
    public Bar(String name, int value) {
        this.name = name;
        this.value = value;
    }
}
```

This generates a Move contract with:
A Registry to track object IDs

A Bar struct with id, is_deleted, name, and value

Functions: 
- **create_registry**
- **create**
- **update_by_id**
- **find_by_id**
- **get_all**
- **delete**

### Step 2: Build and Publish the Contract
Use SuiContractManager to build and publish the generated Move code to the Sui testnet.

```
import org.example.blockchain.SuiContractManager;
import io.sui.Sui;
import java.nio.file.Path;

Sui suiClient = new Sui("https://fullnode.testnet.sui.io:443");
SuiContractManager manager = new SuiContractManager(
suiClient,
"0xyourSenderAddress",    // Your wallet address
"0xyourGasObjectId",     // Gas object ID
50000000L,               // Gas budget
1000L                    // Gas price
);

// Build the Move code
Path moveDir = Path.of("path/to/generated/move/package");
File[] bytecodeFiles = manager.buildMoveCode(moveDir);
System.out.println("Built " + bytecodeFiles.length + " bytecode files");

// Publish the contract
String packageId = manager.publish(moveDir);
System.out.println("Published package ID: " + packageId);


```

### Step 3: Create a Registry
Create a shared Registry to track objects.

```
String registryId = manager.createRegistry("foo", Path.of("working/dir"), packageId);
System.out.println("Registry ID: " + registryId);

```

### Step 4: Call Contract Functions
Use SuiContractManager.moveCall to invoke contract functions.

```
// Create a Bar object
String digest = manager.moveCall(
    "foo",                // Module name
    "create",            // Function name
    List.of("TestBar", 42, registryId), // Args: name, value, registry ID
    Path.of("working/dir"),
    packageId,
    false,               // No transfer
    null
);
System.out.println("Transaction digest: " + digest);

// Update a Bar object
String barId = "0xparsedBarId"; // Replace with actual object ID
manager.moveCall(
    "foo",
    "update_by_id",
    List.of(barId, "NewName", 100), // Args: object ID, new name, new value
    Path.of("working/dir"),
    packageId,
    false,
    null
);

// Delete a Bar object
manager.moveCall(
    "foo",
    "delete",
    List.of(barId), // Args: object ID
    Path.of("working/dir"),
    packageId,
    false,
    null
);
```

## Project Structure
- Annotation: @SuiContract defines the Move module and fields.

- Processor: Generates Move code during compilation.

- SuiContractManager: Manages building, publishing, and function calls.

## Contributing
We welcome contributions! Please:
- Fork the repository.

- Create a feature branch (git checkout -b feature/YourFeature).

- Commit changes (git commit -m 'Add YourFeature').

- Push to the branch (git push origin feature/YourFeature).

- Open a pull request.

Report issues or suggest features at github.com/Estheraiyeola/sui-box.
## Troubleshooting
- **Build fails**: Ensure the Sui CLI is installed and the Move code path is correct.

- **Publish fails**: Verify your wallet has sufficient gas and the correct gas object ID.

- **Move call errors**: Check the transaction output for warnings or malformed JSON.

## Resources
- *Sui Documentation* - https://docs.sui.io/

- *Move Language* - https://move-language.org/

- *Sui Testnet* - https://testnet.sui.io/

## License
MIT License. See LICENSE for details.
Contact
Esther Aiyeola - estheraiyeola@yahoo.com