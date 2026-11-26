# Voice Control

Android app: press the floating mic, then say `read screen`, `tap <name>`, `type <text>`, `scroll down` or `scroll up`.

Flow: mic button -> speech to text -> parse command -> read the screen's accessibility tree -> act -> speak errors.

## Install
1. GitHub > Actions > Build APK > latest run > download `voice-control-apk`.
2. Unzip, install the APK, open the app and follow the two buttons.

On Android 13+, you may need App info > menu > "Allow restricted settings" before enabling the service.
Speech recognition may send audio to your phone's speech provider.
