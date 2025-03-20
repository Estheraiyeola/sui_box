module sui_box::attribute{
    use std::string;
    use std::string::String;

    public enum Type has drop{
        STRING(String),
        INT(bool),
        BOOL(bool),
    }

    public struct Attribute has drop{
        name: String,
        value: Type,
    }


     public fun new(name: String): Attribute {
        let attribute = Attribute {
            name,
            value: Type::STRING(b"".to_string()),
        };
        attribute
    }

    //Setter for the value
    public fun set_value(attribute: &mut Attribute, value: Type){
        attribute.value = value
    }

    //Getter for the value
    public fun get_value(attribute: &Attribute): &Type{
        &attribute.value
    }


    #[test]
    fun test_set_attribute(){
        let name = b"email".to_string();
        let value = Type::STRING(b"example@example.com".to_string());

        let mut attribute = new(name);

        attribute.set_value(value);

        assert!(attribute.get_value() == Type::STRING(b"example@example.com".to_string()), 0)

        
    }
}