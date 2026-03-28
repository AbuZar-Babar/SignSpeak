# SignSpeak Admin Portal

React + Vite admin portal for reviewing and resolving SignSpeak complaints.

## Run

1. Copy `.env.example` to `.env`.
2. Run `npm install`.
3. Run `npm run dev`.

## Required Environment Variables

- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_PUBLISHABLE_KEY`

## Backend Requirements

Run these SQL files against the same Supabase project:

1. `supabase/migrations/20260327_signspeak_v1.sql`
2. `supabase/migrations/20260327_admin_complaints_portal.sql`
3. `supabase/seed.sql`

## Admin Access

1. Create a user in Supabase Auth or through the mobile app.
2. Promote that user by setting `public.profiles.role = 'admin'`.
3. Log into the portal with that account.
