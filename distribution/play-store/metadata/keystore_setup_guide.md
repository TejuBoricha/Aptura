# Google Play Production Keystore Setup & Signing Guide

Aptura's Gradle build is engineered with zero-leak signing: keystore paths and passwords are never checked into git, and are read exclusively from your local `local.properties` file or environment variables.

---

### Step 1: Generate an Upload Keystore (If you do not have one yet)

Run the standard Java `keytool` command in PowerShell from the project root:

```powershell
keytool -genkeypair -v -keystore upload-keystore.jks -alias aptura-upload -keyalg RSA -keysize 2048 -validity 10000
```

When prompted:
1. Enter and confirm a secure keystore password (e.g. `YourSecretPassword123`).
2. Enter your name / organization.
3. Keep the file `upload-keystore.jks` in the project root (it is already ignored by `.gitignore`).

---

### Step 2: Configure `local.properties`

Open `local.properties` (in the project root) and append the signing properties:

```properties
aptura.signing.storeFile=upload-keystore.jks
aptura.signing.storePassword=YourSecretPassword123
aptura.signing.keyAlias=aptura-upload
aptura.signing.keyPassword=YourSecretPassword123
```

*(Alternatively, you can set the environment variables `APTURA_SIGNING_STORE_FILE`, `APTURA_SIGNING_STORE_PASSWORD`, `APTURA_SIGNING_KEY_ALIAS`, and `APTURA_SIGNING_KEY_PASSWORD`).*

---

### Step 3: Build the Production Signed Bundle (`.aab`)

Run:
```powershell
.\gradlew.bat bundleRelease
```

The signed Android App Bundle will be output to:
`app/build/outputs/bundle/release/app-release.aab`

You can verify the signature using `apksigner` or `keytool`:
```powershell
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```

This `.aab` file is what you upload directly to the **Production** or **Internal testing** track in Google Play Console!
