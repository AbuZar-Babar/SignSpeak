# SignSpeak Supabase Setup

1. Run `supabase/migrations/20260327_signspeak_v1.sql` in Supabase SQL Editor.
2. Run `supabase/migrations/20260327_admin_complaints_portal.sql` for the admin complaint portal helpers.
3. Run `supabase/seed.sql` to load the initial dictionary.
4. Create one admin user in Supabase Auth, then update `public.profiles.role` to `admin` for that user's `id`.
5. Set `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` for the Android app before building.
6. Set `VITE_SUPABASE_URL` and `VITE_SUPABASE_PUBLISHABLE_KEY` for `front-end-web/` before running the admin portal.
