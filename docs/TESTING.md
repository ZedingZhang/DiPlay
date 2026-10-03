# Test checklist

Use the [installation guide](INSTALL.md). With the car parked, verify wired and wireless connection, picture, touch and music. Test disconnect/reconnect, then settings Apply/Cancel. Save a diagnostic report after reproducing an issue.

For channel memory, connect until authenticated CarPlay renders, disconnect and reconnect without changing the car's Wi-Fi association. Look for `remembered saved` followed by `remembered first`. Report absent events; creating a hotspot alone is insufficient.

Include head-unit model, DiLink/Android, iPhone/iOS, wired/wireless, app version and exact steps. Do not post credentials or unreviewed personal information. See [compatibility](COMPATIBILITY.md) for remaining limitations.

## Upstream v0.2.10 integration

The integration uses the final upstream tag `v0.2.10` at `3e43e25c55921bdf5149f5f92851acf202ed353a`, including the release corrections for USBMUX, artwork queue bounds, video source validation and port/socket ownership. The four phone fixes below must pass together with the upstream regressions before publishing the fork update.

On Xiaomi Mi 10 / Android 11, reconnect wirelessly, check picture/touch/music, make a call and verify microphone behavior and music playback after the call ends. Try Wi-Fi Direct if used; a BUSY channel should receive bounded retries/fallback, not an indefinite wait. If using USB, also try starting it before filling the Manual wireless hotspot fields. Confirm the saved report contains connection stages, startup settings and microphone diagnostics after exercising those paths. Optional vehicle-only controls should remain hidden on this phone. Repeat all four regression sections below, including rotation while reconnecting and changing the CarPlay size preset.

The manual standalone APK workflow verifies the published v0.2.10 APK digest before extracting its two runtime identity assets, and then checks the rebuilt package/version, APK signature, asset bytes and ARM64 libraries. Source-only CI retains its normal asset-free builds.

## Phone Wi-Fi Direct connection

On Xiaomi Mi 10 / Android 11, turn off the phone's personal hotspot and keep Wi-Fi and Bluetooth enabled. Select Wi-Fi Direct, reconnect and verify picture, touch and music. The report should contain `Wi-Fi P2P endpoint source=framework_owner family=IPv4` when Android supplies a usable GO IPv4 address. Verify disconnect/reconnect on both 2.4 GHz and 5 GHz where supported, and rotate while connected. Group creation or Bluetooth authentication alone does not count as success; check the first video frame.

Switch to Manual hotspot, connect, then switch back to Wi-Fi Direct while the personal hotspot is still enabled. If Android reports P2P disabled, the app should stop channel retries and show a localized message and wireless settings button. Turn off the personal hotspot, leave Wi-Fi on, return and reconnect. If the vendor still reports P2P disabled, turn Wi-Fi off and on in system settings and retry. The app must not toggle the radio or delete another app's group automatically. Verify that an enabled P2P radio still receives bounded BUSY channel fallback, and Manual hotspot remains usable.

In a short phone landscape window, the connection/recovery screen must scroll so the complete error and wireless settings action remain reachable.

The earlier report proves successful group creation/Bluetooth authentication followed by no AirPlay session, and later a disabled P2P radio with repeated BUSY failures. The address/identity changes are a compatibility fix to test on hardware; they are not proof that the iPhone joined the earlier group.

## Phone landscape UI

On Xiaomi Mi 10 / Android 11, open the Chinese home screen in landscape with the default font size, both disconnected and with CarPlay connected. Wireless connect/open CarPlay, choose iPhone, disconnect, USB, connection setup and settings should fit above the fold in a short window at least 600dp wide. On shorter landscape windows below 600dp wide, cards stack and remain scrollable. Larger fonts may require scrolling, but button labels should wrap and touch targets remain at least 48dp high.

Open Settings and Connection setup. Check the denser spacing and side-by-side choices, scroll to the bottom and confirm the top Back button remains visible. Change resolution, cancel another choice, switch connection mode and verify the same saved values and reconnect behavior as before. Rotate on each page; the current page should remain open and portrait should return to the normal layout. Check a car-sized landscape window at least 480dp high still has the original large controls. Native Android 11 layout tests export Chinese home/settings and enlarged English text previews with the CI test reports.

## CarPlay OEM return icon

On a non-BYD device such as Xiaomi Mi 10 / Android 11, connect or fully reconnect and open the CarPlay app grid. The BYD return tile should be absent, including when the device previously used a build that advertised the BYD icon. In the AirPlay `/info` log, `oemIconVisible` should be false and `oemIcons` and `oemIconLabel` should be absent. On a BYD head unit, confirm the return tile still appears and opens the car home screen; a configured custom image should still be used.

## CarPlay size settings

Connect until CarPlay renders. In Settings > Display and performance, select Large, Medium, then Small, applying each change and allowing the reconnect to finish. Compare icons and text on the same CarPlay screen, and check touch targets near all four corners. Repeat at 80% resolution and in portrait orientation, then return to Medium at native resolution.

The diagnostic report must name the selected preset and request a different canvas for each size. For a 2250x1080 surface at native resolution, Large requests 1956x940, Medium 2250x1080, and Small 2648x1270. Small requires hardware decoder support for the enlarged canvas and selected frame rate. If unsupported, the effective canvas falls back to Medium with an explicit reason in the report; the saved Small preference remains available for the next connection or a lower resolution. Rotating the display and camera-window resizing must continue to map touch to the negotiated canvas.

## Rotation during reconnect

On an Android device that supports screen rotation, connect until CarPlay renders, then rotate from landscape to portrait and back while the connection is rebuilding. Repeat in both directions, including several quick rotations and a 180-degree turn. Let the device settle after the last rotation and check that the CarPlay picture has the correct aspect ratio and that touch targets match the displayed controls.

In the diagnostic report, the next `Starting CarPlay controller at` and `Display request` must use the latest settled dimensions, including a `Display updated while handshake is reset` event that arrived during teardown. A queued size change must settle before startup; cancelling it by returning to the accepted size must still resume the connection. On a BYD head unit, also open and close the camera window to confirm that a shrink/restore within the original window keeps the existing CarPlay session.
