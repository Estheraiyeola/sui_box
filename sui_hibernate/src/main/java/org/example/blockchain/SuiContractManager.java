package org.example.blockchain;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import io.sui.Sui;
import io.sui.models.SuiApiException;
import io.sui.models.objects.ObjectChange;
import io.sui.models.transactions.*;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class SuiContractManager {
    private String pkg;
    private final Sui suiClient;
    private final String senderAddress;
    private final String gasObjectId;
    private final long gasBudget;
    private final long gasPrice;
    private final String rpc = "https://fullnode.testnet.sui.io:443";
    private final HttpClient httpClient = HttpClient.newHttpClient();



    public SuiContractManager(Sui suiClient, String senderAddress,
                              String gasObjectId, long gasBudget, long gasPrice) {
        this.suiClient = suiClient;
        this.senderAddress = senderAddress;
        this.gasObjectId = gasObjectId;
        this.gasBudget = gasBudget;
        this.gasPrice = gasPrice;
    }

    public String getPackageId() {
        return pkg;
    }

    public CompletableFuture<String> deployContract(Path packagePath) throws Exception {
        System.out.println("Deploying contract with gasObjectId: " + gasObjectId + ", gasBudget: " + gasBudget + ", gasPrice: " + gasPrice);
        File dir = new File(packagePath.toString());
        File[] moduleFiles = dir.listFiles((d, name) -> name.endsWith(".mv"));
        if (moduleFiles == null || moduleFiles.length == 0) {
            throw new IllegalArgumentException("No .mv files in " + dir);
        }
        List<String> modulesBase64 = new ArrayList<>();
        for (File f : moduleFiles) {
            byte[] b = Files.readAllBytes(f.toPath());
            modulesBase64.add(Base64.getEncoder().encodeToString(b));
        }
        System.out.println("Modules to publish: " + moduleFiles.length);

        TransactionBlockResponseOptions options = new TransactionBlockResponseOptions();
        options.setShowEffects(true);
        options.setShowEvents(true);
        options.setShowInput(true);
        options.setShowObjectChanges(true);

        List<String> dependencies = List.of(
                "0x0000000000000000000000000000000000000000000000000000000000000001",
                "0x0000000000000000000000000000000000000000000000000000000000000002");

        CompletableFuture<TransactionBlockResponse> future = suiClient.publish(
                senderAddress,
                modulesBase64,
                dependencies,
                gasObjectId,
                gasBudget,
                gasPrice,
                null,
                options,
                ExecuteTransactionRequestType.WaitForLocalExecution
        );

        System.out.println("Transaction future: " + future.toString());
        System.out.println("Waiting for publish response...");
        System.out.println("Options: " + options);

        return future.thenApply(resp -> {
            String packageId = resp.getObjectChanges().stream()
                    .filter(c -> c instanceof ObjectChange.ObjectChangePublished)
                    .map(c -> ((ObjectChange.ObjectChangePublished) c).getPackageId())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No PackageID found in publish response"));
            System.out.println("Published package ID: " + packageId);
            this.pkg = packageId; // Store package ID
            return packageId;
        }).exceptionally(throwable -> {
            Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
            String errorMessage = "Failed to deploy contract with gasObjectId: " + gasObjectId +
                    ", senderAddress: " + senderAddress;
            if (cause instanceof SuiApiException) {
                SuiApiException apiEx = (SuiApiException) cause;
                errorMessage += ", SuiApiException: " + apiEx.getMessage() +
                        ", Code: " + apiEx.getMessage() +
                        ", Error: " + apiEx.getError();
                System.err.println(errorMessage);
                apiEx.printStackTrace(System.err);
            } else {
                errorMessage += ", Unexpected error: " + cause.getMessage();
                System.err.println(errorMessage);
                cause.printStackTrace(System.err);
            }
            throw new CompletionException(errorMessage, cause);
        });
    }


    public String createRegistry(String module, Path workingDir, String packageId)
            throws IOException, InterruptedException {
        // Call create_registry
        String digest = moveCall(module, "create_registry", Collections.emptyList(), workingDir, packageId, false, null);

        // Parse the Registry ID from the transaction
        return parseRegistryIdFromTransaction(digest, module,packageId);
    }

    public String moveCall(String module, String function, List<Object> args, Path workingDir, String packageId,
                           boolean assignAndTransfer, @Nullable String transferToAddress)
            throws IOException, InterruptedException {
        // Validate inputs
        Objects.requireNonNull(module, "Module name cannot be null");
        Objects.requireNonNull(function, "Function name cannot be null");
        Objects.requireNonNull(args, "Args list cannot be null");
        Objects.requireNonNull(packageId, "Package ID cannot be null");
        if (assignAndTransfer && transferToAddress == null) {
            throw new IllegalArgumentException("Transfer address cannot be null when assignAndTransfer is true");
        }

        // Build the command
        List<String> command = new ArrayList<>();
        command.add("sui");
        command.add("client");
        command.add("ptb");
        command.add("--move-call");
        command.add(packageId + "::" + module + "::" + function);

        // Log arguments for debugging
        System.out.println("Processing arguments for moveCall: " + args);

        // Add arguments, ensuring correct type handling
        for (int i = 0; i < args.size(); i++) {
            Object arg = args.get(i);
            if (arg == null) {
                throw new IllegalArgumentException("Argument at index " + i + " cannot be null");
            }
            String s;
            if (arg instanceof Number) {
                // Numbers (u64, etc.) are passed as-is
                s = arg.toString();
            } else if (arg instanceof String str) {
                if (str.startsWith("0x")) {
                    // Object IDs with @ prefix for shared objects
                    s = "@" + str;
                } else {
                    // Strings are JSON-quoted for PTB
                    s = "\"" + str + "\"";
                }
            } else {
                throw new IllegalArgumentException("Unsupported argument type at index " + i + ": " + arg.getClass().getName());
            }
            command.add(s);
            System.out.println("Added argument at index " + i + ": " + s);
        }

        // Conditionally add assign and transfer-objects steps
        if (assignAndTransfer) {
            command.add("--assign");
            command.add("bar_obj");
            command.add("--transfer-objects");
            command.add("[bar_obj]");
            command.add("@" + transferToAddress);
        }

        // Add gas and execution flags
        command.add("--gas-budget");
        command.add(String.valueOf(gasBudget));
        command.add("--json");

        // Set up ProcessBuilder
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (workingDir != null) {
            processBuilder.directory(workingDir.toFile());
        }

        // Redirect error stream to capture all output
        processBuilder.redirectErrorStream(true);

        // Log command for debugging
        System.out.println("Executing command: " + String.join(" ", command));

        // Execute the command
        Process process = processBuilder.start();

        // Capture output
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = stdOut.readLine()) != null) {
                output.append(line).append("\n");
            }
            while ((line = stdErr.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }

        // Wait for completion
        boolean completed = process.waitFor(5, TimeUnit.MINUTES);
        if (!completed) {
            process.destroy();
            throw new RuntimeException("Move call timed out after 5 minutes");
        }

        // Check exit code
        if (process.exitValue() != 0) {
            throw new RuntimeException("Move call failed with exit code " + process.exitValue() +
                    "\nError: " + errorOutput + "\nOutput: " + output);
        }

        // Clean output to remove warnings before parsing JSON
        String raw = output.toString().trim();
        System.out.println("Raw output before cleaning: " + raw);
        // Find the first '{' to start JSON
        int jsonStart = raw.indexOf("{");
        if (jsonStart == -1) {
            throw new RuntimeException("No valid JSON found in command output: " + raw);
        }
        raw = raw.substring(jsonStart);
        System.out.println("Cleaned JSON output: " + raw);

        // Parse JSON output
        ObjectMapper mapper = new ObjectMapper();
        System.out.println("RPC payload: " + raw);
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(raw);
        } catch (JsonParseException e) {
            throw new RuntimeException("Failed to parse JSON output: " + raw + "\nError: " + e.getMessage(), e);
        }

        // Check transaction status
        String status = rootNode.path("effects").path("status").path("status").asText();
        if (!"success".equals(status)) {
            String error = rootNode.path("effects").path("status").path("error").asText();
            throw new RuntimeException("Move call failed: " + error + "\nOutput: " + output);
        }

        // Extract transaction digest
        String digest = rootNode.path("digest").asText();
        if (digest == null || digest.isEmpty()) {
            throw new RuntimeException("Transaction digest not found in response");
        }

        System.out.println("Move call stdout:\n" + output);
        System.out.println("Move call stderr:\n" + errorOutput);
        return digest;
    }


    public String publish(Path tempMoveDir) throws IOException, InterruptedException {
        Process publishProcess = new ProcessBuilder(
                "sui", "client", "publish",
                tempMoveDir.toString(),
                "--gas", gasObjectId,
                "--gas-budget", String.valueOf(50000000L),
                "--json"
        )
                .directory(tempMoveDir.toFile())
                .start();

        StringBuilder publishOutput = new StringBuilder();
        StringBuilder publishErrorOutput = new StringBuilder();
        try (BufferedReader stdOut = new BufferedReader(new InputStreamReader(publishProcess.getInputStream()));
             BufferedReader stdErr = new BufferedReader(new InputStreamReader(publishProcess.getErrorStream()))) {
            String line;
            while ((line = stdOut.readLine()) != null) {
                publishOutput.append(line).append("\n");
            }
            while ((line = stdErr.readLine()) != null) {
                publishErrorOutput.append(line).append("\n");
            }
        }
        publishProcess.waitFor(5, TimeUnit.MINUTES);
        System.out.println("Publish stdout:\n" + publishOutput);
        System.out.println("Publish stderr:\n" + publishErrorOutput);

        if (publishProcess.exitValue() != 0) {
            throw new RuntimeException("Publish failed with exit code " + publishProcess.exitValue() +
                    "\nError: " + publishErrorOutput);
        }

        // Step 10: Parse package ID from JSON output
        return parsePackageIdFromJson(publishOutput.toString());
    }

    private String parsePackageIdFromJson(String jsonOutput) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonOutput);
        JsonNode objectChanges = rootNode.path("objectChanges");
        for (JsonNode change : objectChanges) {
            if (change.path("type").asText().equals("published")) {
                return change.path("packageId").asText();
            }
        }
        throw new RuntimeException("No package ID found in publish output");
    }
    private String parseRegistryIdFromTransaction(String digest, String module, String packageId)
            throws IOException, InterruptedException {
        // 1. Build your JSON‑RPC request payload
        String payload = new ObjectMapper().writeValueAsString(Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "sui_getTransactionBlock",
                "params", List.of(digest, Map.of(
                        "showEffects", true,
                        "showObjectChanges", true
                ))
        ));

        // 2. Fire the HTTP POST (using Java 11+ HttpClient for example)
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(rpc))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("RPC call failed: " + resp.body());
        }

        // 3. Parse JSON
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(resp.body()).path("result");
        JsonNode changes = root.path("effects").path("created");
        System.out.println("Changes: " + changes);
        System.out.println("I got here");
        // 4. Find the created Registry
        for (JsonNode change : changes) {
            System.out.println("I got here again");

            return change.path("reference").path("objectId").asText();
        }
        throw new RuntimeException("Registry ID not found in transaction " + digest);
    }

    public static File [] buildMoveCode(Path tempMoveDir) throws IOException, InterruptedException {
        Process buildProcess = new ProcessBuilder("sui", "move", "build", "--path", tempMoveDir.toString())
                .directory(tempMoveDir.toFile())
                .start();
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader stdOut = new BufferedReader(new InputStreamReader(buildProcess.getInputStream()));
             BufferedReader stdErr = new BufferedReader(new InputStreamReader(buildProcess.getErrorStream()))) {
            String line;
            while ((line = stdOut.readLine()) != null) {
                output.append(line).append("\n");
            }
            while ((line = stdErr.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }
        buildProcess.waitFor(30, TimeUnit.SECONDS);
        System.out.println("Build stdout:\n" + output);
        System.out.println("Build stderr:\n" + errorOutput);
        if (buildProcess.exitValue() != 0) {
            throw new RuntimeException("Move build failed with exit code " + buildProcess.exitValue() + "\nError: " + errorOutput);
        }

        // Step 7: Verify bytecode directory
        Path bytecodeDir = tempMoveDir.resolve("build").resolve("test_package").resolve("bytecode_modules");
        File[] bytecodeFiles = bytecodeDir.toFile().listFiles();
        return bytecodeFiles;
    }


}