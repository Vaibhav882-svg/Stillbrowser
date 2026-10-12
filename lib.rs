use tauri::{plugin::{Builder, TauriPlugin}, Runtime};

#[cfg(target_os = "android")]
mod mobile;

pub fn init<R: Runtime>() -> TauriPlugin<R> {
    let builder = Builder::new("browser");
    #[cfg(target_os = "android")]
    let builder = builder.setup(|_app, api| {
        api.register_android_plugin("com.stillbrowser.app", "BrowserPlugin")?;
        Ok(())
    });
    builder
        .invoke_handler(tauri::generate_handler![])
        .build()
}
