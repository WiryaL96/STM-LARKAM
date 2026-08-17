# Implementation Plan - Add SMKN 1 Cimahi Logo

This plan outlines the steps to add the SMKN 1 Cimahi logo to the app header and update the splash screen as requested.

## User Review Required

> [!IMPORTANT]
> The logo file `app/src/main/res/drawable/logo_smkn1cimahi.png` currently contains an error message from a failed download. Since you've provided the logo in the chat, please **save that image as `app/src/main/res/drawable/logo_smkn1cimahi.png`** in your project so the code changes can reference it correctly.

## Proposed Changes

### Assets

#### [MODIFY] logo_smkn1cimahi.png
- Ensure this file is a valid PNG image of the SMKN 1 Cimahi logo.

### UI Components

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/gugum/AndroidStudioProjects/STMLarkam/app/src/main/java/com/wiryadinata/stmlarkam/ui/home/HomeScreen.kt)
- Update the `TopAppBar` title to include the logo next to the "STM LARKAM" text.
- Use a `Row` to align the logo and the existing `Column` (title + subtitle).

#### [MODIFY] [SplashScreen.kt](file:///C:/Users/gugum/AndroidStudioProjects/STMLarkam/app/src/main/java/com/wiryadinata/stmlarkam/ui/splash/SplashScreen.kt)
- Replace the `DirectionsRun` icon with the SMKN 1 Cimahi logo using the `Image` composable.
- Adjust the size and styling to fit the splash screen design.

## Verification Plan

### Automated Tests
- Run `gradle assembleDebug` to ensure the project builds successfully.

### Manual Verification
- Deploy the app to a device/emulator to verify:
    - The logo appears in the `TopAppBar` on the Home screen.
    - The splash screen shows the new logo instead of the running icon.
