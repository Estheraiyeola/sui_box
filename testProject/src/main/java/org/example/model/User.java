package org.example.model;

import org.example.annotation.BlockchainEntity;

@BlockchainEntity(module = "User", struct = "User")
public class User {
    public String name;
    public long age;
    public String email;
}
