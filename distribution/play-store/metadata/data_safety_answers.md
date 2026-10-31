# Google Play Console: Data Safety Questionnaire Walkthrough for Aptura

This document provides exact answers for the **Data safety** questionnaire in Google Play Console. Because Aptura is strictly offline, answering this questionnaire takes less than 2 minutes.

---

### Section 1: Data collection and security

| Questionnaire Item | Response | Why |
|---|---|---|
| **Does your app collect or share any of the required user data types?** | **No** | Aptura does not collect or transmit any user data. The app does not declare or request `android.permission.INTERNET`. |
| **Is all of the user data collected by your app encrypted in transit?** | *N/A (Disabled)* | No data is collected or transmitted over any network. |
| **Do you provide a way for users to request that their data is deleted?** | *N/A (Disabled)* | No user data is stored off the user's physical device. |

---

### Section 2: Specific Data Types

When prompted to select any collected data types across categories:
- **Location:** None
- **Personal info (Name, Email, etc.):** None
- **Financial info:** None
- **Health and fitness:** None
- **Messages / Emails:** None
- **Photos and videos:** **No** (Videos are read strictly locally from the device's MediaStore or user-chosen Storage Access Framework folders; they are never uploaded, collected, or shared).
- **Audio files:** None
- **Files and docs:** None
- **App activity / App info and performance (Crash logs, diagnostics):** **No** (Aptura contains zero third-party telemetry, crash reporting, or analytics SDKs like Firebase, Crashlytics, or Sentry).
- **Device or other IDs:** None

---

### Section 3: App Content Declarations

| Policy Area | Response | Details |
|---|---|---|
| **Privacy Policy URL** | `https://github.com/TejuBoricha/Aptura/blob/main/docs/PRIVACY.md` | Public GitHub markdown document stating zero-data-collection policy. |
| **Target Audience** | **13 and older** (or 18+) | General audience video player. |
| **News Apps** | **No** | Not a news app. |
| **COVID-19 Contact Tracing** | **No** | Not related to contact tracing. |
| **Ads** | **No, my app does not contain ads** | Clean UI with 0 ad networks. |
| **Financial Features** | **My app doesn't provide any financial features** | No in-app purchases or financial services. |
