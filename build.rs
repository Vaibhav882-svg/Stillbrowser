const COMMANDS: &[&str] = &["open", "home", "back", "forward", "reload"];

fn main() {
    tauri_plugin::Builder::new(COMMANDS)
        .android_path("android")
        .build();
}
