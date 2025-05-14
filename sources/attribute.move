module sui_box::attribute {
    use std::string::String;

    // Define possible types for attribute values
    public enum Type has store, drop, copy {
        STRING(String),
        INT(u64),
        BOOL(bool),
    }

    // Define possible type names for attributes
    public enum TypeName has store, drop, copy {
        STRING,
        INT,
        BOOL,
    }

    // Structure representing an attribute with a name, value, and type
    public struct Attribute has store, drop, copy {
        name: String,
        value: Type,
        types: TypeName,
    }

    // Create a new attribute
    public fun new_attribute(name: String, value: Type, types: TypeName): Attribute {
        Attribute { name, value, types }
    }

    // Set the value of an attribute
    public fun set_value(attribute: &mut Attribute, value: Type) {
        attribute.value = value;
    }

    // Get the value of an attribute
    public fun get_value(attribute: &Attribute): &Type {
        &attribute.value
    }

    // Set the name of an attribute
    public fun set_name(attribute: &mut Attribute, name: String) {
        attribute.name = name;
    }

    // Get the name of an attribute
    public fun get_name(attribute: &Attribute): String {
        attribute.name
    }

    // Helper function: returns the type name for STRING
    public fun string_type_name(): TypeName {
        TypeName::STRING
    }

    // Helper function: returns the type name for INT
    public fun int_type_name(): TypeName {
        TypeName::INT
    }

    // Helper function: returns the type name for BOOL
    public fun bool_type_name(): TypeName {
        TypeName::BOOL
    }

    // Helper function: creates a STRING type value
    public fun string_type(value: String): Type {
        Type::STRING(value)
    }

    // Helper function: creates an INT type value
    public fun int_type(value: u64): Type {
        Type::INT(value)
    }

    // Helper function: creates a BOOL type value
    public fun bool_type(value: bool): Type {
        Type::BOOL(value)
    }

    // Equality function for Type values.
    // We dereference t1 and t2 to obtain the actual values.
    public fun type_equal(t1: &Type, t2: &Type): bool {
        let v1 = *t1;
        match (v1) {
            Type::STRING(s1) => {
                match (*t2) {
                    Type::STRING(s2) => s1 == s2,
                    _ => false,
                }
            },
            Type::INT(i1) => {
                match (*t2) {
                    Type::INT(i2) => i1 == i2,
                    _ => false,
                }
            },
            Type::BOOL(b1) => {
                match (*t2){
                    Type::BOOL(b2) => b1 == b2,
                    _ => false,
                }
            },
        }
    }


    #[test]
    fun test_set_attribute() {
        let name = b"email".to_string();
        let value = string_type(b"example@example.com".to_string());
        let attribute = new_attribute(name, value, string_type_name());
        let expected = string_type(b"example@example.com".to_string());
        assert!(type_equal(get_value(&attribute), &expected), 0);
    }
}
