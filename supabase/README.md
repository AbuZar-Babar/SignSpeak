# SignSpeak Backend (Supabase)

This repository serves as the central backend and database hub for the SignSpeak ecosystem, facilitating interactions between the Android Kotlin Mobile App and the React Admin Portal.

## Purpose

The SignSpeak architecture offloads user authentication, real-time database transactions, and user complaint moderation to Supabase. This directory houses the essential SQL migrations and seed data necessary to initialize a fresh development or production environment.

## Database Core Elements
- **Authentication:** `profiles` integration tied to Supabase Auth.
- **Content:** `dictionary_entries` required for PSL reference in the mobile app.
- **Telemetry:** `translation_history` mapping predictions locally saved by users.
- **Moderation:** `complaints` mapped to admin resolution statuses.
- **Organizations:** verified institutes, org admins, student invite codes, and organization analytics.

## Setup Instructions

If you are setting up the SignSpeak architecture from scratch, please execute the SQL queries via your Supabase Dashboard SQL Editor in the exact order listed below.

### 1. Run Schema Migrations
Deploy the base schema, tables, and permissions:
1. Execute [`migrations/20260327_signspeak_v1.sql`](./migrations/20260327_signspeak_v1.sql)
2. Execute [`migrations/20260327_admin_complaints_portal.sql`](./migrations/20260327_admin_complaints_portal.sql) to add complaint portal helper functions.
3. Execute [`migrations/20260515_organization_accounts.sql`](./migrations/20260515_organization_accounts.sql) to add institute organizations, invite codes, rosters, and analytics.
4. Execute [`migrations/20260516_lock_down_security_definer_function_grants.sql`](./migrations/20260516_lock_down_security_definer_function_grants.sql) to restrict organization helper functions to signed-in users and service-role contexts.
5. Execute [`migrations/20260517_platform_org_admin_creation.sql`](./migrations/20260517_platform_org_admin_creation.sql) to add the service-role-only helper used by the admin account creation Edge Function.
6. Execute [`migrations/20260517_restrict_legacy_organization_creation.sql`](./migrations/20260517_restrict_legacy_organization_creation.sql) to prevent normal signed-in users from calling the legacy organization creation RPC.

### 2. Seed Initial Data
Populate the database with default dictionary content:
7. Execute [`seed.sql`](./seed.sql) to load the initial Pakistan Sign Language dictionary references.

### 3. Deploy Edge Functions
Deploy the organization admin creation function after the database migrations are applied:

```bash
supabase functions deploy admin-create-org-admin
```

The function requires the standard Supabase function environment values: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY`.

### 4. Setup Super Admin User
In order to grant access to the Admin Web Portal:
1. Create a standard user inside your Supabase **Authentication** dashboard (or register normally via the mobile app).
2. Promote that user by setting `public.profiles.role = 'admin'`.
3. Log in through the portal's **Super admin login** link to create verified school workspaces and organization admin setup links.

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
