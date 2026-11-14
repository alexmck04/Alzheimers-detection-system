### NEUROMIND PATIENT ANDROID APP


## Overview
NeuroMind Systems Patient Android App is the patients mobile application for the NeuroMind Alzheimer’s detection system. 
It allows patients to subscribe to the app in order to complete cognitive and lifestyle questionnaires and record speech samples for analysis.
The patients are able to view their results, access generated risk reports, rate the app, edit their details & contact support. 
The app integrates with Firebase Authentication and Firestore, and works alongside the NeuroMind Doctors Web Portal.

---

# PATIENT ACCOUNT EXAMPLE:
Email: rinaldstest@gmail.com  
Password: example

# STRIPE CARD DETAILS:
Card Number: 4242 4242 4242 4242
MM/YY: 12/29
CVC: 123
Country: United States (default)
ZIP: 11111

---

## Features
- Login (Firebase Authentication)
- Signup (Email/Password or Google account)
- Subscription-based feature access
- Home page (app description)
- Profile Overview Page (edit details)
- Cognitive questionnaires (text based)
- Lifestyle questionnaires (numerical)
- Speech recording & upload (mp3 of image description)
- AI-based speech risk prediction (predicts risk based off mp3)
- Risk Report overview (shows generated risk report)
- Call Support Number (opens phone app)
- Support Request (send to web Admin)

---

# Features In-Depth

- Login Page
    Allows the Patient to log in using email & password or a Google account.

- Signup Page
    Allows Patients to create an account using email/password or Google sign-in.

- Home Page  
    Displays an "Understanding Alzheimer's" text, giving a description of the illness.

- Subscription Access  
    Patients risk detection features (speech tests and questionnaires) are locked behind an active subscription.
    - Stripe is used to complete the payment, unlocking the app for the Patient

- Profile Overview Page
    The Patients profile overview allows them to change their data
    It also shows their assigned doctor & current Dementia score

- Cognitive Tests  
    Patients can complete cognitive questionnaires designed to test for Alzheimer's
    They consist of memory questions asking about the year/date, remembering a sentence from earlier, listing animals.
    The results are stored in firebase and used in the risk analysis.

- Lifestyle Tests  
    Patients can complete lifestyle questionnaires designed to test for Alzheimer's
    The test asks about the sleep, activity, and habits of the patient ranging from 1 - 5. 
    The results are stored in firebase and used in the risk analysis.

- Speech Test  
    Patients can record a short speech sample using the devices microphone.
    They are asked to describe an image, recording can vary in length.

- AI-based Speech Risk Prediction
    The recording is uploaded and analysed by the AI model, giving a speech-based risk prediction.

- Risk Report Overview  
    The Patient can see their most recent risk report from their assigned doctor
    The report shows their current score, average risk, highest test & lowest test.
    It also provides them with a short description of their assessment.

- Call Support Number (Bottom of Profile Overview)
    Shows a support number, allowing Patients to call it when clicked.
    Opens phoning app with number inserted

- Support Request (Bottom of Profile Overview)
    Allows the Patient to create a support request ticket, asking them to:
    - State the support subject
    - Describe the issue
    Before pressing the "Send" button to submit

---

## Payments
- One off payment (€10)
- Implemented using Stripe's Test Mode
- Uses fake card (mentioned above)

---

## Tech Stack
- Kotlin
- Jetpack Compose
- Android SDK
- Firebase
    - Authentication
    - Google Auth
    - Firestore
    - Cloud Storage
    - Function
- Stripe (Test Mode)
- AI Speech Predictor

---

## Requirements
- Android Studio
- Firebase project
- `google-services.json`
- Internet connection

---

## Code Needed To Run

# Install dependencies
Everything is automatically installed by Gradle when opening the project

# Firebase setup
- Have to add `google-services.json` to the `app/` directory if it's not there

# Run the app
- Open project in Android Studio
- Sync Gradle & Run 'app'
