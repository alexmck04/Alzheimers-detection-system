### NEUROMIND DOCTORS WEB PORTAL


## Overview
NeuroMind Systems Doctors Web Portal is the Doctors & Admin web application for the NeuroMind Alzheimer’s detection system. It supports managing doctors and patients, reviewing submitted assessments, checking the assesments for risks, generating patient risk reports, handling doctors' patient notifications and support page messaging. It integrates with Firebase Authentication & Firestore, and is intended to work alongside the NeuroMind patient Android app.

---

# ADMIN ACCOUNT DETAILS: (only one exists)
    Email:      admin@mymtu.ie
    Password:   example

# DOCTOR ACCOUNT EXAMPLE:
    Email:      homersimpon@gmail.com
    Password:   homerhomer

## Features
- Login (Firebase Authentication)
- Signup (Invited by Admin)
- Role-based access (Doctor / Admin)
- Admin dashboard for doctor/patient management
- Home Page (profile overview & editing)
- Patient list and patient profiles
- Risks list and risk detail pages
- Reports list and report detail pages
- Notifications page
- Ratings page
- Support messages page

---

# Features In-Depth
- Login Page
    Allows the Doctor/Admin to log in by email or with a Google account

- Signup Page
    If a Doctor is invited by an admin, they will recieve an email with a link for the website (given the email exists)
        This is done by using EmailJS with a custom email template.
    They can then create an account using the email that received the invite.

- Role-based Access
    Depending on wether an Admin or Doctor was logged in, the navigation bar changes.
    This allows them to see certain routes as they serve different roles.

- Admin Dashboard
    An Admin that has logged in can see the Doctors & Patients list, this allows for:
        - inviting doctors
        - view pending & existing doctors
        - deleting patients
        - assigning doctors to patients

    They also see a Support Messages Page (exclusive to Admins)

- Home Page
    Shows an overview of the logged in accounts' profile. The information shown consists of:
        - Profile Picture
        - Full Name
        - Email
        - Office
        - Telephone
        - DOB
        - Current Time
    
    It provides the user with an "Edit Profile" button that allows for changing personal details.


- Patient List
    The logged in Doctor can see their assigned patients. It shows the Name, Email, Address and Emergency Contact.

    - Patient Profile
        Clicking on a patients profile allows the doctor to all patient information (only GDPR approved details)
        There is an Edit Profile button that allows the doctor to change the patients details or delete the patient.

- Risks List
    This shows the patients Name, Age and Risk % (average calculated over all tests completed)

    - Risk Detail Page
    Clicking on the patients card will show the different test categories with their respective completed tests and scores.

- Report List
    This shows the doctors the patient list again, requiring them to select a profile to advance

    - Report Detail Page
        Opening a patient profile shows a Document containing:
            - Patient Name
            - Risk Summary (average, highest & lowest, risk level)
            - Completed Tests (cognitive, lifestyle, speech prediction)
            - Last Saved Reports (date, risk% & risk level of last generated report)

        Along with
            - Download PDF button (download document)
            - Generate Report button (creates the report & sends it to the patient)

    
- Notifications Page
    This shows the Doctors notifications for when their assigned patients complete a test.
    It shows the test, time & risk. There is a "Mark As Seen" button to hide them

- Ratings Page
    The Doctors can see all the reviews left by anonymous patients, showing their rating & message.

- Support Message Page (Admin Only)
    The Admin can see all the support requests sent in by the patients.
    They can be marked as solved or unsolved.

---

## Tech Stack
- React
- React Router
- JSX, HTML, CSS, JSON
- EmailJS
- NewsAPI
- Firebase
  - Authentication
  - Firestore
  - Hosting (deployment)
  - Google Auth

---

## Requirements
- Node.js
- Npm
- Firebase project
- Firebase CLI (functions)
- .env (API keys)

---


## CODE NEEDED TO RUN

# install for dependancies
npm install

# rare case you would have to use these
npm install -g firebase-tools
firebase login

