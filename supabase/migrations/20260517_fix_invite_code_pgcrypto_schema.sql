create or replace function public.generate_organization_invite_code(organization_name text)
returns text
language plpgsql
security definer
set search_path = public, extensions
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
    candidate := prefix || '-' || upper(substr(encode(extensions.gen_random_bytes(4), 'hex'), 1, 6));
    exit when not exists (
      select 1
      from public.organization_invite_codes as invite_codes
      where invite_codes.code = candidate
    );
  end loop;

  return candidate;
end;
$$;

revoke all on function public.generate_organization_invite_code(text) from public;
revoke execute on function public.generate_organization_invite_code(text) from anon, authenticated;
grant execute on function public.generate_organization_invite_code(text) to service_role;
