package org.example;

import io.sui.Sui;
import org.example.blockchain.SuiContractManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.io.File;

import static org.example.blockchain.SuiContractManager.buildMoveCode;

public class Main {
    public static void main(String[] args) throws Exception {
        // ─── 0) Your Sui config ───────────────────────────────────────────────
        String sender    = "0xd5c008023bdda07b8543b2b654545dfad2e1be67b72f4b749f6d680e5245f90d";
        String gasObj    = "0x110be019b23e4008400a315d5de9414d5492df9b576d6022033c430b68fa406f";
        String fullnode  = "https://rpc.testnet.sui.io:443";
        String faucet    = "https://faucet.testnet.sui.io:443";
        String keystore  = "/home/essiecodes/.sui/sui_config/sui.keystore";

        Sui sui = new Sui(fullnode, faucet, keystore);
        SuiContractManager mgr =
                new SuiContractManager(sui, sender, gasObj, 50_000_000L, 1000L);

        // ─── 1) Point at your Move project root ───────────────────────────────
        Path projectRoot = Paths.get("/home/essiecodes/sui_box/testProject/move");
        // sanity check: make sure Move.toml and sources/ exist
        if (!Files.exists(projectRoot.resolve("Move.toml")) ||
                !Files.isDirectory(projectRoot.resolve("sources"))) {
            throw new IllegalStateException(
                    "move/Move.toml or move/sources/ not found—please create them as shown above");
        }

        // ─── 2) Compile your .move sources to bytecode ────────────────────────
        File[] bytecode = buildMoveCode(projectRoot);
//
        // ─── 3) Publish the package ───────────────────────────────────────────
        String pkgId = mgr.publish(projectRoot);
        System.out.println("✅ Deployed package ID: " + pkgId);

        // ─── 4) Create your Registry (module is “User”) ────────────────────────
        String registryId = mgr.createRegistry("User", projectRoot, pkgId);
        System.out.println("✅ Registry object:   " + registryId);

        // ─── 5) Call your entry function (`User::create`) ────────────────────
        String digest = mgr.moveCall(
                "User",            // module name
                "create",          // function name
                List.of("Alice", 42, "alice.com", registryId),
                projectRoot,
                pkgId,
                true,              // assign & transfer
                sender             // transfer receiver
        );
        System.out.println("✅ Tx digest:         " + digest);
    }
}
