# Walkthrough - SMKN 1 Cimahi Logo Integration

I have integrated the SMKN 1 Cimahi logo into the app's header and splash screen.

## Changes Made

### UI Components

#### [HomeScreen.kt](file:///C:/Users/gugum/AndroidStudioProjects/STMLarkam/app/src/main/java/com/wiryadinata/stmlarkam/ui/home/HomeScreen.kt)
- Added a `Row` in the `TopAppBar` title to include the logo image next to the app name.
- The logo is sized at 40dp with a 12dp spacer.

#### [SplashScreen.kt](file:///C:/Users/gugum/AndroidStudioProjects/STMLarkam/app/src/main/java/com/wiryadinata/stmlarkam/ui/splash/SplashScreen.kt)
- Replaced the `DirectionsRun` icon with the SMKN 1 Cimahi logo image.
- The logo is centered within the existing circular white background.

## Verification Results

### Automated Tests
- `gradle assembleDebug` passed successfully.

### Manual Verification Required

> [!CAUTION]
> The file `app/src/main/res/drawable/logo_smkn1cimahi.png` currently contains an error message (90 bytes) because I cannot save images directly from our chat.
>
> **Please manually save the logo image you provided to `app/src/main/res/drawable/logo_smkn1cimahi.png`** to see the changes in the app.

## Visual Changes (Expected)

The header should now look like this (but with the real logo):
[Logo] **STM LARKAM**
Rekap Lari Kampus
