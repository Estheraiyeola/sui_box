#[allow(unused_use, duplicate_alias)]
module ${module}::${struct} {
    use sui::object::{Self, UID};
    use sui::transfer;
    use std::string::String;
    use std::option::{Self, Option};

    public struct ${struct} has key, store, copy {
        id: UID,
        <#list fields as field>
            ${field.name}: ${field.moveType},
        </#list>
    }

    public entry fun create_${struct?lower_case}(
    <#list fields as field>
        ${field.name}: ${field.moveType},
    </#list>
    ctx: &mut TxContext
    ) {
        let _obj = ${struct} {
            id: object::new(ctx),
            <#list fields as field>
                ${field.name},
            </#list>
        };
    }


}