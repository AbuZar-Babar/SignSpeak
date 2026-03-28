create or replace function public.sync_complaint_resolution()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  if new.status in ('resolved', 'rejected') then
    if old.status is distinct from new.status or new.resolved_at is null then
      new.resolved_at = timezone('utc', now());
    end if;
  else
    new.resolved_at = null;
  end if;

  return new;
end;
$$;

drop trigger if exists sync_complaint_resolution on public.complaints;
create trigger sync_complaint_resolution
before update on public.complaints
for each row
execute function public.sync_complaint_resolution();

create or replace function public.admin_list_complaints(
  status_filter text default null,
  source_filter text default null,
  search_query text default null
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
    raise exception 'Admin access required';
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
  left join public.dictionary_entries as de
    on de.id = c.dictionary_entry_id
    or (
      c.dictionary_entry_id is null
      and de.slug = c.reported_word_slug
    )
  where (status_filter is null or status_filter = '' or c.status = status_filter)
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

revoke all on function public.admin_list_complaints(text, text, text) from public;
grant execute on function public.admin_list_complaints(text, text, text) to authenticated;
