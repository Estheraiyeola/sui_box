#[allow(unused_use, duplicate_alias, lint(custom_state_change))]
module ${module}::${struct} {
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
    public struct ${struct} has key, store {
    id: UID,
    is_deleted: bool,
    <#list fields as field>
        ${field.name}: ${field.moveType}<#if field_has_next>,</#if>
    </#list>
    }

    // Create a new instance and register its ID
    public fun create(<#list fields as field>${field.name}: ${field.moveType}<#if field_has_next>, </#if></#list>, registry: &mut Registry, ctx: &mut TxContext): ${struct} {
    let obj = ${struct} {
    id: object::new(ctx),
    is_deleted: false,
    <#list fields as field>
        ${field.name}<#if field_has_next>,</#if>
    </#list>
    };
    let id = object::uid_to_inner(&obj.id);
    vector::push_back(&mut registry.items, id);
    obj
    }

    // Update the object’s fields by ID (caller must provide the object)
    public entry fun update_by_id(obj: &mut ${struct}, <#list fields as field>new_${field.name}: ${field.moveType}<#if field_has_next>, </#if></#list>) {
    assert!(!obj.is_deleted, 0); // Cannot update a deleted object
    <#list fields as field>
        obj.${field.name} = new_${field.name};
    </#list>
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
        public entry fun delete(obj: &mut ${struct}) {
        obj.is_deleted = true;
        }
        }