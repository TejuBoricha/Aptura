# Google Play Production Keystore Setup & Signing Guide

Aptura's Gradle build is engineered with zero-leak signing: keystore paths and passwords are never checked into git, and are read exclusively from your local `local.properties` file or environment variables.

---

### Step 1: Generate an Upload Keystore (If you do not have one yet)

Run the standard Java `keytool` command in PowerShell from the project root:

```powershell
New-Item -ItemType Directory -Force "$HOME\.aptura" | Out-Null
keytool -genkeypair -v -keystore "$HOME\.aptura\upload-keystore.jks" -alias aptura-upload -keyalg RSA -keysize 2048 -validity 10000
```

When prompted:
1. Enter and confirm a strong, unique keystore password, and store it in a password manager.
2. Enter your name / organization.
3. Keep the keystore **outside the repository**, as in the command above. `.gitignore` also ignores key files and CI rejects a tracked one, but a file that is not in the repository cannot be committed at all. Back it up somewhere private. If it is ever pushed to GitHub, treat it as compromised and replace it.

---

### Step 2: Configure `local.properties`

Open `local.properties` (in the project root) and append the signing properties:

```properties
aptura.signing.storeFile=C:/Users/<you>/.aptura/upload-keystore.jks
aptura.signing.storePassword=<keystore password>
aptura.signing.keyAlias=aptura-upload
aptura.signing.keyPassword=<key password>
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
