module sui_box::sui_box_module {
    use std::string::String;
    use sui_box::attribute::{
        new_attribute, Attribute, string_type_name, int_type_name, string_type, int_type,
        get_name as get_attribute_name, get_value, set_value
    };
    use sui::event;

    // Error codes
    const E_ATTRIBUTE_ALREADY_EXISTS: u64 = 1;
    const E_ATTRIBUTE_NOT_FOUND: u64 = 2;
    const E_UNAUTHORIZED: u64 = 3;

    // Counter struct to manage unique IDs
    public struct Counter has store, drop {
        count: u64,
    }

    // Initialize a new counter
    public fun init_counter(): Counter {
        Counter { count: 0 }
    }

    // Increment the counter's count
    public fun increment_count(counter: &mut Counter) {
        counter.count = counter.count + 1
    }

    // Model struct representing an entity with attributes.
    // Added "version" for updates, "owner" for access control, and a deletion flag.
    public struct Model has store, drop, copy {
        id: u64,
        name: String,
        attributes: vector<Attribute>,
        version: u64,
        is_deleted: bool,
        owner: address,
    }

    // Create a new Model instance
    public fun new(name: String, attributes: vector<Attribute>, counter: &mut Counter, owner: address): Model {
        increment_count(counter);
        validate_attributes(attributes);
        Model {
            id: counter.count,
            name,
            attributes,
            version: 1,
            is_deleted: false,
            owner,
        }
    }
    // Get the name of a model
    public fun get_name(model: &Model): String {
        model.name
    }

    // Validate that all attribute names are unique
    public fun validate_attributes(attributes: vector<Attribute>) {
        let mut attribute_names = vector::empty<String>();
        let mut i = 0;
        while (i < vector::length(&attributes)) {
            let attribute = &attributes[i];
            let name = get_attribute_name(attribute);
            if (vector::contains(&attribute_names, &name)) {
                abort E_ATTRIBUTE_ALREADY_EXISTS
            };
            vector::push_back(&mut attribute_names, name);
            i = i + 1;
        }
    }

    // Update the name of a model and increment the version.
    // Caller must match model.owner.
    public fun update_name(model: &mut Model, new_name: String, caller: address) {
        if (caller != model.owner) {
            abort E_UNAUTHORIZED
        };
        model.name = new_name;
        model.version = model.version + 1
    }

    // Update an attribute's value by its name.
    public fun update_attribute(model: &mut Model, attr_name: String, new_value: sui_box::attribute::Type, caller: address) {
        if (caller != model.owner) {
            abort E_UNAUTHORIZED
        };
        let mut i = 0;
        let mut found = false;
        while (i < vector::length(&model.attributes)) {
            let attribute = &mut model.attributes[i];
            if (get_attribute_name(attribute) == attr_name) {
                set_value(attribute, new_value);
                found = true;
                break
            };
            i = i + 1;
        };
        if (!found) {
            abort E_ATTRIBUTE_NOT_FOUND
        };
        model.version = model.version + 1
    }

    // Query an attribute value by its name.
    public fun query_attribute(model: &Model, attr_name: String): &sui_box::attribute::Type {
        let mut i = 0;
        while (i < vector::length(&model.attributes)) {
            let attribute = &model.attributes[i];
            if (get_attribute_name(attribute) == attr_name) {
                return get_value(attribute)
            };
            i = i + 1;
        };
        abort E_ATTRIBUTE_NOT_FOUND
    }

    // "Delete" a model by marking it as deleted.
    public fun delete_model(model: &mut Model, caller: address) {
        if (caller != model.owner) {
            abort E_UNAUTHORIZED
        };
        model.is_deleted = true;
        model.version = model.version + 1
    }
        
    // --- Events ---
    // Define event structs to track when a model is updated or deleted.
    public struct ModelUpdatedEvent has store, drop, copy {
        id: u64,
        new_version: u64,
        update_type: String,   // e.g., "update_name" or "update_attribute"
    }
    public struct ModelDeletedEvent has store, drop, copy {
        id: u64,
        new_version: u64,
    }

    // Instead of a generic emit_event, we define specialized functions that
    // call sui::event::emit with a type defined in this module.
    public fun emit_model_updated_event(event: ModelUpdatedEvent) {
        event::emit<ModelUpdatedEvent>(event)
    }

    public fun emit_model_deleted_event(event: ModelDeletedEvent) {
        event::emit<ModelDeletedEvent>(event)
    }

    // Update functions that emit events:
    public fun update_name_with_event(model: &mut Model, new_name: String, caller: address) {
        update_name(model, new_name, caller);
        let event = ModelUpdatedEvent {
            id: model.id,
            new_version: model.version,
            update_type: b"update_name".to_string()
        };
        emit_model_updated_event(event)
    }

    public fun update_attribute_with_event(model: &mut Model, attr_name: String, new_value: sui_box::attribute::Type, caller: address) {
        update_attribute(model, attr_name, new_value, caller);
        let event = ModelUpdatedEvent {
            id: model.id,
            new_version: model.version,
            update_type: b"update_attribute".to_string()
        };
        emit_model_updated_event(event)
    }

    public fun delete_model_with_event(model: &mut Model, caller: address) {
        delete_model(model, caller);
        let event = ModelDeletedEvent {
            id: model.id,
            new_version: model.version,
        };
        emit_model_deleted_event(event)
    }

    // --- Additional Queries ---
    // Example: Filter models by the existence of a specific attribute.
    // In a real system, models would reside in a global registry.
    public fun filter_models_by_attribute(models: vector<Model>, attr_name: String): vector<Model> {
        let mut results = vector::empty<Model>();
        let mut i = 0;
        while (i < vector::length(&models)) {
            let model = &models[i];
            let mut j = 0;
            let mut found = false;
            while (j < vector::length(&model.attributes)) {
                if (get_attribute_name(&model.attributes[j]) == attr_name) {
                    found = true;
                    break
                };
                j = j + 1;
            };
            if (found) {
                vector::push_back(&mut results, *model);
            };
            i = i + 1;
        };
        results
    }

    // --- Tests ---
    #[test]
    public fun test_that_a_model_can_be_created() {
        let mut counter = init_counter();
        let owner: address = @0x1;

        let animals_name = b"Dog".to_string();
        let age: u64 = 14;

        let attribute1 = new_attribute(
            b"animals_name".to_string(),
            string_type(animals_name),
            string_type_name()
        );
        let attribute2 = new_attribute(
            b"age".to_string(),
            int_type(age),
            int_type_name()
        );
        let mut attributes = vector::empty<Attribute>();
        vector::push_back(&mut attributes, attribute1);
        vector::push_back(&mut attributes, attribute2);
        let model = new(b"Animals".to_string(), attributes, &mut counter, owner);

        assert!(model.name == b"Animals".to_string(), 0);
        assert!(vector::length(&model.attributes) == 2, 0);
        assert!(get_attribute_name(&model.attributes[0]) == b"animals_name".to_string(), 0);
        assert!(get_value(&model.attributes[0]) == string_type(b"Dog".to_string()), 0);
    }

    #[test]
    public fun test_update_model() {
        let mut counter = init_counter();
        let owner: address = @0x1;

        let animals_name = b"Dog".to_string();
        let age: u64 = 14;

        let attribute1 = new_attribute(
            b"animals_name".to_string(),
            string_type(animals_name),
            string_type_name()
        );
        let attribute2 = new_attribute(
            b"age".to_string(),
            int_type(age),
            int_type_name()
        );
        let mut attributes = vector::empty<Attribute>();
        vector::push_back(&mut attributes, attribute1);
        vector::push_back(&mut attributes, attribute2);
        let mut model = new(b"Animals".to_string(), attributes, &mut counter, owner);

        // Update model name using update with event
        update_name_with_event(&mut model, b"Wild Animals".to_string(), owner);
        assert!(model.name == b"Wild Animals".to_string(), 0);

        // Update attribute "age" using update with event
        update_attribute_with_event(&mut model, b"age".to_string(), int_type(15), owner);
        let _new_age = query_attribute(&model, b"age".to_string());
        // Use a helper function to assert _new_age equals int_type(15) if needed

        // Delete model with event
        delete_model_with_event(&mut model, owner);
        assert!(model.is_deleted, 0);
    }

    #[test, expected_failure(abort_code = E_ATTRIBUTE_ALREADY_EXISTS)]
    public fun test_that_a_model_cannot_have_attributes_with_the_same_name() {
        let mut counter = init_counter();
        let owner: address = @0x1;

        let animals_name = b"Dog".to_string();
        let age: u64 = 14;

        let attribute1 = new_attribute(
            b"animals_name".to_string(),
            string_type(animals_name),
            string_type_name()
        );
        let attribute2 = new_attribute(
            b"animals_name".to_string(),
            int_type(age),
            int_type_name()
        );
        let mut attributes = vector::empty<Attribute>();
        vector::push_back(&mut attributes, attribute1);
        vector::push_back(&mut attributes, attribute2);
        let _model = new(b"Animals".to_string(), attributes, &mut counter, owner);
    }

     // Test: Attempt to update a non-existent attribute.
    #[test, expected_failure(abort_code = E_ATTRIBUTE_NOT_FOUND)]
    public fun test_update_nonexistent_attribute() {
        let mut counter = init_counter();
        let owner: address = @0x1;

        // Create model with one attribute "animals_name"
        let animals_name = b"Dog".to_string();
        let attribute1 = new_attribute(
            b"animals_name".to_string(),
            string_type(animals_name),
            string_type_name()
        );
        let mut attributes = vector::empty<Attribute>();
        vector::push_back(&mut attributes, attribute1);
        let mut model = new(b"Animals".to_string(), attributes, &mut counter, owner);

        // Try updating a non-existent attribute "age"
        update_attribute_with_event(&mut model, b"age".to_string(), int_type(15), owner);
    }

    // Test: Attempt unauthorized update of model name.
    #[test, expected_failure(abort_code = E_UNAUTHORIZED)]
    public fun test_update_name_unauthorized() {
        let mut counter = init_counter();
        let owner: address = @0x1;
        let unauthorized: address = @0x2;

        let animals_name = b"Dog".to_string();
        let attribute1 = new_attribute(
            b"animals_name".to_string(),
            string_type(animals_name),
            string_type_name()
        );
        let mut attributes = vector::empty<Attribute>();
        vector::push_back(&mut attributes, attribute1);
        let mut model = new(b"Animals".to_string(), attributes, &mut counter, owner);

        // Attempt to update the model name with an unauthorized caller.
        update_name_with_event(&mut model, b"Unauthorized Update".to_string(), unauthorized);
    }

    // Test: Attempt unauthorized deletion of a model.
    #[test, expected_failure(abort_code = E_UNAUTHORIZED)]
    public fun test_delete_model_unauthorized() {
        let mut counter = init_counter();
        let owner: address = @0x1;
        let unauthorized: address = @0x2;

        let animals_name = b"Dog".to_string();
        let attribute1 = new_attribute(
            b"animals_name".to_string(),
            string_type(animals_name),
            string_type_name()
        );
        let mut attributes = vector::empty<Attribute>();
        vector::push_back(&mut attributes, attribute1);
        let mut model = new(b"Animals".to_string(), attributes, &mut counter, owner);

        // Attempt to delete the model with an unauthorized caller.
        delete_model_with_event(&mut model, unauthorized);
    }

    // Test: Ensure a model cannot have duplicate attribute names.
    #[test, expected_failure(abort_code = E_ATTRIBUTE_ALREADY_EXISTS)]
    public fun test_duplicate_attributes() {
        let mut counter = init_counter();
        let owner: address = @0x1;

        let animals_name = b"Dog".to_string();
        let age: u64 = 14;

        let attribute1 = new_attribute(
            b"animals_name".to_string(),
            string_type(animals_name),
            string_type_name()
        );
        let attribute2 = new_attribute(
            b"animals_name".to_string(),
            int_type(age),
            int_type_name()
        );
        let mut attributes = vector::empty<Attribute>();
        vector::push_back(&mut attributes, attribute1);
        vector::push_back(&mut attributes, attribute2);
        let _model = new(b"Animals".to_string(), attributes, &mut counter, owner);
    }
}
