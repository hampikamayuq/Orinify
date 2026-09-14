# Login review — 2026-09-14

The device trace still reports HTTP 400 INVALID_ARGUMENT for signed player requests and
LOGIN_REQUIRED for unsigned requests. This identifies a difference in credential presentation;
it does not establish that login capture is the cause or that changing login fixes playback.

The previous login screen saved cookies on history changes and immediately fetched account info
through the shared client. That client received the new cookie asynchronously via DataStore, so
account lookup could use the previous session. Visitor data was saved separately through an
unrestricted JavaScript interface, and work outlived the screen through GlobalScope.

The revised flow lets the user choose an account and explicitly connect it after YouTube Music
loads. It reads page state only from the exact HTTPS music.youtube.com origin, captures the
cookie for that origin, and validates the candidate through an isolated account-menu request.
Only successful validation persists cookie, visitor data and account metadata in one DataStore
edit. Failed validation leaves the prior session intact. The temporary HTTP client is closed;
coroutines and the WebView follow the screen lifecycle. Exceptions containing transport data
are not logged or displayed. This uses Android CookieManager and evaluateJavascript; no native
JavaScript bridge is registered.

Device verification remains necessary: install the debug APK over the previous debug build,
open login, choose the account, wait for YouTube Music to load, and tap Connect this account.
Then test playback. Account-menu validation proves only that the account endpoint accepts the
session; it is not evidence that a player client will accept it or serve playable audio.

Android API reference: https://developer.android.com/reference/android/webkit/CookieManager

## Follow-up after login v2

The device still returned the same signed-native-client HTTP 400 errors. Current yt-dlp source
(`yt_dlp/extractor/youtube/_base.py`, inspected 2026-09-14) marks web/web_music as supporting
cookies, defaults unsupported clients to false, and does not enable that flag for android_vr.
The earlier report's assumption that a browser session should be offered to ANDROID_VR was wrong.

Removed the credential presentation ladder. Authenticated playback now requests WEB_REMIX
1.20260707.12.00, separately from the browse configuration. Native fallback requests remain
anonymous, with no borrowed visitor identity; the HTTP header builder enforces the cookie support
policy as well as the request planner. Regression tests cover that policy.

A real anonymous probe from this machine returned HTTP 200 LOGIN_REQUIRED with no audio URLs
for ANDROID_VR 1.65.10 and VISIONOS 1.02; WEB_REMIX 1.20260707.12.00 returned UNPLAYABLE.
No account cookies or signed media URLs were used or recorded. This does not prove that the
web player will work authenticated. Orinify still lacks handling for signatureCipher formats and
any additional web playback requirements. A successful account-menu request alone cannot close
this incident. Next device observation needed: does ordinary playback work inside the visible
YouTube Music login page with the same account?

## Visible web playback

The user confirmed that music plays inside the YouTube Music login page with this account.
This narrows the incident to the native request/stream-resolution path for the tested playback;
changing login alone did not fix it.

The error UI now offers a visible first-party web player for the selected track. It uses the
same WebView cookie store as login, requires the user to tap Play, registers no JavaScript
bridge, and does not extract media URLs or bypass site playback requirements. Native playback
is paused while the modal is open. Dismissal destroys its WebView; leaving the foreground
closes it. This is a foreground alternative, not a repair of native/background playback,
downloads, or queue integration. Technical error details are collapsed and scrollable.

Validation: unit tests cover fixed-origin URL generation and rejection of injected/invalid
video IDs. End-to-end playback, WebView lifecycle behavior and installation need device testing.
