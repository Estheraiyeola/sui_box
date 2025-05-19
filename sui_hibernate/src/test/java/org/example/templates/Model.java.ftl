package ${package};

import java.util.List;
import org.example.blockchain.SuiContractManager;

/**
* Auto‑generated bridge for on‑chain struct `${className}`.
*/
public class ${className} {
private final String objectId;
private final SuiContractManager mgr;

<#list fields as field>
    private ${field.javaType} ${field.name};
</#list>

private ${className}(String objectId, SuiContractManager mgr) {
this.objectId = objectId;
this.mgr      = mgr;
}

<#-- standard getters/setters -->
<#list fields as field>
    public ${field.javaType} get${field.name?cap_first}() { return this.${field.name}; }
    public void set${field.name?cap_first}(${field.javaType} v) { this.${field.name} = v; }
</#list>

public String getObjectId() { return objectId; }

/**
* Calls the Move constructor, returning the new object’s ID.
*/
public static String create(
SuiContractManager mgr,
<#list fields as field>
    ${field.javaType} ${field.name}<#if field_has_next>,</#if>
</#list>
) throws Exception {
// module name is passed in via the template context
return mgr.moveCall(
"${module}",      // ← your Move module
"new",            // ← constructor function name
List.of(
<#list fields as field>
    ${field.name}<#if field_has_next>,</#if>
</#list>
),
/* workingDir */  null,
/* packageId  */  null,
/* assignAndTransfer */ false,
/* transferToAddress  */ null
);
}

// (you can add overloads that accept workingDir/packageId/transfer if you like…)

// reflection registry, etc...
}
