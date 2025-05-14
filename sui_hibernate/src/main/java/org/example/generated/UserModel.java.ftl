package ${pkg};

import io.sui.models.transactions.TransactionBlockResponse;
import java.util.concurrent.CompletableFuture;

/**
* Auto-generated bridge for on-chain struct `${struct}`.
*/
public class ${struct}Model {
private final String objectId;
private final SuiContractManager mgr;
private ${struct}Model(String objectId, SuiContractManager mgr) {
this.objectId = objectId; this.mgr = mgr;
}

<#list fields?split("\n") as f>
    private ${f?replace(";", "")};
    public ${f?replace(";", "")} get${f?capitalize}() { /* parse via query */ return null; }
</#list>

public static CompletableFuture<${struct}Model> create(
    SuiContractManager mgr,
    <#list fields?split("\n") as f>${f?replace(":",":")?replace(",", "")}${f?has_next?", ":""}</#list>
    ) {
    return mgr.executeMoveCall(
    mgr.getPackageId(),
    "${struct}Module",
    "new",
    List.of(<#list fields?split("\n") as f>${f?substring(0,f?index_of(":"))}${f?has_next?", ":""}</#list>)
    ).thenApply(resp -> {
    String id = resp.getEffects().getCreated()[0].getObjectId();
    return new ${struct}Model(id, mgr);
    });
    }

    public CompletableFuture<Void> updateName(String newName, String caller) {
        return mgr.executeMoveCall(
        mgr.getPackageId(),
        "${struct}Module",
        "update_name_with_event",
        List.of(objectId, newName, caller)
        ).thenApply(_r->null);
        }
        // ... more methods ...
        }
