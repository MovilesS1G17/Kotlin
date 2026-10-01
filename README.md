# Kotlin
This is the frontend code repository for ISIS-3510 Mobile Application Development course project, built using Kotlin to create a native Android app experience.

## Live backend setup

The app now uses the shared FastAPI backend for email authentication, profile name, videos and folders. The JWT is encrypted with Android Keystore before it is stored on device. For the emulator, Debug defaults to `http://10.0.2.2:8000`; set Gradle property `centraliaApiBaseUrl` to your backend URL for another environment. Release builds should use HTTPS. When source metadata is unavailable, the save flow keeps the validated URL and shows missing fields rather than fixture content. Library changes and source-open events are sent to the backend.
