revoke execute on function public.admin_create_organization(text, text, text, text) from public, anon, authenticated;
grant execute on function public.admin_create_organization(text, text, text, text) to service_role;
