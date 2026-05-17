create or replace function public.platform_create_organization_for_admin(
  organization_name text,
  organization_city text default null,
  organization_contact_email text default null,
  organization_website_url text default null,
  organization_admin_user_id uuid default null,
  created_by_user_id uuid default null,
  organization_admin_full_name text default null
)
returns table (
  organization_id uuid,
  name text,
  invite_code text
)
language plpgsql
security definer
set search_path = public, auth
as $$
#variable_conflict use_column
declare
  created_organization_id uuid;
  created_invite_code text;
begin
  if nullif(trim(coalesce(organization_name, '')), '') is null then
    raise exception 'Organization name is required';
  end if;

  if organization_admin_user_id is null then
    raise exception 'Organization admin user is required';
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
    'verified',
    created_by_user_id
  )
  returning id into created_organization_id;

  insert into public.profiles (
    id,
    full_name,
    role,
    primary_organization_id
  )
  values (
    organization_admin_user_id,
    nullif(trim(coalesce(organization_admin_full_name, '')), ''),
    'user',
    created_organization_id
  )
  on conflict (id) do update
  set
    full_name = coalesce(nullif(trim(coalesce(excluded.full_name, '')), ''), public.profiles.full_name),
    primary_organization_id = created_organization_id,
    role = case
      when public.profiles.role = 'admin' then public.profiles.role
      else 'user'
    end;

  insert into public.organization_memberships (
    organization_id,
    user_id,
    role
  )
  values (
    created_organization_id,
    organization_admin_user_id,
    'org_admin'
  )
  on conflict on constraint organization_memberships_pkey do update
  set role = 'org_admin';

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
    created_by_user_id
  );

  return query
  select
    o.id,
    o.name,
    created_invite_code
  from public.organizations as o
  where o.id = created_organization_id;
end;
$$;

revoke all on function public.platform_create_organization_for_admin(text, text, text, text, uuid, uuid, text) from public;
revoke execute on function public.platform_create_organization_for_admin(text, text, text, text, uuid, uuid, text) from anon, authenticated;
grant execute on function public.platform_create_organization_for_admin(text, text, text, text, uuid, uuid, text) to service_role;
