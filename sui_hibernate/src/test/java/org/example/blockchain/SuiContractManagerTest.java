package org.example.blockchain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import io.github.cdimascio.dotenv.Dotenv;
import io.sui.Sui;
import io.sui.bcsgen.TypeTag;
import io.sui.models.transactions.TransactionBlockResponse;
import io.sui.models.transactions.TransactionBlockResponseOptions;
import org.example.annotation.BlockchainEntity;
import org.example.processor.BlockchainEntityProcessor;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.example.blockchain.SuiContractManager.buildMoveCode;
import static org.junit.jupiter.api.Assertions.*;

public class SuiContractManagerTest {
    private Path tempDir;
    private SuiContractManager mgr;
    private Dotenv env;
    private String senderAddress;
    private String keystorePath;
    private String faucetUrl;
    private String fullnodeUrl;
    private String gasObject;

    @BeforeEach
    public void setUp() throws IOException {
        // Create temporary directory for test output
        tempDir = Files.createTempDirectory("test-sui-" + UUID.randomUUID());
        Files.createDirectories(tempDir.resolve("sources"));
        Files.createDirectories(tempDir.resolve("test/templates"));
        Files.createDirectories(tempDir.resolve("org/example/templates"));
        System.setProperty("test.move.dir", tempDir.toString());
        System.setProperty("test.java.dir", tempDir.toString());
        System.out.println("Temporary directory: " + tempDir.toAbsolutePath());

        // Load environment variables
        env = Dotenv.configure().directory("src/.env").load();
        senderAddress = env.get("SUI_SENDER_ADDRESS");
        keystorePath = env.get("SUI_KEYSTORE_PATH");
        faucetUrl = env.get("SUI_FAUCET_URL");
        fullnodeUrl = env.get("SUI_FULLNODE_URL");
        gasObject = env.get("SUI_GAS_OBJECT");

        // Validate environment variables
        if (senderAddress == null) {
            throw new IllegalStateException("SUI_SENDER_ADDRESS not set in src/.env file");
        }
        if (gasObject == null) {
            throw new IllegalStateException("SUI_GAS_OBJECT not set in src/.env file");
        }
        if (fullnodeUrl == null) {
            throw new IllegalStateException("SUI_FULLNODE_URL not set in src/.env file");
        }
        if (faucetUrl == null) {
            throw new IllegalStateException("SUI_FAUCET_URL not set in src/.env file");
        }
        if (keystorePath == null) {
            throw new IllegalStateException("SUI_KEYSTORE_PATH not set in src/.env file");
        }

        // Initialize Sui client and contract manager
        Sui suiClient = new Sui(fullnodeUrl, faucetUrl, keystorePath);
        mgr = new SuiContractManager(suiClient, senderAddress, gasObject, 50000000L, 1000L);
    }

    @Test
    public void testMoveContractGenerationAndDeployment() throws Exception {
        // Step 1: Create temporary Move project directory
        Path tempMoveDir = Files.createTempDirectory("test-move-" + UUID.randomUUID());
        Path sourcesDir = tempMoveDir.resolve("sources");
        Files.createDirectories(sourcesDir);
        System.out.println("Created Move project directory: " + tempMoveDir.toAbsolutePath());

        // Step 2: Set system properties for processor
        System.setProperty("test.move.dir", tempMoveDir.toString());
        System.setProperty("test.java.dir", tempMoveDir.toString());

        // Step 3: Compile test Java source
        JavaFileObject input = JavaFileObjects.forSourceLines(
                "test.Bar",
                "package test;",
                "import org.example.annotation.BlockchainEntity;",
                "@BlockchainEntity(module = \"foo\", struct = \"Bar\")",
                "public class Bar {",
                "    private String name;",
                "    private long count;",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new BlockchainEntityProcessor())
                .compile(input);
        assertThat(compilation).succeededWithoutWarnings();

        // Step 4: Verify and overwrite foo.move
        File[] sourceFiles = sourcesDir.toFile().listFiles();
        System.out.println("Files in sourcesDir after processing: " +
                (sourceFiles != null ? Arrays.toString(sourceFiles) : "null"));
        assertNotNull(sourceFiles, "Sources directory is empty");
        assertTrue(Arrays.stream(sourceFiles).anyMatch(f -> f.getName().equals("foo.move")),
                "foo.move not found in sources directory");


        // Step 5: Create Move.toml
        Path moveTomlPath = tempMoveDir.resolve("Move.toml");
        String moveTomlContent = """
            [package]
            name = "test_package"
            version = "0.0.1"
            edition = "2024"

            [addresses]
            foo = "0x0"

            [dependencies]
            """;
        Files.writeString(moveTomlPath, moveTomlContent);
        System.out.println("Wrote Move.toml to: " + moveTomlPath.toAbsolutePath());

        // Step 6: Compile Move files to bytecode
        File[] bytecodeFiles = buildMoveCode(tempMoveDir);
        System.out.println("Files in bytecodeDir: " +
                (bytecodeFiles != null ? Arrays.toString(bytecodeFiles) : "null"));
        assertNotNull(bytecodeFiles, "Bytecode directory is empty");
        assertTrue(bytecodeFiles.length > 0, "No bytecode files generated");

        // Step 7: Publish the contract using sui client publish
        String packageId = mgr.publish(tempMoveDir);
        assertNotNull(packageId, "Deployment failed: Package ID is null");
        assertTrue(packageId.startsWith("0x"), "Invalid package ID: " + packageId);
        System.out.println("Deployed contract with package ID: " + packageId);


    }

    @Test
    public void testMoveContractGeneration_DeploymentAnd_Calling_Transactions() throws Exception {
        // Step 1: Create temporary Move project directory
        Path tempMoveDir = Files.createTempDirectory("test-move-" + UUID.randomUUID());
        Path sourcesDir = tempMoveDir.resolve("sources");
        Files.createDirectories(sourcesDir);
        System.out.println("Created Move project directory: " + tempMoveDir.toAbsolutePath());

        // Step 2: Set system properties for processor
        System.setProperty("test.move.dir", tempMoveDir.toString());
        System.setProperty("test.java.dir", tempMoveDir.toString());

        // Step 3: Compile test Java source
        JavaFileObject input = JavaFileObjects.forSourceLines(
                "test.Bar",
                "package test;",
                "import org.example.annotation.BlockchainEntity;",
                "@BlockchainEntity(module = \"foo\", struct = \"Bar\")",
                "public class Bar {",
                "    private String name;",
                "    private long count;",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new BlockchainEntityProcessor())
                .compile(input);
        assertThat(compilation).succeededWithoutWarnings();

        // Step 4: Verify and overwrite foo.move
        File[] sourceFiles = sourcesDir.toFile().listFiles();
        System.out.println("Files in sourcesDir after processing: " +
                (sourceFiles != null ? Arrays.toString(sourceFiles) : "null"));
        assertNotNull(sourceFiles, "Sources directory is empty");
        assertTrue(Arrays.stream(sourceFiles).anyMatch(f -> f.getName().equals("foo.move")),
                "foo.move not found in sources directory");


        // Step 5: Create Move.toml
        Path moveTomlPath = tempMoveDir.resolve("Move.toml");
        String moveTomlContent = """
            [package]
            name = "test_package"
            version = "0.0.1"
            edition = "2024"

            [addresses]
            foo = "0x0"

            [dependencies]
            """;
        Files.writeString(moveTomlPath, moveTomlContent);
        System.out.println("Wrote Move.toml to: " + moveTomlPath.toAbsolutePath());

        // Step 6: Compile Move files to bytecode
        File[] bytecodeFiles = buildMoveCode(tempMoveDir);
        System.out.println("Files in bytecodeDir: " +
                (bytecodeFiles != null ? Arrays.toString(bytecodeFiles) : "null"));
        assertNotNull(bytecodeFiles, "Bytecode directory is empty");
        assertTrue(bytecodeFiles.length > 0, "No bytecode files generated");

        // Step 7: Publish the contract using sui client publish
        String packageId = mgr.publish(tempMoveDir);
        assertNotNull(packageId, "Deployment failed: Package ID is null");
        assertTrue(packageId.startsWith("0x"), "Invalid package ID: " + packageId);
        System.out.println("Deployed contract with package ID: " + packageId);


        // Create Registry and get registryId
        String registryId = mgr.createRegistry("Bar", tempMoveDir, packageId);
        System.out.println("Registry ID: " + registryId);

        // Call create function
        String createDigest = mgr.moveCall(
                "Bar",
                "create",
                Arrays.asList( "Alice", 25, registryId),
                tempMoveDir,
                packageId,
                true,
                senderAddress
        );
        System.out.println("Create Bar Digest: " + createDigest);
    }




}