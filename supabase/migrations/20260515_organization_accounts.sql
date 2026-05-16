create table if not exists public.organizations (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  city text,
  contact_email text,
  website_url text,
  status text not null default 'unverified'
    check (status in ('unverified', 'verified', 'disabled')),
  created_by uuid references auth.users (id) on delete set null,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.organization_memberships (
  organization_id uuid not null references public.organizations (id) on delete cascade,
  user_id uuid not null references auth.users (id) on delete cascade,
  role text not null default 'member'
    check (role in ('org_admin', 'member')),
  created_at timestamptz not null default timezone('utc', now()),
  primary key (organization_id, user_id)
);

create table if not exists public.organization_invite_codes (
  id uuid primary key default gen_random_uuid(),
  organization_id uuid not null references public.organizations (id) on delete cascade,
  code text not null unique,
  active boolean not null default true,
  created_by uuid references auth.users (id) on delete set null,
  created_at timestamptz not null default timezone('utc', now())
);

alter table public.profiles
  add column if not exists primary_organization_id uuid
  references public.organizations (id) on delete set null;

alter table public.translation_history
  add column if not exists organization_id uuid
  references public.organizations (id) on delete set null;

create index if not exists organizations_status_idx
  on public.organizations (status);
create index if not exists organizations_created_by_idx
  on public.organizations (created_by);
create index if not exists organization_memberships_user_idx
  on public.organization_memberships (user_id);
create index if not exists organization_invite_codes_org_idx
  on public.organization_invite_codes (organization_id);
create index if not exists translation_history_org_created_idx
  on public.translation_history (organization_id, created_at desc);

drop trigger if exists set_organizations_updated_at on public.organizations;
create trigger set_organizations_updated_at
before update on public.organizations
for each row
execute function public.set_updated_at();

create or replace function public.guard_profile_role_update()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_admin_user() and new.role is distinct from old.role then
    raise exception 'Only platform admins can change profile roles';
  end if;

  return new;
end;
$$;

drop trigger if exists guard_profile_role_update on public.profiles;
create trigger guard_profile_role_update
before update on public.profiles
for each row
execute function public.guard_profile_role_update();

create or replace function public.guard_organization_status_update()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_admin_user() and new.status is distinct from old.status then
    raise exception 'Only platform admins can change organization verification status';
  end if;

  return new;
end;
$$;

drop trigger if exists guard_organization_status_update on public.organizations;
create trigger guard_organization_status_update
before update on public.organizations
for each row
execute function public.guard_organization_status_update();

create or replace function public.is_organization_admin(target_organization_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.is_admin_user()
    or exists (
      select 1
      from public.organization_memberships
      where organization_id = target_organization_id
        and user_id = auth.uid()
        and role = 'org_admin'
    );
$$;

create or replace function public.user_primary_organization(target_user_id uuid)
returns uuid
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(
    (
      select p.primary_organization_id
      from public.profiles as p
      where p.id = target_user_id
        and exists (
          select 1
          from public.organization_memberships as m
          where m.user_id = target_user_id
            and m.organization_id = p.primary_organization_id
        )
    ),
    (
      select m.organization_id
      from public.organization_memberships as m
      where m.user_id = target_user_id
      order by
        case m.role when 'org_admin' then 0 else 1 end,
        m.created_at asc
      limit 1
    )
  );
$$;

create or replace function public.sync_translation_history_organization()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  new.organization_id := public.user_primary_organization(new.user_id);
  return new;
end;
$$;

drop trigger if exists sync_translation_history_organization on public.translation_history;
create trigger sync_translation_history_organization
before insert on public.translation_history
for each row
execute function public.sync_translation_history_organization();

alter table public.organizations enable row level security;
alter table public.organization_memberships enable row level security;
alter table public.organization_invite_codes enable row level security;

drop policy if exists "organizations_select_member_or_admin" on public.organizations;
create policy "organizations_select_member_or_admin"
on public.organizations
for select
to authenticated
using (
  public.is_admin_user()
  or exists (
    select 1
    from public.organization_memberships as m
    where m.organization_id = organizations.id
      and m.user_id = auth.uid()
  )
);

drop policy if exists "organizations_update_admin" on public.organizations;
create policy "organizations_update_admin"
on public.organizations
for update
to authenticated
using (public.is_organization_admin(id))
with check (public.is_organization_admin(id));

drop policy if exists "organization_memberships_select_scoped" on public.organization_memberships;
create policy "organization_memberships_select_scoped"
on public.organization_memberships
for select
to authenticated
using (
  user_id = auth.uid()
  or public.is_organization_admin(organization_id)
);

drop policy if exists "organization_invite_codes_select_admin" on public.organization_invite_codes;
create policy "organization_invite_codes_select_admin"
on public.organization_invite_codes
for select
to authenticated
using (public.is_organization_admin(organization_id));

drop policy if exists "organization_invite_codes_update_admin" on public.organization_invite_codes;
create policy "organization_invite_codes_update_admin"
on public.organization_invite_codes
for update
to authenticated
using (public.is_organization_admin(organization_id))
with check (public.is_organization_admin(organization_id));

drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own"
on public.profiles
for select
to authenticated
using (
  id = auth.uid()
  or public.is_admin_user()
  or exists (
    select 1
    from public.organization_memberships as admin_membership
    join public.organization_memberships as target_membership
      on target_membership.organization_id = admin_membership.organization_id
    where admin_membership.user_id = auth.uid()
      and admin_membership.role = 'org_admin'
      and target_membership.user_id = profiles.id
  )
);

drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own"
on public.profiles
for update
to authenticated
using (id = auth.uid() or public.is_admin_user())
with check (
  public.is_admin_user()
  or (
    id = auth.uid()
    and (
      primary_organization_id is null
      or exists (
        select 1
        from public.organization_memberships as m
        where m.user_id = auth.uid()
          and m.organization_id = primary_organization_id
      )
    )
  )
);

drop policy if exists "translation_history_select_own" on public.translation_history;
create policy "translation_history_select_own"
on public.translation_history
for select
to authenticated
using (
  user_id = auth.uid()
  or public.is_admin_user()
  or public.is_organization_admin(organization_id)
);

drop policy if exists "complaints_select_own" on public.complaints;
create policy "complaints_select_own"
on public.complaints
for select
to authenticated
using (
  user_id = auth.uid()
  or public.is_admin_user()
  or public.is_organization_admin(public.user_primary_organization(user_id))
);

drop policy if exists "complaints_update_admin" on public.complaints;
create policy "complaints_update_admin"
on public.complaints
for update
to authenticated
using (
  public.is_admin_user()
  or public.is_organization_admin(public.user_primary_organization(user_id))
)
with check (
  public.is_admin_user()
  or public.is_organization_admin(public.user_primary_organization(user_id))
);

create or replace function public.generate_organization_invite_code(organization_name text)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
  prefix text;
  candidate text;
begin
  prefix := upper(left(regexp_replace(coalesce(organization_name, 'ORG'), '[^a-zA-Z0-9]+', '', 'g'), 4));
  if prefix = '' then
    prefix := 'ORG';
  end if;

  loop
    candidate := prefix || '-' || upper(substr(encode(gen_random_bytes(4), 'hex'), 1, 6));
    exit when not exists (
      select 1
      from public.organization_invite_codes
      where code = candidate
    );
  end loop;

  return candidate;
end;
$$;

create or replace function public.admin_create_organization(
  organization_name text,
  organization_city text default null,
  organization_contact_email text default null,
  organization_website_url text default null
)
returns table (
  organization_id uuid,
  name text,
  city text,
  contact_email text,
  website_url text,
  status text,
  invite_code text
)
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  created_organization_id uuid;
  created_invite_code text;
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;

  if nullif(trim(organization_name), '') is null then
    raise exception 'Organization name is required';
  end if;

  insert into public.organizations (
    name,
    city,
    contact_email,
    website_url,
    status,
    created_by
  )
  values (
    trim(organization_name),
    nullif(trim(coalesce(organization_city, '')), ''),
    nullif(trim(coalesce(organization_contact_email, '')), ''),
    nullif(trim(coalesce(organization_website_url, '')), ''),
    'unverified',
    auth.uid()
  )
  returning id into created_organization_id;

  insert into public.organization_memberships (organization_id, user_id, role)
  values (created_organization_id, auth.uid(), 'org_admin')
  on conflict (organization_id, user_id) do update
  set role = 'org_admin';

  update public.profiles
  set primary_organization_id = created_organization_id
  where id = auth.uid()
    and primary_organization_id is null;

  created_invite_code := public.generate_organization_invite_code(organization_name);

  insert into public.organization_invite_codes (
    organization_id,
    code,
    active,
    created_by
  )
  values (
    created_organization_id,
    created_invite_code,
    true,
    auth.uid()
  );

  return query
  select
    o.id,
    o.name,
    o.city,
    o.contact_email,
    o.website_url,
    o.status,
    created_invite_code
  from public.organizations as o
  where o.id = created_organization_id;
end;
$$;

create or replace function public.admin_rotate_organization_invite_code(target_organization_id uuid)
returns table (
  organization_id uuid,
  invite_code text
)
language plpgsql
security definer
set search_path = public
as $$
declare
  organization_name text;
  created_invite_code text;
begin
  if not public.is_organization_admin(target_organization_id) then
    raise exception 'Organization admin access required';
  end if;

  select o.name
  into organization_name
  from public.organizations as o
  where o.id = target_organization_id;

  if organization_name is null then
    raise exception 'Organization not found';
  end if;

  update public.organization_invite_codes
  set active = false
  where organization_invite_codes.organization_id = target_organization_id
    and active = true;

  created_invite_code := public.generate_organization_invite_code(organization_name);

  insert into public.organization_invite_codes (
    organization_id,
    code,
    active,
    created_by
  )
  values (
    target_organization_id,
    created_invite_code,
    true,
    auth.uid()
  );

  return query
  select target_organization_id, created_invite_code;
end;
$$;

create or replace function public.join_organization_by_invite_code(invite_code text)
returns table (
  organization_id uuid,
  organization_name text,
  organization_role text,
  organization_status text
)
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  target_organization_id uuid;
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;

  select ic.organization_id
  into target_organization_id
  from public.organization_invite_codes as ic
  join public.organizations as o
    on o.id = ic.organization_id
  where upper(ic.code) = upper(trim(invite_code))
    and ic.active = true
    and o.status <> 'disabled'
  limit 1;

  if target_organization_id is null then
    raise exception 'Invalid or inactive institute code';
  end if;

  insert into public.organization_memberships (organization_id, user_id, role)
  values (target_organization_id, auth.uid(), 'member')
  on conflict (organization_id, user_id) do nothing;

  update public.profiles
  set primary_organization_id = target_organization_id
  where id = auth.uid();

  return query
  select
    o.id,
    o.name,
    m.role,
    o.status
  from public.organizations as o
  join public.organization_memberships as m
    on m.organization_id = o.id
    and m.user_id = auth.uid()
  where o.id = target_organization_id;
end;
$$;

create or replace function public.admin_list_my_organizations()
returns table (
  id uuid,
  name text,
  city text,
  contact_email text,
  website_url text,
  status text,
  user_role text,
  invite_code text,
  created_at timestamptz
)
language sql
stable
security definer
set search_path = public
as $$
  select
    o.id,
    o.name,
    o.city,
    o.contact_email,
    o.website_url,
    o.status,
    case
      when public.is_admin_user() then coalesce(m.role, 'platform_admin')
      else m.role
    end as user_role,
    ic.code as invite_code,
    o.created_at
  from public.organizations as o
  left join public.organization_memberships as m
    on m.organization_id = o.id
    and m.user_id = auth.uid()
  left join lateral (
    select code
    from public.organization_invite_codes
    where organization_id = o.id
      and active = true
    order by created_at desc
    limit 1
  ) as ic on true
  where public.is_admin_user()
    or m.user_id = auth.uid()
  order by o.created_at desc;
$$;

create or replace function public.admin_list_organization_users(target_organization_id uuid)
returns table (
  user_id uuid,
  full_name text,
  email text,
  role text,
  joined_at timestamptz,
  last_active_at timestamptz,
  translation_count bigint,
  complaint_count bigint
)
language plpgsql
stable
security definer
set search_path = public, auth
as $$
begin
  if not public.is_organization_admin(target_organization_id) then
    raise exception 'Organization admin access required';
  end if;

  return query
  select
    m.user_id,
    nullif(p.full_name, '') as full_name,
    u.email::text as email,
    m.role,
    m.created_at as joined_at,
    max(th.created_at) as last_active_at,
    count(distinct th.id) as translation_count,
    count(distinct c.id) as complaint_count
  from public.organization_memberships as m
  join auth.users as u
    on u.id = m.user_id
  left join public.profiles as p
    on p.id = m.user_id
  left join public.translation_history as th
    on th.user_id = m.user_id
    and th.organization_id = m.organization_id
  left join public.complaints as c
    on c.user_id = m.user_id
    and public.user_primary_organization(c.user_id) = m.organization_id
  where m.organization_id = target_organization_id
  group by
    m.user_id,
    p.full_name,
    u.email,
    m.role,
    m.created_at
  order by m.created_at desc;
end;
$$;

create or replace function public.admin_get_organization_overview(target_organization_id uuid)
returns table (
  registered_users bigint,
  active_users_30d bigint,
  translations_30d bigint,
  complaints_30d bigint
)
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  if not public.is_organization_admin(target_organization_id) then
    raise exception 'Organization admin access required';
  end if;

  return query
  select
    (
      select count(*)
      from public.organization_memberships as m
      where m.organization_id = target_organization_id
    ) as registered_users,
    (
      select count(distinct th.user_id)
      from public.translation_history as th
      where th.organization_id = target_organization_id
        and th.created_at >= timezone('utc', now()) - interval '30 days'
    ) as active_users_30d,
    (
      select count(*)
      from public.translation_history as th
      where th.organization_id = target_organization_id
        and th.created_at >= timezone('utc', now()) - interval '30 days'
    ) as translations_30d,
    (
      select count(*)
      from public.complaints as c
      where public.user_primary_organization(c.user_id) = target_organization_id
        and c.created_at >= timezone('utc', now()) - interval '30 days'
    ) as complaints_30d;
end;
$$;

create or replace function public.admin_list_organization_top_signs(
  target_organization_id uuid,
  result_limit integer default 8
)
returns table (
  predicted_word_slug text,
  usage_count bigint
)
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  if not public.is_organization_admin(target_organization_id) then
    raise exception 'Organization admin access required';
  end if;

  return query
  select
    th.predicted_word_slug,
    count(*) as usage_count
  from public.translation_history as th
  where th.organization_id = target_organization_id
    and th.created_at >= timezone('utc', now()) - interval '30 days'
  group by th.predicted_word_slug
  order by usage_count desc, th.predicted_word_slug asc
  limit greatest(1, least(coalesce(result_limit, 8), 25));
end;
$$;

create or replace function public.admin_list_organization_daily_usage(
  target_organization_id uuid,
  day_limit integer default 14
)
returns table (
  usage_date date,
  translation_count bigint
)
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  if not public.is_organization_admin(target_organization_id) then
    raise exception 'Organization admin access required';
  end if;

  return query
  select
    date_trunc('day', th.created_at)::date as usage_date,
    count(*) as translation_count
  from public.translation_history as th
  where th.organization_id = target_organization_id
    and th.created_at >= timezone('utc', now()) - make_interval(days => greatest(1, least(coalesce(day_limit, 14), 90)))
  group by date_trunc('day', th.created_at)::date
  order by usage_date asc;
end;
$$;

drop function if exists public.admin_list_complaints(text, text, text);
create or replace function public.admin_list_complaints(
  status_filter text default null,
  source_filter text default null,
  search_query text default null,
  organization_filter uuid default null
)
returns table (
  id uuid,
  source_type text,
  status text,
  note text,
  admin_note text,
  expected_word text,
  reported_word_slug text,
  created_at timestamptz,
  updated_at timestamptz,
  resolved_at timestamptz,
  reporter_id uuid,
  reporter_name text,
  reporter_email text,
  organization_id uuid,
  organization_name text,
  translation_history_id uuid,
  history_predicted_word_slug text,
  history_confidence numeric,
  history_model_version text,
  history_created_at timestamptz,
  dictionary_entry_id uuid,
  dictionary_slug text,
  dictionary_english_word text,
  dictionary_urdu_word text,
  dictionary_category text
)
language plpgsql
security definer
set search_path = public, auth
as $$
begin
  if not public.is_admin_user() then
    if organization_filter is null then
      raise exception 'Organization scope required';
    end if;

    if not public.is_organization_admin(organization_filter) then
      raise exception 'Organization admin access required';
    end if;
  end if;

  return query
  select
    c.id,
    c.source_type,
    c.status,
    c.note,
    c.admin_note,
    c.expected_word,
    c.reported_word_slug,
    c.created_at,
    c.updated_at,
    c.resolved_at,
    c.user_id as reporter_id,
    nullif(p.full_name, '') as reporter_name,
    u.email::text as reporter_email,
    org.id as organization_id,
    org.name as organization_name,
    th.id as translation_history_id,
    th.predicted_word_slug as history_predicted_word_slug,
    th.confidence as history_confidence,
    th.model_version as history_model_version,
    th.created_at as history_created_at,
    de.id as dictionary_entry_id,
    de.slug as dictionary_slug,
    de.english_word as dictionary_english_word,
    de.urdu_word as dictionary_urdu_word,
    de.category as dictionary_category
  from public.complaints as c
  left join public.profiles as p
    on p.id = c.user_id
  left join auth.users as u
    on u.id = c.user_id
  left join public.translation_history as th
    on th.id = c.translation_history_id
  left join public.organizations as org
    on org.id = coalesce(th.organization_id, public.user_primary_organization(c.user_id))
  left join public.dictionary_entries as de
    on de.id = c.dictionary_entry_id
    or (
      c.dictionary_entry_id is null
      and de.slug = c.reported_word_slug
    )
  where (organization_filter is null or org.id = organization_filter)
    and (status_filter is null or status_filter = '' or c.status = status_filter)
    and (source_filter is null or source_filter = '' or c.source_type = source_filter)
    and (
      search_query is null
      or search_query = ''
      or lower(coalesce(c.note, '')) like '%' || lower(search_query) || '%'
      or lower(coalesce(c.admin_note, '')) like '%' || lower(search_query) || '%'
      or lower(coalesce(c.expected_word, '')) like '%' || lower(search_query) || '%'
      or lower(coalesce(c.reported_word_slug, '')) like '%' || lower(search_query) || '%'
      or lower(coalesce(p.full_name, '')) like '%' || lower(search_query) || '%'
      or lower(coalesce(u.email, '')) like '%' || lower(search_query) || '%'
      or lower(coalesce(org.name, '')) like '%' || lower(search_query) || '%'
      or lower(coalesce(de.english_word, '')) like '%' || lower(search_query) || '%'
      or lower(coalesce(de.urdu_word, '')) like '%' || lower(search_query) || '%'
    )
  order by
    case c.status
      when 'open' then 0
      when 'reviewing' then 1
      when 'resolved' then 2
      when 'rejected' then 3
      else 4
    end,
    c.created_at desc;
end;
$$;

revoke all on function public.generate_organization_invite_code(text) from public;
revoke all on function public.admin_create_organization(text, text, text, text) from public;
revoke all on function public.admin_rotate_organization_invite_code(uuid) from public;
revoke all on function public.join_organization_by_invite_code(text) from public;
revoke all on function public.admin_list_my_organizations() from public;
revoke all on function public.admin_list_organization_users(uuid) from public;
revoke all on function public.admin_get_organization_overview(uuid) from public;
revoke all on function public.admin_list_organization_top_signs(uuid, integer) from public;
revoke all on function public.admin_list_organization_daily_usage(uuid, integer) from public;
revoke all on function public.admin_list_complaints(text, text, text, uuid) from public;

grant execute on function public.admin_create_organization(text, text, text, text) to authenticated;
grant execute on function public.admin_rotate_organization_invite_code(uuid) to authenticated;
grant execute on function public.join_organization_by_invite_code(text) to authenticated;
grant execute on function public.admin_list_my_organizations() to authenticated;
grant execute on function public.admin_list_organization_users(uuid) to authenticated;
grant execute on function public.admin_get_organization_overview(uuid) to authenticated;
grant execute on function public.admin_list_organization_top_signs(uuid, integer) to authenticated;
grant execute on function public.admin_list_organization_daily_usage(uuid, integer) to authenticated;
grant execute on function public.admin_list_complaints(text, text, text, uuid) to authenticated;
