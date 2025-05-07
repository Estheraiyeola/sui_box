module ${package}::${struct}Module {
use std::string::String;
use sui_box::attribute::{new_attribute, Attribute, string_type, int_type, string_type_name, int_type_name};

public struct ${struct} has store, drop, copy {
<#list fields?split("\n") as f>
    ${f}
</#list>
}

public fun new(<#list fields?split("\n") as f>${f?replace(":",":")?replace(",", "")}${f?has_next?", ":""}</#list>): ${struct} {
// increment counter, validate, etc…
${struct} { <#list fields?split("\n") as f>${f?substring(0,f?index_of(":"))}${f?has_next?", ":""}</#list> }
}

// emit create/update/delete events, getters…
}
