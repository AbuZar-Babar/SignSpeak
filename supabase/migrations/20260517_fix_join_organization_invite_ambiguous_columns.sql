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
#variable_conflict use_column
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

  insert into public.organization_memberships (
    organization_id,
    user_id,
    role
  )
  values (
    target_organization_id,
    auth.uid(),
    'member'
  )
  on conflict on constraint organization_memberships_pkey do nothing;

  update public.profiles
  set primary_organization_id = target_organization_id
  where profiles.id = auth.uid();

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

revoke all on function public.join_organization_by_invite_code(text) from public;
grant execute on function public.join_organization_by_invite_code(text) to authenticated;
