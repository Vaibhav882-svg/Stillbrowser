import { invoke } from '@tauri-apps/api/core';
const $ = id => document.getElementById(id);
const address = $('address');
const backButton = $('back');
const forwardButton = $('forward');
let hasOpenedDestination = false;

function normalize(input) {
  const value = input.trim();
  if (!value) return '';
  if (/^https?:\/\//i.test(value)) return value;
  if (/^[\w.-]+\.[a-z]{2,}(:\d+)?([/?#].*)?$/i.test(value) && !/\s/.test(value)) return `https://${value}`;
  return `https://www.google.com/search?q=${encodeURIComponent(value)}`;
}
function toast(message) {
  const el = $('toast'); el.textContent = message; el.classList.add('show');
  clearTimeout(toast.timeout); toast.timeout = setTimeout(() => el.classList.remove('show'), 2400);
}
async function openDestination(raw) {
  const url = normalize(raw);
  if (!url) return;
  address.value = url;
  try {
    // Android: open the URL in the native WebView overlay, not an external browser.
    await invoke('plugin:browser|open', { url });
    hasOpenedDestination = true;
    backButton.disabled = false;
    forwardButton.disabled = false;
    toast('Opening inside Still Browser.');
  } catch (nativeError) {
    // Desktop development fallback only. A packaged Android build must include the plugin.
    try {
      await invoke('open_external_url', { url });
      toast('Opened in the system browser (desktop fallback).');
    } catch (error) {
      toast('Browser bridge is not available in this build.');
      console.error(nativeError, error);
    }
  }
}
$('navForm').addEventListener('submit', e => { e.preventDefault(); openDestination(address.value); });
document.querySelectorAll('[data-url]').forEach(button => button.addEventListener('click', () => openDestination(button.dataset.url)));
backButton.addEventListener('click', async () => {
  if (!hasOpenedDestination) return;
  try { await invoke('plugin:browser|back'); }
  catch (error) { console.error(error); toast('Back navigation is unavailable in this build.'); }
});
forwardButton.addEventListener('click', async () => {
  if (!hasOpenedDestination) return;
  try { await invoke('plugin:browser|forward'); }
  catch (error) { console.error(error); toast('Forward navigation is unavailable in this build.'); }
});
$('reload').addEventListener('click', async () => {
  if (!hasOpenedDestination) { toast('Open a website first.'); return; }
  try { await invoke('plugin:browser|reload'); }
  catch (error) { console.error(error); toast('Reload is unavailable in this build.'); }
});
$('brandHome').addEventListener('click', async event => {
  event.preventDefault();
  try { await invoke('plugin:browser|home'); }
  catch (error) { console.error(error); toast('Home is unavailable in this build.'); }
});
document.querySelectorAll('.launch-icon img').forEach(img => {
  img.addEventListener('load', () => { img.parentElement.dataset.hasIcon = 'true'; });
  img.addEventListener('error', () => img.remove());
});
$('settingsBtn').addEventListener('click', () => {
  const dialog = $('settingsDialog');
  if (!dialog.open) dialog.showModal();
});
