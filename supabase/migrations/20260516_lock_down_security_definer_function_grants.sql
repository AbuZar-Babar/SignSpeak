revoke execute on function public.admin_create_organization(text, text, text, text) from public, anon;
revoke execute on function public.admin_rotate_organization_invite_code(uuid) from public, anon;
revoke execute on function public.join_organization_by_invite_code(text) from public, anon;
revoke execute on function public.admin_list_my_organizations() from public, anon;
revoke execute on function public.admin_list_organization_users(uuid) from public, anon;
revoke execute on function public.admin_get_organization_overview(uuid) from public, anon;
revoke execute on function public.admin_list_organization_top_signs(uuid, integer) from public, anon;
revoke execute on function public.admin_list_organization_daily_usage(uuid, integer) from public, anon;
revoke execute on function public.admin_list_complaints(text, text, text, uuid) from public, anon;

revoke execute on function public.generate_organization_invite_code(text) from public, anon, authenticated;
revoke execute on function public.guard_organization_status_update() from public, anon, authenticated;
revoke execute on function public.guard_profile_role_update() from public, anon, authenticated;
revoke execute on function public.sync_translation_history_organization() from public, anon, authenticated;
revoke execute on function public.handle_new_user() from public, anon, authenticated;

revoke execute on function public.is_admin_user() from public, anon;
revoke execute on function public.is_organization_admin(uuid) from public, anon;
revoke execute on function public.user_primary_organization(uuid) from public, anon;

grant execute on function public.admin_create_organization(text, text, text, text) to authenticated, service_role;
grant execute on function public.admin_rotate_organization_invite_code(uuid) to authenticated, service_role;
grant execute on function public.join_organization_by_invite_code(text) to authenticated, service_role;
grant execute on function public.admin_list_my_organizations() to authenticated, service_role;
grant execute on function public.admin_list_organization_users(uuid) to authenticated, service_role;
grant execute on function public.admin_get_organization_overview(uuid) to authenticated, service_role;
grant execute on function public.admin_list_organization_top_signs(uuid, integer) to authenticated, service_role;
grant execute on function public.admin_list_organization_daily_usage(uuid, integer) to authenticated, service_role;
grant execute on function public.admin_list_complaints(text, text, text, uuid) to authenticated, service_role;
grant execute on function public.is_admin_user() to authenticated, service_role;
grant execute on function public.is_organization_admin(uuid) to authenticated, service_role;
grant execute on function public.user_primary_organization(uuid) to authenticated, service_role;
grant execute on function public.generate_organization_invite_code(text) to service_role;
grant execute on function public.guard_organization_status_update() to service_role;
grant execute on function public.guard_profile_role_update() to service_role;
grant execute on function public.sync_translation_history_organization() to service_role;
grant execute on function public.handle_new_user() to service_role;
