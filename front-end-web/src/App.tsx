import { FormEvent, useEffect, useMemo, useState } from 'react';
import type { Session } from '@supabase/supabase-js';
import { supabase, supabaseConfigError } from './lib/supabase';
import type { ComplaintRow, ComplaintSource, ComplaintStatus, UserRole } from './types';

type StatusFilter = 'all' | ComplaintStatus;
type SourceFilter = 'all' | ComplaintSource;

const statusOptions: Array<{ label: string; value: StatusFilter }> = [
  { label: 'All statuses', value: 'all' },
  { label: 'Open', value: 'open' },
  { label: 'Reviewing', value: 'reviewing' },
  { label: 'Resolved', value: 'resolved' },
  { label: 'Rejected', value: 'rejected' },
];

const sourceOptions: Array<{ label: string; value: SourceFilter }> = [
  { label: 'All sources', value: 'all' },
  { label: 'Prediction', value: 'prediction' },
  { label: 'Dictionary', value: 'dictionary' },
];

const statusCards: ComplaintStatus[] = ['open', 'reviewing', 'resolved', 'rejected'];

function formatDate(value: string | null): string {
  if (!value) {
    return 'Not set';
  }

  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function titleCase(value: string): string {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function complaintHeadline(complaint: ComplaintRow): string {
  return (
    complaint.dictionary_english_word ||
    complaint.reported_word_slug ||
    complaint.expected_word ||
    'Unknown word'
  );
}

function reporterLabel(complaint: ComplaintRow): string {
  return complaint.reporter_name || complaint.reporter_email || 'Unknown reporter';
}

export default function App() {
  const [session, setSession] = useState<Session | null>(null);
  const [role, setRole] = useState<UserRole | null>(null);
  const [profileName, setProfileName] = useState('');
  const [roleLoading, setRoleLoading] = useState(true);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);
  const [authNotice, setAuthNotice] = useState<string | null>(null);

  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');
  const [sourceFilter, setSourceFilter] = useState<SourceFilter>('all');
  const [searchDraft, setSearchDraft] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  const [complaints, setComplaints] = useState<ComplaintRow[]>([]);
  const [selectedComplaintId, setSelectedComplaintId] = useState<string | null>(null);
  const [listLoading, setListLoading] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const [statusDraft, setStatusDraft] = useState<ComplaintStatus>('open');
  const [adminNoteDraft, setAdminNoteDraft] = useState('');
  const [saveBusy, setSaveBusy] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveNotice, setSaveNotice] = useState<string | null>(null);

  useEffect(() => {
    const client = supabase;
    if (!client) {
      setRoleLoading(false);
      return;
    }

    let mounted = true;

    const syncSession = async () => {
      const { data, error } = await client.auth.getSession();
      if (!mounted) {
        return;
      }

      if (error) {
        setAuthError(error.message);
        setRoleLoading(false);
        return;
      }

      setSession(data.session);
      if (!data.session) {
        setRoleLoading(false);
      }
    };

    void syncSession();

    const { data: authListener } = client.auth.onAuthStateChange((_event, nextSession) => {
      if (!mounted) {
        return;
      }

      setSession(nextSession);
      if (!nextSession) {
        setRole(null);
        setProfileName('');
        setComplaints([]);
        setSelectedComplaintId(null);
        setRoleLoading(false);
      }
    });

    return () => {
      mounted = false;
      authListener.subscription.unsubscribe();
    };
  }, []);

  useEffect(() => {
    const client = supabase;
    if (!client) {
      return;
    }

    if (!session?.user.id) {
      setRole(null);
      setProfileName('');
      setRoleLoading(false);
      return;
    }

    let cancelled = false;

    const loadProfile = async () => {
      setRoleLoading(true);
      setAuthError(null);

      const { data, error } = await client
        .from('profiles')
        .select('role, full_name')
        .eq('id', session.user.id)
        .single();

      if (cancelled) {
        return;
      }

      if (error) {
        setAuthError(error.message);
        setRole(null);
        setProfileName('');
        setRoleLoading(false);
        return;
      }

      setRole((data.role as UserRole | null) ?? null);
      setProfileName(data.full_name ?? '');
      setRoleLoading(false);
    };

    void loadProfile();

    return () => {
      cancelled = true;
    };
  }, [session?.user.id]);

  useEffect(() => {
    const client = supabase;
    if (!client || role !== 'admin') {
      return;
    }

    let cancelled = false;

    const loadComplaints = async () => {
      setListLoading(true);
      setListError(null);

      const { data, error } = await client.rpc('admin_list_complaints', {
        status_filter: statusFilter === 'all' ? null : statusFilter,
        source_filter: sourceFilter === 'all' ? null : sourceFilter,
        search_query: searchTerm.trim() || null,
      });

      if (cancelled) {
        return;
      }

      if (error) {
        setListError(error.message);
        setComplaints([]);
        setSelectedComplaintId(null);
        setListLoading(false);
        return;
      }

      const rows = (data ?? []) as ComplaintRow[];
      setComplaints(rows);
      setSelectedComplaintId((currentId) => {
        if (!rows.length) {
          return null;
        }

        return rows.some((row) => row.id === currentId) ? currentId : rows[0].id;
      });
      setListLoading(false);
    };

    void loadComplaints();

    return () => {
      cancelled = true;
    };
  }, [role, statusFilter, sourceFilter, searchTerm, refreshKey]);

  const selectedComplaint = useMemo(
    () => complaints.find((complaint) => complaint.id === selectedComplaintId) ?? null,
    [complaints, selectedComplaintId],
  );

  useEffect(() => {
    if (!selectedComplaint) {
      setStatusDraft('open');
      setAdminNoteDraft('');
      setSaveError(null);
      setSaveNotice(null);
      return;
    }

    setStatusDraft(selectedComplaint.status);
    setAdminNoteDraft(selectedComplaint.admin_note ?? '');
    setSaveError(null);
    setSaveNotice(null);
  }, [selectedComplaint?.id]);

  const complaintMetrics = useMemo(() => {
    return complaints.reduce<Record<ComplaintStatus, number>>(
      (accumulator, complaint) => {
        accumulator[complaint.status] += 1;
        return accumulator;
      },
      {
        open: 0,
        reviewing: 0,
        resolved: 0,
        rejected: 0,
      },
    );
  }, [complaints]);

  const pendingCount = complaintMetrics.open + complaintMetrics.reviewing;

  const handleLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!supabase) {
      return;
    }

    setAuthBusy(true);
    setAuthError(null);
    setAuthNotice(null);

    const { error } = await supabase.auth.signInWithPassword({
      email: email.trim(),
      password,
    });

    if (error) {
      setAuthError(error.message);
      setAuthBusy(false);
      return;
    }

    setPassword('');
    setAuthNotice('Signed in. Checking admin access.');
    setAuthBusy(false);
  };

  const handleResetPassword = async () => {
    if (!supabase) {
      return;
    }

    if (!email.trim()) {
      setAuthError('Enter an email address before requesting a reset link.');
      return;
    }

    setAuthBusy(true);
    setAuthError(null);
    setAuthNotice(null);

    const { error } = await supabase.auth.resetPasswordForEmail(email.trim());

    if (error) {
      setAuthError(error.message);
      setAuthBusy(false);
      return;
    }

    setAuthNotice('Password reset email sent.');
    setAuthBusy(false);
  };

  const handleSignOut = async () => {
    if (!supabase) {
      return;
    }

    await supabase.auth.signOut();
    setRole(null);
    setProfileName('');
    setComplaints([]);
    setSelectedComplaintId(null);
    setEmail('');
    setPassword('');
    setAuthNotice(null);
  };

  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSearchTerm(searchDraft.trim());
  };

  const clearFilters = () => {
    setStatusFilter('all');
    setSourceFilter('all');
    setSearchDraft('');
    setSearchTerm('');
  };

  const saveComplaint = async () => {
    if (!supabase || !selectedComplaint) {
      return;
    }

    setSaveBusy(true);
    setSaveError(null);
    setSaveNotice(null);

    const { error } = await supabase
      .from('complaints')
      .update({
        status: statusDraft,
        admin_note: adminNoteDraft.trim() || null,
      })
      .eq('id', selectedComplaint.id);

    if (error) {
      setSaveError(error.message);
      setSaveBusy(false);
      return;
    }

    setSaveNotice('Complaint updated.');
    setSaveBusy(false);
    setRefreshKey((value) => value + 1);
  };

  if (supabaseConfigError) {
    return (
      <main className="auth-shell">
        <section className="hero-panel">
          <p className="eyebrow">Configuration required</p>
          <h1>SignSpeak admin portal cannot start without Supabase keys.</h1>
          <p className="muted-text">
            Copy <code>.env.example</code> to <code>.env</code> and set the project URL and
            publishable key before loading the portal.
          </p>
          <p className="error-banner">{supabaseConfigError}</p>
        </section>
      </main>
    );
  }

  if (!session) {
    return (
      <main className="auth-shell">
        <section className="hero-panel">
          <p className="eyebrow">SignSpeak Control Desk</p>
          <h1>Resolve translation complaints before they become user trust issues.</h1>
          <p className="muted-text">
            Admins can review reporter context, inspect the suspected word, track prediction
            details, and send complaints through a clean workflow from open to resolved.
          </p>
          <div className="hero-metrics">
            <article>
              <strong>One place</strong>
              <span>Complaint queue, notes, and resolution state</span>
            </article>
            <article>
              <strong>Role-locked</strong>
              <span>Only accounts marked as admin in Supabase can enter</span>
            </article>
          </div>
        </section>

        <section className="auth-card">
          <p className="eyebrow">Admin login</p>
          <h2>Sign in with your Supabase account</h2>
          <form className="auth-form" onSubmit={handleLogin}>
            <label>
              Email
              <input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="admin@signspeak.pk"
              />
            </label>
            <label>
              Password
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Your password"
              />
            </label>
            <button type="submit" disabled={authBusy}>
              {authBusy ? 'Signing in...' : 'Sign in'}
            </button>
          </form>
          <button className="secondary-button" onClick={handleResetPassword} disabled={authBusy}>
            Send password reset email
          </button>
          {authError ? <p className="error-banner">{authError}</p> : null}
          {authNotice ? <p className="notice-banner">{authNotice}</p> : null}
          <p className="muted-text small-text">
            New accounts will still need <code>public.profiles.role = 'admin'</code> before the
            portal grants access.
          </p>
        </section>
      </main>
    );
  }

  if (roleLoading) {
    return (
      <main className="status-shell">
        <p className="eyebrow">Checking access</p>
        <h1>Loading your admin profile.</h1>
      </main>
    );
  }

  if (role !== 'admin') {
    return (
      <main className="status-shell">
        <p className="eyebrow">Access blocked</p>
        <h1>This account is not marked as an admin.</h1>
        <p className="muted-text">
          Promote the user in <code>public.profiles.role</code> and sign in again.
        </p>
        <div className="status-actions">
          <button onClick={handleSignOut}>Sign out</button>
        </div>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">SignSpeak Admin Portal</p>
          <h1>Complaint maintenance</h1>
        </div>
        <div className="topbar-actions">
          <div className="admin-summary">
            <span className="summary-label">Signed in as</span>
            <strong>{profileName || session.user.email}</strong>
            <span className="summary-subtle">{session.user.email}</span>
          </div>
          <button className="secondary-button" onClick={handleSignOut}>
            Sign out
          </button>
        </div>
      </header>

      <section className="stat-grid">
        <article className="hero-stat pending-stat">
          <span>Pending</span>
          <strong>{pendingCount}</strong>
          <small>Open and reviewing complaints</small>
        </article>
        {statusCards.map((status) => (
          <article key={status} className="hero-stat">
            <span>{titleCase(status)}</span>
            <strong>{complaintMetrics[status]}</strong>
            <small>{status === 'resolved' ? 'Completed items' : 'Current filtered items'}</small>
          </article>
        ))}
      </section>

      <section className="toolbar">
        <div className="toolbar-group">
          <label>
            Status
            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
            >
              {statusOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Source
            <select
              value={sourceFilter}
              onChange={(event) => setSourceFilter(event.target.value as SourceFilter)}
            >
              {sourceOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        <form className="search-form" onSubmit={handleSearchSubmit}>
          <input
            value={searchDraft}
            onChange={(event) => setSearchDraft(event.target.value)}
            placeholder="Search by word, note, name, or email"
          />
          <button type="submit">Search</button>
        </form>

        <div className="toolbar-group toolbar-actions">
          <button className="secondary-button" onClick={clearFilters}>
            Clear
          </button>
          <button className="secondary-button" onClick={() => setRefreshKey((value) => value + 1)}>
            Refresh
          </button>
        </div>
      </section>

      <section className="workspace">
        <aside className="list-panel">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Complaint queue</p>
              <h2>{complaints.length} results</h2>
            </div>
            {listLoading ? <span className="loading-chip">Loading...</span> : null}
          </div>

          {listError ? <p className="error-banner">{listError}</p> : null}

          <div className="complaint-list">
            {!listLoading && !complaints.length ? (
              <article className="empty-state">
                <h3>No complaints match the current filter.</h3>
                <p>Try a different status or remove the search query.</p>
              </article>
            ) : null}

            {complaints.map((complaint) => (
              <button
                key={complaint.id}
                className={`complaint-item ${complaint.id === selectedComplaintId ? 'active' : ''}`}
                onClick={() => setSelectedComplaintId(complaint.id)}
              >
                <div className="complaint-item-top">
                  <strong>{complaintHeadline(complaint)}</strong>
                  <span className={`status-pill status-${complaint.status}`}>{complaint.status}</span>
                </div>
                <p className="complaint-meta">
                  {reporterLabel(complaint)} · {titleCase(complaint.source_type)}
                </p>
                <p className="complaint-snippet">
                  {complaint.note || complaint.expected_word || 'No user note provided.'}
                </p>
                <p className="complaint-date">Created {formatDate(complaint.created_at)}</p>
              </button>
            ))}
          </div>
        </aside>

        <section className="detail-panel">
          {!selectedComplaint ? (
            <article className="empty-state detail-empty">
              <h3>Select a complaint to inspect it.</h3>
              <p>The detail panel will show reporter info, word context, and admin actions.</p>
            </article>
          ) : (
            <>
              <div className="panel-header">
                <div>
                  <p className="eyebrow">Complaint detail</p>
                  <h2>{complaintHeadline(selectedComplaint)}</h2>
                </div>
                <span className={`status-pill status-${selectedComplaint.status}`}>
                  {selectedComplaint.status}
                </span>
              </div>

              <div className="detail-grid">
                <article className="detail-card">
                  <h3>Reporter</h3>
                  <dl>
                    <div>
                      <dt>Name</dt>
                      <dd>{selectedComplaint.reporter_name || 'Not provided'}</dd>
                    </div>
                    <div>
                      <dt>Email</dt>
                      <dd>{selectedComplaint.reporter_email || 'Not available'}</dd>
                    </div>
                    <div>
                      <dt>Created</dt>
                      <dd>{formatDate(selectedComplaint.created_at)}</dd>
                    </div>
                    <div>
                      <dt>Updated</dt>
                      <dd>{formatDate(selectedComplaint.updated_at)}</dd>
                    </div>
                    <div>
                      <dt>Resolved</dt>
                      <dd>{formatDate(selectedComplaint.resolved_at)}</dd>
                    </div>
                  </dl>
                </article>

                <article className="detail-card">
                  <h3>Word context</h3>
                  <dl>
                    <div>
                      <dt>Source</dt>
                      <dd>{titleCase(selectedComplaint.source_type)}</dd>
                    </div>
                    <div>
                      <dt>Reported slug</dt>
                      <dd>{selectedComplaint.reported_word_slug || 'Not captured'}</dd>
                    </div>
                    <div>
                      <dt>Expected word</dt>
                      <dd>{selectedComplaint.expected_word || 'Not provided'}</dd>
                    </div>
                    <div>
                      <dt>Dictionary word</dt>
                      <dd>{selectedComplaint.dictionary_english_word || 'No linked entry'}</dd>
                    </div>
                    <div>
                      <dt>Urdu</dt>
                      <dd>{selectedComplaint.dictionary_urdu_word || 'No linked entry'}</dd>
                    </div>
                    <div>
                      <dt>Category</dt>
                      <dd>{selectedComplaint.dictionary_category || 'Uncategorized'}</dd>
                    </div>
                  </dl>
                </article>

                <article className="detail-card wide-card">
                  <h3>User note</h3>
                  <p>{selectedComplaint.note || 'No user note submitted.'}</p>
                </article>

                <article className="detail-card wide-card">
                  <h3>Prediction context</h3>
                  <dl className="prediction-grid">
                    <div>
                      <dt>History ID</dt>
                      <dd>{selectedComplaint.translation_history_id || 'Not linked'}</dd>
                    </div>
                    <div>
                      <dt>Predicted word</dt>
                      <dd>{selectedComplaint.history_predicted_word_slug || 'Not linked'}</dd>
                    </div>
                    <div>
                      <dt>Confidence</dt>
                      <dd>
                        {selectedComplaint.history_confidence != null
                          ? `${Math.round(selectedComplaint.history_confidence * 100)}%`
                          : 'Not captured'}
                      </dd>
                    </div>
                    <div>
                      <dt>Model version</dt>
                      <dd>{selectedComplaint.history_model_version || 'Not captured'}</dd>
                    </div>
                    <div>
                      <dt>Prediction time</dt>
                      <dd>{formatDate(selectedComplaint.history_created_at)}</dd>
                    </div>
                  </dl>
                </article>
              </div>

              <article className="detail-card editor-card">
                <div className="panel-header">
                  <div>
                    <p className="eyebrow">Admin action</p>
                    <h3>Maintain this complaint</h3>
                  </div>
                </div>

                <div className="editor-grid">
                  <label>
                    Status
                    <select
                      value={statusDraft}
                      onChange={(event) => setStatusDraft(event.target.value as ComplaintStatus)}
                    >
                      {statusOptions
                        .filter((option) => option.value !== 'all')
                        .map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                    </select>
                  </label>

                  <label className="full-width">
                    Admin note
                    <textarea
                      value={adminNoteDraft}
                      onChange={(event) => setAdminNoteDraft(event.target.value)}
                      placeholder="Add the reason for your decision, investigation results, or next steps."
                      rows={6}
                    />
                  </label>
                </div>

                <div className="editor-actions">
                  <button onClick={saveComplaint} disabled={saveBusy}>
                    {saveBusy ? 'Saving...' : 'Save changes'}
                  </button>
                  {saveError ? <p className="error-banner inline-banner">{saveError}</p> : null}
                  {saveNotice ? <p className="notice-banner inline-banner">{saveNotice}</p> : null}
                </div>
              </article>
            </>
          )}
        </section>
      </section>
    </main>
  );
}
