export type ComplaintStatus = 'open' | 'reviewing' | 'resolved' | 'rejected';

export type ComplaintSource = 'prediction' | 'dictionary';

export type UserRole = 'user' | 'admin';

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
