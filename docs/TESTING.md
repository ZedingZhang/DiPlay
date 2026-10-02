# Test checklist

Use the [installation guide](INSTALL.md). With the car parked, verify wired and wireless connection, picture, touch and music. Test disconnect/reconnect, then settings Apply/Cancel. Save a diagnostic report after reproducing an issue.

For channel memory, connect until authenticated CarPlay renders, disconnect and reconnect without changing the car's Wi-Fi association. Look for `remembered saved` followed by `remembered first`. Report absent events; creating a hotspot alone is insufficient.

Include head-unit model, DiLink/Android, iPhone/iOS, wired/wireless, app version and exact steps. Do not post credentials or unreviewed personal information. See [compatibility](COMPATIBILITY.md) for remaining limitations.

## CarPlay size settings

Connect until CarPlay renders. In Settings > Display and performance, select Large, Medium, then Small, applying each change and allowing the reconnect to finish. Compare icons and text on the same CarPlay screen, and check touch targets near all four corners. Repeat at 80% resolution and in portrait orientation, then return to Medium at native resolution.

The diagnostic report must name the selected preset and request a different canvas for each size. For a 2250x1080 surface at native resolution, Large requests 1956x940, Medium 2250x1080, and Small 2648x1270. Small requires hardware decoder support for the enlarged canvas and selected frame rate. If unsupported, the effective canvas falls back to Medium with an explicit reason in the report; the saved Small preference remains available for the next connection or a lower resolution. Rotating the display and camera-window resizing must continue to map touch to the negotiated canvas.

## Rotation during reconnect

On an Android device that supports screen rotation, connect until CarPlay renders, then rotate from landscape to portrait and back while the connection is rebuilding. Repeat in both directions, including several quick rotations and a 180-degree turn. Let the device settle after the last rotation and check that the CarPlay picture has the correct aspect ratio and that touch targets match the displayed controls.

In the diagnostic report, the next `Starting CarPlay controller at` and `Display request` must use the latest settled dimensions, including a `Display updated while handshake is reset` event that arrived during teardown. A queued size change must settle before startup; cancelling it by returning to the accepted size must still resume the connection. On a BYD head unit, also open and close the camera window to confirm that a shrink/restore within the original window keeps the existing CarPlay session.
