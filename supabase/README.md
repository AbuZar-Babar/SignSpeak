# SignSpeak Backend (Supabase)

This repository serves as the central backend and database hub for the SignSpeak ecosystem, facilitating interactions between the Android Kotlin Mobile App and the React Admin Portal.

## Purpose

The SignSpeak architecture offloads user authentication, real-time database transactions, and user complaint moderation to Supabase. This directory houses the essential SQL migrations and seed data necessary to initialize a fresh development or production environment.

## Database Core Elements
- **Authentication:** `profiles` integration tied to Supabase Auth.
- **Content:** `dictionary_entries` required for PSL reference in the mobile app.
- **Telemetry:** `translation_history` mapping predictions locally saved by users.
- **Moderation:** `complaints` mapped to admin resolution statuses.

## Setup Instructions

If you are setting up the SignSpeak architecture from scratch, please execute the SQL queries via your Supabase Dashboard SQL Editor in the exact order listed below.

### 1. Run Schema Migrations
Deploy the base schema, tables, and permissions:
1. Execute [`migrations/20260327_signspeak_v1.sql`](./migrations/20260327_signspeak_v1.sql)
2. Execute [`migrations/20260327_admin_complaints_portal.sql`](./migrations/20260327_admin_complaints_portal.sql) to add complaint portal helper functions.

### 2. Seed Initial Data
Populate the database with default dictionary content:
3. Execute [`seed.sql`](./seed.sql) to load the initial Pakistan Sign Language dictionary references.

### 3. Setup Admin User
In order to grant access to the Admin Web Portal:
1. Create a standard user inside your Supabase **Authentication** dashboard (or register normally via the mobile app).
2. Promote that user by setting `public.profiles.role = 'admin'`.

## Client App Configuration

> **CRITICAL WARNING:** The API keys provided to **both** the React Admin Web Portal and the Kotlin Mobile App **MUST** point to the exact same Supabase project! If the keys differ, the ecosystem will break (e.g., complaints filed from the app will not appear in the web portal).

Once your database is created and seeded, provide your connection keys (the **URL** and **Anon/Publishable Key**) to the respective client applications.

**For the Admin Portal (`front-end-web`):**
```env
VITE_SUPABASE_URL=your_project_url
VITE_SUPABASE_PUBLISHABLE_KEY=your_anon_key
```

**For the Android App (`kotlin app`):**
```properties
SUPABASE_URL=your_project_url
SUPABASE_PUBLISHABLE_KEY=your_anon_key
```
