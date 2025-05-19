package org.example.model.templates;

import java.util.List;
import org.example.blockchain.SuiContractManager;

/**
* Auto‑generated bridge for on‑chain struct `User`.
*/
public class User {
private final String objectId;
private final SuiContractManager mgr;

    private java.lang.String name;
    private long age;
    private java.lang.String email;

private User(String objectId, SuiContractManager mgr) {
this.objectId = objectId;
this.mgr      = mgr;
}

    public java.lang.String getName() { return this.name; }
    public void setName(java.lang.String v) { this.name = v; }
    public long getAge() { return this.age; }
    public void setAge(long v) { this.age = v; }
    public java.lang.String getEmail() { return this.email; }
    public void setEmail(java.lang.String v) { this.email = v; }

public String getObjectId() { return objectId; }

/**
* Calls the Move constructor, returning the new object’s ID.
*/
public static String create(
SuiContractManager mgr,
    java.lang.String name,
    long age,
    java.lang.String email
) throws Exception {
// module name is passed in via the template context
return mgr.moveCall(
"User",      // ← your Move module
"new",            // ← constructor function name
List.of(
    name,
    age,
    email
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
