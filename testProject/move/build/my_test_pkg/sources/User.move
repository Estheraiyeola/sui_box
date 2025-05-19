#[allow(unused_use, duplicate_alias, lint(custom_state_change))]
module User::User {
use sui::object::{Self, UID, ID};
use sui::tx_context::{Self, TxContext};
use sui::transfer;
use std::string::{Self, String};
use std::vector;
use std::option::{Self, Option};

// Registry to track all created objects by their IDs
public struct Registry has key {
id: UID,
items: vector<ID>,
    }

    // Function to create and share the registry
    public entry fun create_registry(ctx: &mut TxContext) {
    let registry = Registry {
    id: object::new(ctx),
    items: vector::empty(),
    };
    transfer::share_object(registry);
    }

    // Struct with the additional is_deleted field
    public struct User has key, store {
    id: UID,
    is_deleted: bool,
        name: String,
        age: u64,
        email: String
    }

    // Create a new instance and register its ID
    public fun create(name: String, age: u64, email: String, registry: &mut Registry, ctx: &mut TxContext): User {
    let obj = User {
    id: object::new(ctx),
    is_deleted: false,
        name,
        age,
        email
    };
    let id = object::uid_to_inner(&obj.id);
    vector::push_back(&mut registry.items, id);
    obj
    }

    // Update the object’s fields by ID (caller must provide the object)
    public entry fun update_by_id(obj: &mut User, new_name: String, new_age: u64, new_email: String) {
    assert!(!obj.is_deleted, 0); // Cannot update a deleted object
        obj.name = new_name;
        obj.age = new_age;
        obj.email = new_email;
    }

    // Check if an ID exists in the registry
    public fun find_by_id(registry: &Registry, id: ID): bool {
    vector::contains(&registry.items, &id)
    }

    // Get all registered IDs
    public fun get_all(registry: &Registry): vector<ID> {
        registry.items
        }

        // Mark the object as deleted
        public entry fun delete(obj: &mut User) {
        obj.is_deleted = true;
        }
        }