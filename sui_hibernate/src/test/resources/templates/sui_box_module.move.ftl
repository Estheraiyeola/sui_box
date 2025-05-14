#[allow(unused_use, duplicate_alias, lint(custom_state_change))]
module ${module}::${struct} {
    use sui::object::{Self, UID};
    use sui::tx_context::{Self, TxContext};
    use sui::transfer;
    use std::string::{Self, String};
    use std::vector;
    use std::option::{Self, Option};

    public struct ${struct} has key, store {
        id: UID,
        <#list fields as field>
            ${field.name}: ${field.moveType}<#if field_has_next>,</#if>
        </#list>
    }

    public fun create(<#list fields as field>${field.name}: ${field.moveType}<#if field_has_next>, </#if></#list>, ctx: &mut TxContext): ${struct} {
        let obj = ${struct} {
        id: object::new(ctx),
        <#list fields as field>
            ${field.name}<#if field_has_next>,</#if>
        </#list>
        };
        obj
    }
}