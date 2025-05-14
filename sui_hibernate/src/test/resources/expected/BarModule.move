module foo::BarModule {
    use std::string::String;

    public struct Bar has store, drop, copy {
        name: String,
        count: u64,
    }

    public fun new(name: String, count: u64): Bar {
        Bar { name, count }
    }

    public fun get_name(bar: &Bar): &String {
        &bar.name
    }

    public fun get_count(bar: &Bar): u64 {
        bar.count
    }

    public fun set_name(bar: &mut Bar, new_name: String) {
        bar.name = new_name;
    }

    public fun set_count(bar: &mut Bar, new_count: u64) {
        bar.count = new_count;
    }
}
