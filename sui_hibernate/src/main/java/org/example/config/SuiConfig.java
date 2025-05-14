package org.example.config;

import io.github.cdimascio.dotenv.Dotenv;

public class SuiConfig {
    private static final Dotenv ENV = Dotenv.configure().directory("src/.env").load();
    public static final String SENDER_ADDRESS = ENV.get("SUI_SENDER_ADDRESS");
    public static final String GAS_OBJECT = ENV.get("SUI_GAS_OBJECT");
    public static final String FULLNODE_URL = ENV.get("SUI_FULLNODE_URL");
    public static final String FAUCET_URL = ENV.get("SUI_FAUCET_URL");
    public static final String KEYSTORE_PATH = ENV.get("SUI_KEYSTORE_PATH");
}