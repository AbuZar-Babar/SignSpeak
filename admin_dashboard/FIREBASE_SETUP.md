# Firebase Setup Instructions

The admin dashboard requires Firebase configuration to enable authentication features.

## Setup Steps

1. **Create a `.env.local` file** in the `admin_dashboard` directory

2. **Add the following environment variables** with your Firebase project credentials:

```env
NEXT_PUBLIC_FIREBASE_API_KEY=your-api-key-here
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=your-project-id.firebaseapp.com
NEXT_PUBLIC_FIREBASE_PROJECT_ID=your-project-id
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=your-project-id.appspot.com
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=your-messaging-sender-id
NEXT_PUBLIC_FIREBASE_APP_ID=your-app-id
```

3. **Where to find these values:**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Select your project (or create a new one)
   - Go to Project Settings (gear icon) > General tab
   - Scroll down to "Your apps" section
   - If you don't have a web app, click "Add app" and select the web icon (</>)
   - Copy the config values from the `firebaseConfig` object

4. **Restart the development server** after creating/updating `.env.local`:
   ```bash
   npm run dev
   ```

## Note

- The app will work without Firebase configuration, but authentication features will be disabled
- The `.env.local` file is gitignored and should not be committed to version control
- Make sure all variables start with `NEXT_PUBLIC_` to be accessible in the browser

