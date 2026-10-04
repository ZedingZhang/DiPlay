# Test checklist

Use the [installation guide](INSTALL.md). With the car parked, verify wired and wireless connection, picture, touch and music. Test disconnect/reconnect, then settings Apply/Cancel. Save a diagnostic report after reproducing an issue.

For channel memory, connect until authenticated CarPlay renders, disconnect and reconnect without changing the car's Wi-Fi association. Look for `remembered saved` followed by `remembered first`. Report absent events; creating a hotspot alone is insufficient.

Include head-unit model, DiLink/Android, iPhone/iOS, wired/wireless, app version and exact steps. Do not post credentials or unreviewed personal information. See [compatibility](COMPATIBILITY.md) for remaining limitations.

## Upstream post-v0.2.11 integration

The integration includes the upstream release tag `v0.2.11` at `6014025c653c4dae88d319ce446e0bf1ddb658ea` and subsequent main commits through `e6e7cc0eaf175ef397eee7ac6223eb267cdc68bd`. These main updates have not been published as a newer upstream release. The four phone fixes below must pass together with the upstream regressions before publishing the fork update. The separate draft Wi-Fi Direct investigation in PR #6 is deferred and is not part of this integration. Its unresolved phone connection failure is not claimed fixed by these updates.

The debug package identifies itself as `0.2.11-post-sync-hud-test` to distinguish this snapshot from the earlier release. Check connection and microphone, then exercise the following new phone controls:

- Enter a custom resolution such as 57%, save/reconnect and confirm it remains 57% after reopening Settings. Cancel a change to 83% and confirm it stays 57%. At 57%, compare all three CarPlay sizes in both orientations; the requested canvas and touch mapping must follow both settings.
- Return from connection setup to the existing CarPlay activity and reconnect. The saved connection mode, hotspot and authentication target must be reloaded. The configured multi-finger swipe must open the same DiPlay settings page as the home Settings button, preserving the current session. Advanced settings start collapsed and remain expanded after rotation when opened. Dialog Cancel leaves the saved preference unchanged; Apply and reconnect applies changes that require a new session.
- Toggle top and bottom system bars independently; leave and reopen CarPlay to confirm the saved values. Rotate and enter/leave split-screen; preparation content must fit, navigation must remain accessible and touch mapping must follow the negotiated canvas.
- In the picture panel, change brightness, contrast, saturation and warmth during CarPlay, try the temporary original comparison and reset to neutral. These controls require TextureView rendering; confirm default picture settings preserve the original appearance.
- Try System, Day, Night and Ambient night modes. Ambient depends on a working light sensor; test the threshold and delay and fallback to System if unavailable. The iPhone's appearance setting can still override the result.
- Export diagnostics. Where public Downloads or a document picker are unavailable, verify the private fallback can be viewed/shared. USB confirmation automation is optional and requires explicitly enabling its accessibility service; leave it disabled unless testing it.

BYD turn cards and virtual instrument maps remain upstream features. Their hardware behavior is not validated by Xiaomi phone tests.

On Xiaomi Mi 10 / Android 11, reconnect wirelessly, check picture/touch/music, make a call and verify microphone behavior and music playback after the call ends. Try Wi-Fi Direct if used; Auto should use bounded recovery rather than an indefinite wait, while unsupported manual channels must report an error. If using USB, also try starting it before filling the Manual wireless hotspot fields. Confirm the saved report contains connection stages, startup settings and microphone diagnostics after exercising those paths. The BYD navigation card should remain hidden on this phone; the new optional Advanced vehicle data section does not need to be enabled. Repeat all four regression sections below, including rotation while reconnecting and changing the CarPlay size preset.

The manual standalone APK workflow verifies the published v0.2.11 APK digest before extracting its two runtime identity assets, and then checks the rebuilt package/version, APK signature, asset bytes and ARM64 libraries. Source-only CI retains its normal asset-free builds.

## Phone landscape UI

The phone settings APK is identified by `0.2.11-phone-settings-hud-test`. In landscape at least 600dp wide with a short window, main Settings must place CarPlay controls and Connection setup alongside each other, then Diagnostics and Automatic connection alongside each other. The first two complete groups and the next headings should fit on the first screen at normal Chinese font size. Display/performance and expanded Advanced settings keep a full-width area with paired choices and switches. The fixed header includes the page title, replacing the separate large introductory heading.

Connection setup places the connection mode and hotspot details on the left, with pairing and connection actions on the right. Choose iPhone/Review permissions and Wireless/USB actions are paired; wireless and USB buttons should be available on the first screen at normal font size. Only the selected mode's description is shown. Changing to a manual hotspot must still require saving valid details; cancelling must preserve Wi-Fi Direct and existing saved credentials. Selecting Wi-Fi Direct restores the channel chooser. Verify scroll restoration and fixed navigation after changing a mode or reopening Settings from CarPlay.

Repeat with enlarged text, portrait, and a landscape window narrower than 600dp; text may wrap and require scrolling, but no button may be clipped or have a touch target smaller than 48dp. A car-sized 1024x600dp window outside multi-window retains its full-width groups and large controls. The APK does not change the screen-orientation or app-language policies withdrawn during the previous task.

On Xiaomi Mi 10 / Android 11, open the Chinese home screen in landscape with the default font size, both disconnected and with CarPlay connected. Wireless connect/open CarPlay, choose iPhone, disconnect, USB, connection setup and settings should fit above the fold in a short window at least 600dp wide. On shorter landscape windows below 600dp wide, cards stack and remain scrollable. Larger fonts may require scrolling, but button labels should wrap and touch targets remain at least 48dp high.

Open Settings and Connection setup. Check the denser spacing and side-by-side choices, scroll to the bottom and confirm the top Back button remains visible. Change resolution, cancel another choice, switch connection mode and verify the same saved values and reconnect behavior as before. Rotate on each page; the current page should remain open, and narrow portrait windows use the upstream compact layout while keeping the primary actions accessible. Check a car-sized landscape window at least 550dp wide and 480dp high outside multi-window mode still has the original large controls. Native Android 11 layout tests export Chinese home/settings, a narrow window and enlarged English text previews with the CI test reports.

## Unified settings and advanced controls

On Xiaomi Mi 10 / Android 11, compare home Settings with the three-finger swipe during CarPlay: the page and settings must be identical. Repeat with the configured two- and four-finger gestures. Wrong finger counts or horizontal/upward gestures must not open settings. Opening settings, dismissing a dialog or returning home must preserve a healthy session; Open CarPlay returns to it.

Expand Advanced settings. Check Local offline / USB CH341 authentication, software HEVC, physical reference/basis, safe area, receiver manufacturer/model and on-screen debug logs. A saved 55 fps value must remain visible, with all 30–60 fps options in 5 fps steps. Local authentication must be validated before saving; failure leaves the old authentication choice intact. Choosing USB must not install or silently switch to local authentication. A non-BYD phone must still hide OEM icon controls and the CarPlay BYD tile.

Safe-area editing becomes available after the CarPlay viewport is known. Cancel and Reset inside that dialog must not change the saved rect; Save applies the draft and reconnects. Rotate while editing and confirm the saved rect belongs to the dimensions named in the dialog rather than the newly rotated viewport. Debug-log visibility must reload when returning to CarPlay. Scroll the advanced section in a short landscape window and confirm fixed navigation and at least 48dp touch targets.

## CarPlay OEM return icon

On a non-BYD device such as Xiaomi Mi 10 / Android 11, connect or fully reconnect and open the CarPlay app grid. The BYD return tile should be absent, including when the device previously used a build that advertised the BYD icon. In the AirPlay `/info` log, `oemIconVisible` should be false and `oemIcons` and `oemIconLabel` should be absent. On a BYD head unit, confirm the return tile still appears and opens the car home screen; a configured custom image should still be used.

## CarPlay size settings

Connect until CarPlay renders. In Settings > Display and performance, select Large, Medium, then Small, applying each change and allowing the reconnect to finish. Compare icons and text on the same CarPlay screen, and check touch targets near all four corners. Repeat at 80% resolution and in portrait orientation, then return to Medium at native resolution.

The diagnostic report must name the selected preset and request a different canvas for each size. For a 2250x1080 surface at native resolution, Large requests 1956x940, Medium 2250x1080, and Small 2648x1270. Small requires hardware decoder support for the enlarged canvas and selected frame rate. If unsupported, the effective canvas falls back to Medium with an explicit reason in the report; the saved Small preference remains available for the next connection or a lower resolution. Rotating the display and camera-window resizing must continue to map touch to the negotiated canvas.

## Settings rotation and locked CarPlay direction

- On Xiaomi Mi 10 / Android 11, set the app language to Simplified Chinese. Rotate Home, Settings and Connection setup between portrait and both landscape directions. Each page must follow the phone and use the compact two-column settings layout in landscape.
- Connect with the phone already in landscape. While the CarPlay picture is visible, rotate through portrait, reverse landscape and upside-down portrait, waiting at least 10 seconds each time. CarPlay must keep its original direction and picture without showing preparation or reconnecting. Repeat after starting a fresh connection in portrait and in reverse landscape.
- Open Settings with the configured three-finger swipe while CarPlay is connected. Rotate the settings page, then return to CarPlay without changing settings. CarPlay must restore its original locked direction and keep the existing session. Repeat several times; the log must not contain a rotation-triggered `Display changed` reconnect.
- In Settings, deliberately change CarPlay size or resolution and apply. The requested reconnect must still happen, using the original CarPlay direction. Disconnect completely, rotate Settings to another direction and connect again; this new session must lock the newly selected direction.

## Preferred Wi-Fi Direct channel

- In **Settings → Connection setup → Wi-Fi Direct**, confirm **Preferred channel: Auto** on a fresh install. Select channel 149 and Cancel; Auto must remain selected. Select 149 and Save, reopen the chooser and restart the app to confirm it stays saved.
- Disconnect/reconnect after saving. Check `channel preference=149 frequencyMHz=5745`, `create mode=PREFERRED_CHANNEL`, and `requestedMHz=5745 actualMHz=5745 matched=true`. An unsupported channel or a different actual channel must report an error instead of silently falling back. Select Auto to restore automatic startup.
- Compare Auto and manual choices with the car already joined to Wi-Fi. A manual choice must override station alignment and any remembered automatic channel. Successful manual sessions must not replace the remembered automatic configuration.
- Switch to built-in hotspot and USB. The channel chooser must be hidden for built-in hotspot, and neither connection may apply the Wi-Fi Direct preference. Returning to Wi-Fi Direct must restore the saved choice. Saving a channel during a connection must leave that session running and apply the change to the next connection.

## Wireless and USB car data

- On wireless, with **Report location to iPhone** on, confirm the Bluetooth bootstrap identifies with `location=false vehicleStatus=false`, then the Wi-Fi tunnel receives its own `start-location-information` before its first `location-information`. There must be no location output on Bluetooth and no unsolicited continuation from it.
- On USB, start a fresh wired session rather than plugging into an already active wireless session. Confirm the USB iAP2 link receives StartLocationInformation and carries all location updates itself.
- With **Car battery for the iPhone** on, confirm on wireless that the Bluetooth bootstrap does not advertise Vehicle Status and that the Wi-Fi tunnel receives its own `0xa100 start-vehicle-status` before sending `0xa101 vehicle-status`. On USB, the single wired iAP2 link must receive `0xa100` and send `0xa101`. A missing or stale battery reading must leave Vehicle Status undeclared rather than sending invented values.

## Video while parked

- Use a plain HTTPS MP4 or HLS item that supports AirPlay, such as one sent from Safari. DRM-protected services and apps that disable AirPlay are not acceptance tests.
- Test fresh wireless and fresh USB sessions separately. Confirm `/info videoInCar=true`, SETUP negotiates `videoPlayback`, the event channel becomes ready, and the latest P-state reports `delivery=SENT` even if it was first `QUEUED`.
- Confirm the video settings stream and remote-control stream are accepted, `requestUI videoplayback:` opens the player, and the player log reports a validated internet network before loading the URL.
- Shift out of P and confirm availability becomes false and the car player closes immediately. Disable ADB or make the gear unreadable and confirm the same fail-closed behavior.

## Rotation during reconnect

On an Android device that supports screen rotation, connect until CarPlay renders, then rotate from landscape to portrait and back while the connection is rebuilding. Repeat in both directions, including several quick rotations and a 180-degree turn. Let the device settle after the last rotation and check that the CarPlay picture has the correct aspect ratio and that touch targets match the displayed controls.

In the diagnostic report, the next `Starting CarPlay controller at` and `Display request` must use the latest settled dimensions, including a `Display updated while handshake is reset` event that arrived during teardown. A queued size change must settle before startup; cancelling it by returning to the accepted size must still resume the connection. On a BYD head unit, also open and close the camera window to confirm that a shrink/restore within the original window keeps the existing CarPlay session.

## Location reporting

With the car parked, open **Settings → Location → Report location to iPhone**.

- On a fresh installation, the switch is off. Enabling it requests precise location if needed; denying the request or granting only approximate location leaves it off.
- Grant precise location, enable the switch, then reopen Settings to confirm the saved state. With no connection running, the setting applies to the next connection.
- During wired and wireless CarPlay, enabling or disabling the switch reconnects the session. When enabled and requested by the iPhone, check for `start-location-information` and `location-information` in the DiPlay diagnostics; on wireless, also verify that reporting continues after the Bluetooth-to-Wi-Fi handoff.
- Disable the switch and confirm the next session does not advertise location reporting. These checks verify the accessory reporting path; they do not establish which inputs iOS uses in each fused location result.

## Advanced vehicle data

- Expand **Settings → Location → Advanced vehicle data**. Confirm a fresh install uses **Default mode · verified on DiLink 5.0 head units** and shows the battery, wheel-speed and parked-video switches without a field probe.
- In Default mode, tap **Check ADB access** and record the battery, speed and gear it shows. With CarPlay connected, turn on a switch whose data cannot be read: CarPlay must stay connected and the page must show what cannot be read.
- Select **Legacy head-unit detection · tested on controller 13 / DiLink 3.0**. Approve the key if the car asks; the same action must continue into the read-only field probe. With “Always allow” ticked, the page must not say the car allowed DiPlay only once. A failed probe must leave Default mode selected.
- Reopen Settings, restart DiPlay, change gear and reconnect CarPlay. The successful probe, resolved fields and enabled battery/wheel-speed/video switches must remain saved without another tap, even when the current firmware metadata differs.
- Switch back to Default mode and confirm the saved legacy probe remains available when Legacy mode is selected again.
- Turn ADB off temporarily. Saved functions and switches must remain visible; turning ADB back on allows automatic validation. Two READY-but-unreadable validations trigger one automatic re-probe, while an incomplete re-probe preserves the previous snapshot and shows manual retry.
- Press the first probe, authorization and retry controls after scrolling down the page. Progress and results must remain at the same scroll position rather than jumping to the top.
- Scroll down Settings, open CarPlay, then return to Settings (Back to DiPlay or the three-finger gesture). The page must keep its scroll position.
- When the BYD navigation card is available, confirm **Dashboard song** exists there exactly once and does not appear in Advanced vehicle data. Without that card, it must appear once under Advanced vehicle data, and turning it on must show the CarPlay song on the dashboard.

## Hotspot and vehicle-settings interaction

- On a supported BYD unit, choose the built-in car hotspot. Automatic hotspot startup stays off on a fresh installation. Enable it explicitly and approve the ADB prompt; the setting saves only after DiPlay confirms its own required permissions. Denial must leave it off. Choosing Wi-Fi Direct hides the hotspot card and preserves its saved preference.
- Expand Advanced vehicle data. Battery, wheel-speed and parked-video switches must appear only in that section, with unavailable legacy fields hidden. The hotspot card must not provide duplicate switches that bypass the selected mode.
- Start a user vehicle check or probe, then try the hotspot switch before it finishes. A second authorization flow must not start. After the vehicle operation finishes, the hotspot switch becomes usable again.
- Start hotspot authorization while Advanced vehicle data is expanded. Mode and vehicle choices must stay disabled until it completes; an automatic saved-field validation must resume afterwards without another authorization prompt.
- During a pending battery preflight, let the hotspot eligibility check finish and redraw Settings. A valid vehicle result must still apply the requested reconnect once; an unreadable result or ADB failure must keep the existing connection.
