# android-example
This repository contains an example Android app, showing how to integrate with the TDM Android Frontend Library.


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
4. Open the repository root in Android Studio (File/New/Import project...).

5. Attach an Android Phone or Tablet to your computer and verify that Android Studio finds it.

6. Run the test application from Android Studio (Run/Run testapplication)
