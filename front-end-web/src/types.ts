export type ComplaintStatus = 'open' | 'reviewing' | 'resolved' | 'rejected';

export type ComplaintSource = 'prediction' | 'dictionary';

export type UserRole = 'user' | 'admin';

export type OrganizationStatus = 'unverified' | 'verified' | 'disabled';

export type OrganizationRole = 'platform_admin' | 'org_admin' | 'member';

export type OrganizationRow = {
  id: string;
  name: string;
  city: string | null;
  contact_email: string | null;
  website_url: string | null;
  status: OrganizationStatus;
  user_role: OrganizationRole;
  invite_code: string | null;
  created_at: string;
};

export type OrganizationUserRow = {
  user_id: string;
  full_name: string | null;
  email: string | null;
  role: OrganizationRole;
  joined_at: string;
  last_active_at: string | null;
  translation_count: number;
  complaint_count: number;
};

export type OrganizationOverviewRow = {
  registered_users: number;
  active_users_30d: number;
  translations_30d: number;
  complaints_30d: number;
};

export type OrganizationTopSignRow = {
  predicted_word_slug: string;
  usage_count: number;
};

export type OrganizationDailyUsageRow = {
  usage_date: string;
  translation_count: number;
};

export type ComplaintRow = {
  id: string;
  source_type: ComplaintSource;
  status: ComplaintStatus;
  note: string | null;
  admin_note: string | null;
  expected_word: string | null;
  reported_word_slug: string | null;
  created_at: string;
  updated_at: string;
  resolved_at: string | null;
  reporter_id: string;
  reporter_name: string | null;
  reporter_email: string | null;
  organization_id: string | null;
  organization_name: string | null;
  translation_history_id: string | null;
  history_predicted_word_slug: string | null;
  history_confidence: number | null;
  history_model_version: string | null;
  history_created_at: string | null;
  dictionary_entry_id: string | null;
  dictionary_slug: string | null;
  dictionary_english_word: string | null;
  dictionary_urdu_word: string | null;
  dictionary_category: string | null;
};
