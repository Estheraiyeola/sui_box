module sui_box::registry {
    use sui_box::sui_box_module::{Model, filter_models_by_attribute as fmba, get_name};
    use std::vector;
    use std::string::String;

    const E_MODEL_ALREADY_EXISTS: u64 = 0;

    /// Registry resource that stores a vector of Model.
    public struct Registry has store, drop {
        models: vector<Model>,
    }

    /// Initializes a new empty registry.
    public fun init_registry(): Registry {
        Registry { models: vector::empty<Model>() }
    }

    /// Adds a model to the registry.
    public fun add_model(reg: &mut Registry, model: Model) {
        validate(&model, reg);
        vector::push_back(&mut reg.models, model)
    }

    public fun validate(model: &Model, reg: &Registry) {
        let len = vector::length(&reg.models);
        let mut i = 0;
        while (i < len) {
            assert!(get_name(model) != get_name(&reg.models[i]), E_MODEL_ALREADY_EXISTS);
            i = i + 1;
        }
    }

    /// Removes a model by its index position.
    /// Returns true if removed; false if index out of bounds.
    public fun remove_model(reg: &mut Registry, index: u64): bool {
        let len = vector::length(&reg.models);
        if (index >= len) {
            return false
        };
        vector::remove(&mut reg.models, index);
        true
    }

    /// Retrieves all models stored in the registry.
    public fun get_all_models(reg: &Registry): &vector<Model> {
        &reg.models
    }

    /// Filters the models in the registry by the existence of a specific attribute.
    public fun filter_models_by_attribute(reg: &Registry, attr_name: String): vector<Model> {
        let mut results = vector::empty<Model>();
        let mut i = 0;
        let len = vector::length(&reg.models);
        while (i < len) {
            let model = &reg.models[i];
            let mut temp = vector::empty<Model>();
            vector::push_back(&mut temp, *model);
            let filtered = sui_box::sui_box_module::filter_models_by_attribute(temp, attr_name);
            if (vector::length(&filtered) > 0) {
                vector::push_back(&mut results, *model);
            };
            i = i + 1;
        };
        results
    }

    // --- Tests for the Registry Module ---
    
    #[test]
    public fun test_registry_add_and_get_all() {
        let mut reg = init_registry();
        let dummy_model: Model = 
            sui_box::sui_box_module::new(b"TestModel".to_string(), vector::empty(), &mut sui_box::sui_box_module::init_counter(), @0x1);
        add_model(&mut reg, dummy_model);
        let all_models = get_all_models(&reg);
        assert!(vector::length(all_models) == 1, 0);
    }

    #[test]
    public fun test_registry_filter_models_by_attribute() {
        let mut reg = init_registry();
        let mut counter = sui_box::sui_box_module::init_counter();
        let owner: address = @0x1;

        let attr_age = sui_box::attribute::new_attribute(
            b"age".to_string(),
            sui_box::attribute::int_type(30),
            sui_box::attribute::int_type_name()
        );
        let attr_name = sui_box::attribute::new_attribute(
            b"name".to_string(),
            sui_box::attribute::string_type(b"Alice".to_string()),
            sui_box::attribute::string_type_name()
        );

        let mut attrs1 = vector::empty<sui_box::attribute::Attribute>();
        vector::push_back(&mut attrs1, attr_age);
        let model1 = sui_box::sui_box_module::new(b"Person1".to_string(), attrs1, &mut counter, owner);

        let mut attrs2 = vector::empty<sui_box::attribute::Attribute>();
        vector::push_back(&mut attrs2, attr_name);
        let model2 = sui_box::sui_box_module::new(b"Person2".to_string(), attrs2, &mut counter, owner);

        add_model(&mut reg, model1);
        add_model(&mut reg, model2);

        let filtered = filter_models_by_attribute(&reg, b"age".to_string());
        assert!(vector::length(&filtered) == 1, 0);
    }

    #[test]
    public fun test_registry_remove_model() {
        let mut reg = init_registry();
        let mut counter = sui_box::sui_box_module::init_counter();
        let owner: address = @0x1;

        let model = sui_box::sui_box_module::new(b"TestModel".to_string(), vector::empty(), &mut counter, owner);
        add_model(&mut reg, model);
        let removed = remove_model(&mut reg, 0); // Changed to use index 0
        assert!(removed, 0);
        let all_models = get_all_models(&reg);
        assert!(vector::length(all_models) == 0, 0);
    }
}