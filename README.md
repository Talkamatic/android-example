# android-example
This repository contains an example Android app, showing how to integrate with the TDM Android Frontend Library.

Note: The Android Frontend Library is only supported by TDM 7.0 and lower.

1. [Prerequisites](#prerequisites)
2. [Building and running](#building-and-running)

## Prerequisites
_The following instructions apply if you are using the builting speech recognizer and text-to-speech component of your Android device._

The app defaults to using English. But in order to use it, you need to make sure that English is active in the **Google Text-to-speech** component. Similarily, other languages require the same setup.

If your device is connected to the Internet through a Wi-Fi connection, you are then good to go. If not, you also need to download language models to use the speech recognizer and text-to-speech components in offline mode.

### Enabling text-to-speech in your language
Go to **Settings > Language & Input > Text-to-speech output**. Tap the cogwheel next to **Google Text-to-speech engine**, then **Language** and select your language.

### Enabling offline mode for the **Google Voice input**
Go to **Settings > Language & Input > Voice input**. Tap the cogwheel next to **Enhanced Google services**, then **Offline speech recognition** and download your language.

### Enabling offline mode for the **Google Text-to-speech engine**
Go to **Settings > Language & Input > Text-to-speech output**. Tap the cogwheel next to **Google Text-to-speech engine**, then **Install voice data** and select your language.

## Building and running
In order to try the app, you need to do the following:

1. You need to clone the repository if you haven't done so already.

  ```
  > git clone https://github.com/Talkamatic/android-example.git
  ```

2. Add a the TDM Android Frontend Library. This file has to be obtained from Talkamatic.

  ```
  > cd android-example
  > mkdir testapplication/libs
  > cp <path>/tdmFrontend-X.X.X-release.aar testapplication/libs/
  ```

3. Make sure testapplication/build.gradle points to the right version of the .aar from step 2.

  ```
  dependencies {
  ...
      compile(name:'tdmFrontend-X.X.X-release', ext:'aar')
  ...
  }
  ```

4. Make sure that connections are made to the TDM server instead of localhost, in testapplication/src/main/java/se/talkamatic/testapplication/MainActivity.java

  ```
    private final String SERVER_ADDRESS = "localhost";
    private final int SERVER_PORT = 9090;
  ```

5. Open the repository root in Android Studio (File/New/Import project...).

6. Attach an Android Phone or Tablet to your computer and verify that Android Studio finds it.

7. Run the test application from Android Studio (Run/Run testapplication)
