# DripBlood-mobile-application
An Android app that connects blood donors with people in urgent need, shows nearby blood points/hospitals, and provides community and SOS features to support life‑saving donations.

Overview
Below is a ready‑to‑paste README.md for your DripBlood Android app, styled similarly to the hospital receptionist repo you linked, with emojis, sections, and professional structure.

You can copy this into a README.md at the root of your GitHub repo and then tweak wording/screenshots later.

# 🩸 DripBlood – Blood Donation & Emergency Helper
An Android app that connects blood donors with people in urgent need, shows nearby blood points/hospitals, and provides community and SOS features to support life‑saving donations.
---
## 📋 Table of Contents
- ✨ Features  
- 🛠️ Tech Stack  
- 📦 Installation  
- ⚙️ Configuration  
- 🚀 Getting Started  
- 🧱 Project Structure  
- 📖 Usage Guide  
- 🔐 Security & API Keys  
- 🤝 Contributing  
- 📄 License  
---
## ✨ Features
- 🧑‍🤝‍🧑 **User Accounts** – Email/password signup, Google & Facebook login  
- 🩸 **Blood Donor Network** – Register as a donor and manage your donor profile  
- 🧭 **Nearby Blood Points** – Map view to find nearby blood banks / donation camps  
- 📣 **Community Posts** – Admin can post blood donation campaigns and updates  
- 🆘 **Emergency SOS Widget** – Quick access to emergency contacts from the home screen  
- 🩺 **Requests & Management** – Create, view, and manage blood requests  
- 🏅 **Achievements** – Track donation history and achievements  
---
## 🛠️ Tech Stack
- **Platform**: Android (Java)  
- **Backend Services**:  
  - Firebase Authentication  
  - Firebase Realtime Database  
  - Firebase Storage / Cloudinary (for images)  
- **Maps & Location**: Google Maps / Places APIs, Overpass / HERE APIs  
- **Social Login**: Google Sign‑In, Facebook Login  
- **Build System**: Gradle  
---
## 📦 Installation
### Prerequisites
- Android Studio (latest stable)
- Android SDK & emulator or physical device
- Your own:
  - Firebase project
  - Google Maps / Places API keys
  - Facebook app (for login)
  - Cloudinary account (optional, for image hosting)
### Step 1: Clone the Repository
```bash
git clone https://github.com/<your-username>/DripBlood.git
cd DripBlood
Step 2: Open in Android Studio
File -> Open… and select the project folder.
Let Gradle sync and download dependencies.
⚙️ Configuration
Important: Real keys are not committed. You must add your own keys locally.

1. Firebase
Create a Firebase project.
Add an Android app with package name com.example.dripblood (or change the package in code and Firebase).
Download google-services.json and place it in:
app/google-services.json
This file is in .gitignore and will not be pushed to GitHub.

2. Google APIs (Maps / Places / Directions)
In these resource files:

app/src/main/res/values/strings.xml
app/src/main/res/res/values/strings.xml
app/src/main/res/values/google_maps_api.xml
app/src/main/res/res/values/google_maps_api.xml
Replace placeholders with your own keys:

<string name="google_maps_key">YOUR_GOOGLE_MAPS_API_KEY</string>
<string name="google_places_api_key">YOUR_GOOGLE_PLACES_API_KEY</string>
<string name="directions_api_key">YOUR_GOOGLE_DIRECTIONS_API_KEY</string>
<string name="web_client_id">YOUR_GOOGLE_WEB_CLIENT_ID</string>
<string name="default_web_client_id">YOUR_GOOGLE_WEB_CLIENT_ID</string>
3. Facebook Login
In strings.xml / res/res/values/strings.xml:

<string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
<string name="fb_login_protocol_scheme">fbYOUR_APP_ID</string>
<string name="facebook_client_token">YOUR_FACEBOOK_CLIENT_TOKEN</string>
Match these with your Facebook app settings and manifest configuration.

4. Cloudinary (optional, for images)
In:

ProfileActivity.java
CommunityActivity.java
set:

config.put("cloud_name", "YOUR_CLOUDINARY_CLOUD_NAME");
config.put("api_key", "YOUR_CLOUDINARY_API_KEY");
config.put("api_secret", "YOUR_CLOUDINARY_API_SECRET");
🚀 Getting Started
Configure Firebase, Google, Facebook, and Cloudinary as described above.
Connect a device or start an Android emulator.
In Android Studio, click Run ▶ to install and launch the app.
You can:

Register a new user or log in with Google/Facebook.
Set up your donor profile.
Explore nearby blood points on the map.
Use the SOS widget after adding it to your home screen.
🧱 Project Structure
DripBlood/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/dripblood/
│   │   │   │   ├── activities/        # Activities (Login, Dashboard, Map, Admin, etc.)
│   │   │   │   ├── fragments/         # Fragments (Home, Requests, Achievements, etc.)
│   │   │   │   └── widgets/           # SOS app widget
│   │   │   └── res/                   # Layouts, drawables, menus, values
│   │   └── androidTest/ test/         # Tests
│   └── build.gradle                   # Module Gradle config
│
├── .gitignore
├── build.gradle                        # Project Gradle config
└── README.md                           # This file
📖 Usage Guide
🧑‍💻 User Flow
Sign Up / Login
Create an account using email/password, Google, or Facebook.
Complete Profile
Add your name, blood group, address, phone number, and donor status.
Find Blood
Use the map to locate nearby camps and donors.
Create Requests
Post blood requests for specific divisions and blood groups.
Admin Features (if enabled)
Access admin dashboard to manage users, camps, and community posts.


🔐 Security & API Keys
✅ Real API keys, Firebase config, and secrets are not committed.
✅ app/google-services.json and key files (*.jks, *.keystore, *.pem, *.key) are in .gitignore.
✅ Example placeholders are used in code and should be replaced locally.
🔁 Rotate your API keys if they were ever previously committed anywhere public.
🤝 Contributing
Contributions are welcome!

Fork the repository
Create a feature branch:
git checkout -b feature/amazing-feature
Commit your changes:
git commit -m "Add amazing feature"
Push to the branch:
git push origin feature/amazing-feature
Open a Pull Request
📄 License
This project is licensed under the MIT License – see the LICENSE file for details.

Made with ❤️ to support life‑saving blood donations
