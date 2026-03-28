create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  full_name text,
  role text not null default 'user' check (role in ('user', 'admin')),
  created_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.dictionary_entries (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  english_word text not null,
  urdu_word text not null,
  category text not null default 'General',
  external_url text,
  review_status text not null default 'verified'
    check (review_status in ('verified', 'needs_review')),
  is_active boolean not null default true,
  sort_order integer not null default 0,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.user_bookmarks (
  user_id uuid not null references auth.users (id) on delete cascade,
  dictionary_entry_id uuid not null references public.dictionary_entries (id) on delete cascade,
  created_at timestamptz not null default timezone('utc', now()),
  primary key (user_id, dictionary_entry_id)
);

create table if not exists public.translation_history (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  predicted_word_slug text not null,
  confidence numeric(5,4) not null check (confidence >= 0 and confidence <= 1),
  model_version text not null,
  source text not null default 'on_device',
  created_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.complaints (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  source_type text not null check (source_type in ('prediction', 'dictionary')),
  translation_history_id uuid references public.translation_history (id) on delete set null,
  dictionary_entry_id uuid references public.dictionary_entries (id) on delete set null,
  reported_word_slug text,
  expected_word text,
  note text,
  status text not null default 'open'
    check (status in ('open', 'reviewing', 'resolved', 'rejected')),
  admin_note text,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now()),
  resolved_at timestamptz
);

create index if not exists dictionary_entries_sort_order_idx
  on public.dictionary_entries (sort_order);
create index if not exists dictionary_entries_category_idx
  on public.dictionary_entries (category);
create index if not exists dictionary_entries_english_word_idx
  on public.dictionary_entries (lower(english_word));
create index if not exists dictionary_entries_urdu_word_idx
  on public.dictionary_entries (lower(urdu_word));
create index if not exists translation_history_user_created_idx
  on public.translation_history (user_id, created_at desc);
create index if not exists complaints_user_created_idx
  on public.complaints (user_id, created_at desc);
create index if not exists complaints_status_idx
  on public.complaints (status);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.updated_at = timezone('utc', now());
  return new;
end;
$$;

drop trigger if exists set_dictionary_entries_updated_at on public.dictionary_entries;
create trigger set_dictionary_entries_updated_at
before update on public.dictionary_entries
for each row
execute function public.set_updated_at();

drop trigger if exists set_complaints_updated_at on public.complaints;
create trigger set_complaints_updated_at
before update on public.complaints
for each row
execute function public.set_updated_at();

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, full_name)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', '')
  )
  on conflict (id) do update
  set full_name = excluded.full_name;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row
execute function public.handle_new_user();

insert into public.profiles (id, full_name)
select
  users.id,
  coalesce(users.raw_user_meta_data ->> 'full_name', '')
from auth.users as users
on conflict (id) do nothing;

create or replace function public.is_admin_user()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.profiles
    where id = auth.uid()
      and role = 'admin'
  );
$$;

alter table public.profiles enable row level security;
alter table public.dictionary_entries enable row level security;
alter table public.user_bookmarks enable row level security;
alter table public.translation_history enable row level security;
alter table public.complaints enable row level security;

drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own"
on public.profiles
for select
to authenticated
using (id = auth.uid() or public.is_admin_user());

drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own"
on public.profiles
for update
to authenticated
using (id = auth.uid() or public.is_admin_user())
with check (id = auth.uid() or public.is_admin_user());

drop policy if exists "dictionary_entries_public_read" on public.dictionary_entries;
create policy "dictionary_entries_public_read"
on public.dictionary_entries
for select
to anon, authenticated
using (is_active = true);

drop policy if exists "dictionary_entries_admin_insert" on public.dictionary_entries;
create policy "dictionary_entries_admin_insert"
on public.dictionary_entries
for insert
to authenticated
with check (public.is_admin_user());

drop policy if exists "dictionary_entries_admin_update" on public.dictionary_entries;
create policy "dictionary_entries_admin_update"
on public.dictionary_entries
for update
to authenticated
using (public.is_admin_user())
with check (public.is_admin_user());

drop policy if exists "dictionary_entries_admin_delete" on public.dictionary_entries;
create policy "dictionary_entries_admin_delete"
on public.dictionary_entries
for delete
to authenticated
using (public.is_admin_user());

drop policy if exists "user_bookmarks_select_own" on public.user_bookmarks;
create policy "user_bookmarks_select_own"
on public.user_bookmarks
for select
to authenticated
using (user_id = auth.uid());

drop policy if exists "user_bookmarks_insert_own" on public.user_bookmarks;
create policy "user_bookmarks_insert_own"
on public.user_bookmarks
for insert
to authenticated
with check (user_id = auth.uid());

drop policy if exists "user_bookmarks_delete_own" on public.user_bookmarks;
create policy "user_bookmarks_delete_own"
on public.user_bookmarks
for delete
to authenticated
using (user_id = auth.uid());

drop policy if exists "translation_history_select_own" on public.translation_history;
create policy "translation_history_select_own"
on public.translation_history
for select
to authenticated
using (user_id = auth.uid() or public.is_admin_user());

drop policy if exists "translation_history_insert_own" on public.translation_history;
create policy "translation_history_insert_own"
on public.translation_history
for insert
to authenticated
with check (user_id = auth.uid());

drop policy if exists "translation_history_delete_admin" on public.translation_history;
create policy "translation_history_delete_admin"
on public.translation_history
for delete
to authenticated
using (public.is_admin_user());

drop policy if exists "complaints_select_own" on public.complaints;
create policy "complaints_select_own"
on public.complaints
for select
to authenticated
using (user_id = auth.uid() or public.is_admin_user());

drop policy if exists "complaints_insert_own" on public.complaints;
create policy "complaints_insert_own"
on public.complaints
for insert
to authenticated
with check (user_id = auth.uid());

drop policy if exists "complaints_update_admin" on public.complaints;
create policy "complaints_update_admin"
on public.complaints
for update
to authenticated
using (public.is_admin_user())
with check (public.is_admin_user());
