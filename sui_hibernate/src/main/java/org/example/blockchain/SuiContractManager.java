package org.example.blockchain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sui.Sui;
import io.sui.models.SuiApiException;
import io.sui.models.objects.ObjectChange;
import io.sui.models.transactions.*;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

    public CompletableFuture<TransactionBlockResponse> executeMoveCall(
            String pkg, String module, String fn, List<Object> args) {
        TransactionBlockResponseOptions options = new TransactionBlockResponseOptions();
        options.setShowEffects(true);
        options.setShowEvents(true);
        options.setShowInput(true);
        options.setShowObjectChanges(true);

        return suiClient.moveCall(
                senderAddress, pkg, module, fn,
                Collections.emptyList(),
                args, null, 3000000L, null, null,
                options, ExecuteTransactionRequestType.WaitForLocalExecution
        );
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
        String packageId = parsePackageIdFromJson(publishOutput.toString());
        return packageId;
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