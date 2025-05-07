# Sui Box Smart Contracts

This project contains Move smart contracts for managing entities, modeled as "Models" with a set of "Attributes." This module simulates a basic database registry on-chain, complete with features such as creating, updating, deleting models, filtering models, and emitting events. 

## Modules

### sui_box::attribute
- **Type**: Represents possible attribute values (STRING, INT, BOOL).
- **Attribute**: Stores the name, value, and type of an attribute.
- Provides helper functions for creating and comparing attribute values.

### sui_box::sui_box_module
- **Model**: Represents an entity with an id, name, attributes, version, deletion flag, and owner.
- Functions for creating a new model, updating its name, attributes, querying attributes, and marking it as deleted.
- Event emission functions for logging updates and deletions.

### sui_box::registry
- Acts as an on-chain registry (database) for models.
- Functions for adding a model, removing a model, retrieving all models, and filtering models by an attribute.

## Deployment

1. **Setup Development Environment:**
   - Install [Rust](https://www.rust-lang.org/tools/install).
   - Install the Sui CLI as per the [Sui documentation](https://docs.sui.io/).
   - Clone this repository to your local machine.

2. **Build and Test:**
   - Navigate to the repository root.
   - Run:
     ```
     sui move test
     ```
     to run all tests. Ensure that tests pass before deployment.

3. **Deploy Contracts:**
   - Use the Sui CLI to deploy the Move package:
     ```
     sui client publish --gas-budget <budget>
     ```
   - Note the package ID returned by the publish command.

## Interacting with the Contracts

Using the Sui CLI or Sui4j (Java SDK), you can:
- **Create a Model:** Call the `new` function, passing required parameters (name, attributes, counter, owner).
- **Update a Model:** Use functions such as `update_name_with_event` and `update_attribute_with_event`. These enforce access control based on the model's owner.
- **Delete a Model:** Mark a model as deleted using `delete_model_with_event`.
- **Query a Model:** Retrieve attributes using `query_attribute`, and filter models using the registry functions.

## Events
- When models are updated or deleted, events (`ModelUpdatedEvent` and `ModelDeletedEvent`) are emitted.
- These events can be indexed off-chain for auditability and further processing.

## Future Enhancements
- Implement additional query functions.
- Extend access control with role-based authorization.
- Integrate full Sui event handling (once the official API is available).

## Documentation
Inline comments and doc comments are provided in the source code. For further questions, please consult the [Sui documentation](https://docs.sui.io/).

# sui_box
