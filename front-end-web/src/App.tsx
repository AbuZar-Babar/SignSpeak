import { FormEvent, useEffect, useMemo, useState } from 'react';
import type { Session } from '@supabase/supabase-js';
import { supabase, supabaseConfigError } from './lib/supabase';
import type {
  ComplaintRow,
  ComplaintSource,
  ComplaintStatus,
  OrganizationDailyUsageRow,
  OrganizationOverviewRow,
  OrganizationRow,
  OrganizationTopSignRow,
  OrganizationUserRow,
  UserRole,
} from './types';

type StatusFilter = 'all' | ComplaintStatus;
type SourceFilter = 'all' | ComplaintSource;
type ThemeMode = 'light' | 'dark';

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

const navItems = [
  { icon: 'dashboard', label: 'Dashboard', active: false },
  { icon: 'forum', label: 'Complaints', active: true },
  { icon: 'sign_language', label: 'Interpreters', active: false },
  { icon: 'leaderboard', label: 'Analytics', active: false },
  { icon: 'history', label: 'Logs', active: false },
] as const satisfies ReadonlyArray<{ icon: string; label: string; active?: boolean }>;

const themeStorageKey = 'signspeak-admin-theme';

const statusMeta: Record<
  ComplaintStatus,
  { label: string; icon: string; metricHint: string }
> = {
  open: {
    label: 'Open',
    icon: 'mark_email_unread',
    metricHint: 'Untriaged complaints',
  },
  reviewing: {
    label: 'Reviewing',
    icon: 'visibility',
    metricHint: 'Needs moderator action',
  },
  resolved: {
    label: 'Resolved',
    icon: 'check_circle',
    metricHint: 'Closed successfully',
  },
  rejected: {
    label: 'Rejected',
    icon: 'cancel',
    metricHint: 'Dismissed reports',
  },
};

const sourceMeta: Record<ComplaintSource, { label: string; icon: string }> = {
  prediction: { label: 'Prediction', icon: 'video_camera_front' },
  dictionary: { label: 'Dictionary', icon: 'menu_book' },
};

function formatDate(value: string | null): string {
  if (!value) {
    return 'Not set';
  }

  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function formatRelativeDate(value: string): string {
  const deltaMs = Date.now() - new Date(value).getTime();
  const deltaMinutes = Math.max(1, Math.round(deltaMs / 60000));

  if (deltaMinutes < 60) {
    return `${deltaMinutes}m ago`;
  }

  const deltaHours = Math.round(deltaMinutes / 60);
  if (deltaHours < 24) {
    return `${deltaHours}h ago`;
  }

  const deltaDays = Math.round(deltaHours / 24);
  return `${deltaDays}d ago`;
}

function complaintHeadline(complaint: ComplaintRow): string {
  return (
    complaint.dictionary_english_word ||
    complaint.reported_word_slug ||
    complaint.expected_word ||
    'Unknown word'
  );
}

function complaintSnippet(complaint: ComplaintRow): string {
  return complaint.note || complaint.expected_word || 'No user note provided.';
}

function reporterLabel(complaint: ComplaintRow): string {
  return complaint.reporter_name || complaint.reporter_email || 'Unknown reporter';
}

function initialsFor(complaint: ComplaintRow): string {
  const source = reporterLabel(complaint).replace(/[^a-zA-Z0-9 ]/g, ' ');
  const parts = source
    .split(' ')
    .map((part) => part.trim())
    .filter(Boolean)
    .slice(0, 2);

  if (!parts.length) {
    return 'SS';
  }

  return parts.map((part) => part[0]?.toUpperCase() ?? '').join('');
}

function confidenceLabel(value: number | null): string {
  return value == null ? 'Not captured' : `${Math.round(value * 100)}%`;
}

function modelVersionLabel(value: string | null): string {
  return value || 'Unavailable';
}

function complaintPriority(status: ComplaintStatus): string {
  switch (status) {
    case 'reviewing':
      return 'High';
    case 'open':
      return 'Medium';
    case 'resolved':
      return 'Resolved';
    case 'rejected':
      return 'Closed';
    default:
      return 'Normal';
  }
}

function metricDelta(status: ComplaintStatus): string {
  switch (status) {
    case 'open':
      return 'Intake';
    case 'reviewing':
      return 'Active';
    case 'resolved':
      return 'Stable';
    case 'rejected':
      return 'Closed';
    default:
      return '';
  }
}

function getInitialTheme(): ThemeMode {
  if (typeof window === 'undefined') {
    return 'light';
  }

  const stored = window.localStorage.getItem(themeStorageKey);
  if (stored === 'light' || stored === 'dark') {
    return stored;
  }

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function Icon({
  name,
  className,
}: {
  name: string;
  className?: string;
}) {
  return (
    <span aria-hidden="true" className={`material-symbols-outlined ${className ?? ''}`.trim()}>
      {name}
    </span>
  );
}

function StatusBadge({ status }: { status: ComplaintStatus }) {
  const meta = statusMeta[status];

  return (
    <span className={`status-badge status-${status}`}>
      <Icon className="status-badge-icon" name={meta.icon} />
      {meta.label}
    </span>
  );
}

function PortalBrand() {
  return (
    <div className="portal-brand">
      <div className="portal-brand-mark">
        <Icon name="sign_language" />
      </div>
      <div>
        <h1>SignSpeak</h1>
        <p>Admin Portal</p>
      </div>
    </div>
  );
}

export default function App() {
  const [theme, setTheme] = useState<ThemeMode>(getInitialTheme);
  const [session, setSession] = useState<Session | null>(null);
  const [role, setRole] = useState<UserRole | null>(null);
  const [profileName, setProfileName] = useState('');
  const [roleLoading, setRoleLoading] = useState(true);
  const [organizations, setOrganizations] = useState<OrganizationRow[]>([]);
  const [selectedOrganizationId, setSelectedOrganizationId] = useState<string>('');
  const [organizationsLoading, setOrganizationsLoading] = useState(false);
  const [organizationsError, setOrganizationsError] = useState<string | null>(null);
  const [organizationRefreshKey, setOrganizationRefreshKey] = useState(0);
  const [organizationName, setOrganizationName] = useState('');
  const [organizationCity, setOrganizationCity] = useState('');
  const [organizationEmail, setOrganizationEmail] = useState('');
  const [organizationWebsite, setOrganizationWebsite] = useState('');
  const [organizationBusy, setOrganizationBusy] = useState(false);
  const [organizationNotice, setOrganizationNotice] = useState<string | null>(null);
  const [organizationUsers, setOrganizationUsers] = useState<OrganizationUserRow[]>([]);
  const [organizationOverview, setOrganizationOverview] =
    useState<OrganizationOverviewRow | null>(null);
  const [organizationTopSigns, setOrganizationTopSigns] = useState<OrganizationTopSignRow[]>([]);
  const [organizationDailyUsage, setOrganizationDailyUsage] = useState<OrganizationDailyUsageRow[]>(
    [],
  );
  const [organizationDataLoading, setOrganizationDataLoading] = useState(false);
  const [organizationDataError, setOrganizationDataError] = useState<string | null>(null);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
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
    document.documentElement.classList.toggle('theme-dark', theme === 'dark');
    document.documentElement.style.colorScheme = theme;
    window.localStorage.setItem(themeStorageKey, theme);
  }, [theme]);

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
        setOrganizations([]);
        setSelectedOrganizationId('');
        setOrganizationUsers([]);
        setOrganizationOverview(null);
        setOrganizationTopSigns([]);
        setOrganizationDailyUsage([]);
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
    if (!client || !session?.user.id || roleLoading) {
      return;
    }

    let cancelled = false;

    const loadOrganizations = async () => {
      setOrganizationsLoading(true);
      setOrganizationsError(null);

      const { data, error } = await client.rpc('admin_list_my_organizations');

      if (cancelled) {
        return;
      }

      if (error) {
        setOrganizations([]);
        setSelectedOrganizationId('');
        setOrganizationsError(error.message);
        setOrganizationsLoading(false);
        return;
      }

      const rows = (data ?? []) as OrganizationRow[];
      setOrganizations(rows);
      setSelectedOrganizationId((currentId) => {
        if (!rows.length) {
          return '';
        }

        return rows.some((row) => row.id === currentId) ? currentId : rows[0].id;
      });
      setOrganizationsLoading(false);
    };

    void loadOrganizations();

    return () => {
      cancelled = true;
    };
  }, [session?.user.id, roleLoading, organizationRefreshKey]);

  useEffect(() => {
    const client = supabase;
    if (!client || !session?.user.id || roleLoading) {
      return;
    }

    if (role !== 'admin' && !selectedOrganizationId) {
      setComplaints([]);
      setSelectedComplaintId(null);
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
        organization_filter: selectedOrganizationId || null,
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
  }, [
    role,
    roleLoading,
    selectedOrganizationId,
    session?.user.id,
    statusFilter,
    sourceFilter,
    searchTerm,
    refreshKey,
  ]);

  const selectedComplaint = useMemo(
    () => complaints.find((complaint) => complaint.id === selectedComplaintId) ?? null,
    [complaints, selectedComplaintId],
  );

  const selectedOrganization = useMemo(
    () => organizations.find((organization) => organization.id === selectedOrganizationId) ?? null,
    [organizations, selectedOrganizationId],
  );

  useEffect(() => {
    const client = supabase;
    if (!client || !selectedOrganizationId) {
      setOrganizationUsers([]);
      setOrganizationOverview(null);
      setOrganizationTopSigns([]);
      setOrganizationDailyUsage([]);
      setOrganizationDataLoading(false);
      return;
    }

    let cancelled = false;

    const loadOrganizationData = async () => {
      setOrganizationDataLoading(true);
      setOrganizationDataError(null);

      const [overviewResult, usersResult, topSignsResult, dailyUsageResult] = await Promise.all([
        client.rpc('admin_get_organization_overview', {
          target_organization_id: selectedOrganizationId,
        }),
        client.rpc('admin_list_organization_users', {
          target_organization_id: selectedOrganizationId,
        }),
        client.rpc('admin_list_organization_top_signs', {
          target_organization_id: selectedOrganizationId,
          result_limit: 8,
        }),
        client.rpc('admin_list_organization_daily_usage', {
          target_organization_id: selectedOrganizationId,
          day_limit: 14,
        }),
      ]);

      if (cancelled) {
        return;
      }

      const firstError =
        overviewResult.error ||
        usersResult.error ||
        topSignsResult.error ||
        dailyUsageResult.error;

      if (firstError) {
        setOrganizationUsers([]);
        setOrganizationOverview(null);
        setOrganizationTopSigns([]);
        setOrganizationDailyUsage([]);
        setOrganizationDataError(firstError.message);
        setOrganizationDataLoading(false);
        return;
      }

      setOrganizationOverview(
        ((overviewResult.data ?? []) as OrganizationOverviewRow[])[0] ?? null,
      );
      setOrganizationUsers((usersResult.data ?? []) as OrganizationUserRow[]);
      setOrganizationTopSigns((topSignsResult.data ?? []) as OrganizationTopSignRow[]);
      setOrganizationDailyUsage((dailyUsageResult.data ?? []) as OrganizationDailyUsageRow[]);
      setOrganizationDataLoading(false);
    };

    void loadOrganizationData();

    return () => {
      cancelled = true;
    };
  }, [selectedOrganizationId, organizationRefreshKey, refreshKey]);

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
  const filteredResultLabel =
    complaints.length === 1 ? '1 complaint in scope' : `${complaints.length} complaints in scope`;
  const maxDailyTranslations = useMemo(
    () =>
      organizationDailyUsage.reduce(
        (maxValue, item) => Math.max(maxValue, Number(item.translation_count)),
        0,
      ),
    [organizationDailyUsage],
  );

  const toggleTheme = () => {
    setTheme((current) => (current === 'light' ? 'dark' : 'light'));
  };

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

  const handleCreateOrganization = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!supabase) {
      return;
    }

    if (!organizationName.trim()) {
      setOrganizationsError('Enter an institute name.');
      return;
    }

    setOrganizationBusy(true);
    setOrganizationsError(null);
    setOrganizationNotice(null);

    const { data, error } = await supabase.rpc('admin_create_organization', {
      organization_name: organizationName.trim(),
      organization_city: organizationCity.trim() || null,
      organization_contact_email: organizationEmail.trim() || null,
      organization_website_url: organizationWebsite.trim() || null,
    });

    if (error) {
      setOrganizationsError(error.message);
      setOrganizationBusy(false);
      return;
    }

    const created = ((data ?? []) as Array<{ organization_id: string; invite_code: string }>)[0];
    setOrganizationName('');
    setOrganizationCity('');
    setOrganizationEmail('');
    setOrganizationWebsite('');
    setOrganizationNotice(
      created?.invite_code
        ? `Organization created. Invite code: ${created.invite_code}`
        : 'Organization created.',
    );
    setSelectedOrganizationId(created?.organization_id ?? '');
    setOrganizationRefreshKey((value) => value + 1);
    setOrganizationBusy(false);
  };

  const rotateInviteCode = async () => {
    if (!supabase || !selectedOrganizationId) {
      return;
    }

    setOrganizationBusy(true);
    setOrganizationsError(null);
    setOrganizationNotice(null);

    const { data, error } = await supabase.rpc('admin_rotate_organization_invite_code', {
      target_organization_id: selectedOrganizationId,
    });

    if (error) {
      setOrganizationsError(error.message);
      setOrganizationBusy(false);
      return;
    }

    const rotated = ((data ?? []) as Array<{ invite_code: string }>)[0];
    setOrganizationNotice(
      rotated?.invite_code ? `New invite code: ${rotated.invite_code}` : 'Invite code rotated.',
    );
    setOrganizationRefreshKey((value) => value + 1);
    setOrganizationBusy(false);
  };

  const handleSignOut = async () => {
    if (!supabase) {
      return;
    }

    await supabase.auth.signOut();
    setRole(null);
    setProfileName('');
    setOrganizations([]);
    setSelectedOrganizationId('');
    setOrganizationUsers([]);
    setOrganizationOverview(null);
    setOrganizationTopSigns([]);
    setOrganizationDailyUsage([]);
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
      <main className="auth-page">
        <header className="auth-topbar">
          <PortalBrand />
          <button
            aria-label="Toggle dark mode"
            className="theme-toggle"
            onClick={toggleTheme}
            type="button"
          >
            <Icon name={theme === 'light' ? 'dark_mode' : 'light_mode'} />
          </button>
        </header>

        <section className="auth-stage">
          <div className="auth-orb auth-orb-primary" />
          <div className="auth-orb auth-orb-secondary" />

          <article className="auth-card auth-card-wide">
            <p className="eyebrow">Configuration required</p>
            <h2 className="page-title">The admin portal cannot start without Supabase keys.</h2>
            <p className="support-copy">
              Copy <code>.env.example</code> to <code>.env</code> and set the project URL and
              publishable key before loading the portal.
            </p>
            <p className="inline-feedback inline-feedback-error">{supabaseConfigError}</p>
          </article>
        </section>
      </main>
    );
  }

  if (!session) {
    return (
      <main className="auth-page">
        <header className="auth-topbar">
          <PortalBrand />
          <button
            aria-label="Toggle dark mode"
            className="theme-toggle"
            onClick={toggleTheme}
            type="button"
          >
            <Icon name={theme === 'light' ? 'dark_mode' : 'light_mode'} />
          </button>
        </header>

        <section className="auth-stage">
          <div className="auth-orb auth-orb-primary" />
          <div className="auth-orb auth-orb-secondary" />

          <article className="auth-card auth-card-wide">
            <p className="eyebrow">Empathetic Editorial Dashboard</p>
            <h2 className="page-title">Admin Access</h2>
            <p className="support-copy">
              Sign in to review SignSpeak complaints, inspect sign and dictionary context, and
              maintain resolution quality across the moderation queue.
            </p>

            <div className="auth-points">
              <div className="auth-point">
                <strong>Complaint workspace</strong>
                <span>Reporter context, prediction context, and resolution notes in one place.</span>
              </div>
              <div className="auth-point">
                <strong>Role-gated access</strong>
                <span>Only users marked as admin in Supabase can enter the portal.</span>
              </div>
            </div>
          </article>

          <article className="auth-card">
            <p className="eyebrow">Secure sign in</p>
            <h3 className="card-title">Use your Supabase admin account</h3>

            <form className="auth-form" onSubmit={handleLogin}>
              <label>
                Email
                <div className="field-shell">
                  <Icon name="alternate_email" />
                  <input
                    autoComplete="email"
                    onChange={(event) => setEmail(event.target.value)}
                    placeholder="admin@signspeak.pk"
                    type="email"
                    value={email}
                  />
                </div>
              </label>

              <label>
                <span className="field-label-row">
                  Password
                  <button
                    className="text-link"
                    disabled={authBusy}
                    onClick={handleResetPassword}
                    type="button"
                  >
                    Forgot password?
                  </button>
                </span>
                <div className="field-shell">
                  <Icon name="lock" />
                  <input
                    autoComplete="current-password"
                    onChange={(event) => setPassword(event.target.value)}
                    placeholder="Your password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                  />
                  <button
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    className="icon-button ghost-button"
                    onClick={() => setShowPassword((current) => !current)}
                    type="button"
                  >
                    <Icon name={showPassword ? 'visibility_off' : 'visibility'} />
                  </button>
                </div>
              </label>

              <button className="primary-button" disabled={authBusy} type="submit">
                <span>{authBusy ? 'Signing in...' : 'Sign in'}</span>
                <Icon name="login" />
              </button>
            </form>

            {authError ? <p className="inline-feedback inline-feedback-error">{authError}</p> : null}
            {authNotice ? (
              <p className="inline-feedback inline-feedback-success">{authNotice}</p>
            ) : null}

            <p className="support-copy support-copy-small">
              Access is granted only after <code>public.profiles.role = 'admin'</code>.
            </p>
          </article>
        </section>
      </main>
    );
  }

  if (roleLoading) {
    return (
      <main className="state-page">
        <header className="restricted-nav">
          <PortalBrand />
          <button
            aria-label="Toggle dark mode"
            className="theme-toggle"
            onClick={toggleTheme}
            type="button"
          >
            <Icon name={theme === 'light' ? 'dark_mode' : 'light_mode'} />
          </button>
        </header>

        <section className="state-card">
          <div className="state-icon">
            <Icon name="hourglass_top" />
          </div>
          <p className="eyebrow">Checking access</p>
          <h2 className="page-title">Loading your admin profile.</h2>
          <p className="support-copy">SignSpeak is validating your role and workspace access.</p>
        </section>
      </main>
    );
  }

  return (
    <div className="portal-shell">
      <aside className="portal-sidebar">
        <div className="sidebar-section">
          <PortalBrand />
        </div>

        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <button
              key={item.label}
              className={`nav-item ${item.active ? 'nav-item-active' : ''}`}
              type="button"
            >
              <Icon name={item.icon} />
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-summary panel">
          <p className="sidebar-summary-title">Weekly resolution</p>
          <div className="progress-track">
            <div
              className="progress-value"
              style={{
                width: `${complaints.length ? Math.round((complaintMetrics.resolved / complaints.length) * 100) : 0}%`,
              }}
            />
          </div>
          <p className="sidebar-summary-copy">
            {complaints.length
              ? `${Math.round((complaintMetrics.resolved / complaints.length) * 100)}% of current results are resolved`
              : 'No complaints loaded yet'}
          </p>
        </div>

        <div className="sidebar-footer">
          <button className="nav-item" type="button">
            <Icon name="help" />
            <span>Support</span>
          </button>
          <button className="nav-item" type="button">
            <Icon name="settings" />
            <span>Settings</span>
          </button>
        </div>
      </aside>

      <div className="portal-main">
        <header className="portal-topbar">
          <div>
            <p className="eyebrow">Complaint detail workspace</p>
            <h2 className="page-title page-title-small">SignSpeak moderation queue</h2>
          </div>

          <div className="topbar-tools">
            <form className="topbar-search" onSubmit={handleSearchSubmit}>
              <Icon name="search" />
              <input
                onChange={(event) => setSearchDraft(event.target.value)}
                placeholder="Search by word, note, name, or email"
                type="search"
                value={searchDraft}
              />
            </form>

            <button
              aria-label="Toggle dark mode"
              className="theme-toggle"
              onClick={toggleTheme}
              type="button"
            >
              <Icon name={theme === 'light' ? 'dark_mode' : 'light_mode'} />
            </button>

            <button aria-label="Notifications" className="icon-button" type="button">
              <Icon name="notifications" />
              {pendingCount ? <span className="notification-dot" /> : null}
            </button>

            <div className="profile-chip">
              <div className="profile-avatar">
                {(profileName || session.user.email || 'S').charAt(0).toUpperCase()}
              </div>
              <div className="profile-copy">
                <strong>{profileName || session.user.email}</strong>
                <span>{session.user.email}</span>
              </div>
            </div>

            <button className="secondary-button" onClick={handleSignOut} type="button">
              Sign out
            </button>
          </div>
        </header>

        <main className="portal-content">
          <section className="organization-panel panel">
            <div className="organization-header">
              <div>
                <p className="eyebrow">Institute workspace</p>
                <h3>
                  {selectedOrganization
                    ? selectedOrganization.name
                    : 'Create or select an institute'}
                </h3>
                <p>
                  Institute admins can create an organization, share the invite code, and review
                  organization-scoped users and analytics.
                </p>
              </div>

              <div className="organization-actions">
                {organizations.length ? (
                  <label>
                    Active institute
                    <div className="select-shell">
                      <select
                        onChange={(event) => {
                          setSelectedOrganizationId(event.target.value);
                          setRefreshKey((value) => value + 1);
                        }}
                        value={selectedOrganizationId}
                      >
                        {organizations.map((organization) => (
                          <option key={organization.id} value={organization.id}>
                            {organization.name}
                          </option>
                        ))}
                      </select>
                      <Icon name="expand_more" />
                    </div>
                  </label>
                ) : null}

                {selectedOrganization ? (
                  <div className="invite-card">
                    <span>Invite code</span>
                    <strong>{selectedOrganization.invite_code || 'Not generated'}</strong>
                    <button
                      className="secondary-button"
                      disabled={organizationBusy}
                      onClick={rotateInviteCode}
                      type="button"
                    >
                      Rotate code
                    </button>
                  </div>
                ) : null}
              </div>
            </div>

            <form className="organization-form" onSubmit={handleCreateOrganization}>
              <label>
                Institute name
                <div className="field-shell">
                  <Icon name="domain" />
                  <input
                    onChange={(event) => setOrganizationName(event.target.value)}
                    placeholder="ABC Deaf School"
                    value={organizationName}
                  />
                </div>
              </label>

              <label>
                City
                <div className="field-shell">
                  <Icon name="location_city" />
                  <input
                    onChange={(event) => setOrganizationCity(event.target.value)}
                    placeholder="Lahore"
                    value={organizationCity}
                  />
                </div>
              </label>

              <label>
                Contact email
                <div className="field-shell">
                  <Icon name="mail" />
                  <input
                    onChange={(event) => setOrganizationEmail(event.target.value)}
                    placeholder="admin@school.edu.pk"
                    type="email"
                    value={organizationEmail}
                  />
                </div>
              </label>

              <label>
                Website
                <div className="field-shell">
                  <Icon name="language" />
                  <input
                    onChange={(event) => setOrganizationWebsite(event.target.value)}
                    placeholder="https://school.edu.pk"
                    value={organizationWebsite}
                  />
                </div>
              </label>

              <button className="primary-button" disabled={organizationBusy} type="submit">
                <span>{organizationBusy ? 'Creating...' : 'Create institute'}</span>
                <Icon name="add_business" />
              </button>
            </form>

            {organizationsLoading ? (
              <p className="inline-feedback inline-feedback-success">Loading organizations...</p>
            ) : null}
            {organizationsError ? (
              <p className="inline-feedback inline-feedback-error">{organizationsError}</p>
            ) : null}
            {organizationNotice ? (
              <p className="inline-feedback inline-feedback-success">{organizationNotice}</p>
            ) : null}
          </section>

          {selectedOrganization ? (
            <section className="organization-dashboard">
              <div className="organization-metrics">
                <article className="metric-card">
                  <div className="metric-card-header">
                    <div className="metric-icon metric-icon-reviewing">
                      <Icon name="group" />
                    </div>
                    <span className="metric-chip">{selectedOrganization.status}</span>
                  </div>
                  <strong>{organizationOverview?.registered_users ?? 0}</strong>
                  <span>Registered users</span>
                  <small>{selectedOrganization.city || 'No city provided'}</small>
                </article>

                <article className="metric-card">
                  <div className="metric-card-header">
                    <div className="metric-icon metric-icon-resolved">
                      <Icon name="verified_user" />
                    </div>
                    <span className="metric-chip">30 days</span>
                  </div>
                  <strong>{organizationOverview?.active_users_30d ?? 0}</strong>
                  <span>Active users</span>
                  <small>Students or staff with recent translations</small>
                </article>

                <article className="metric-card">
                  <div className="metric-card-header">
                    <div className="metric-icon metric-icon-pending">
                      <Icon name="sign_language" />
                    </div>
                    <span className="metric-chip">30 days</span>
                  </div>
                  <strong>{organizationOverview?.translations_30d ?? 0}</strong>
                  <span>Translations</span>
                  <small>Synced institute usage</small>
                </article>

                <article className="metric-card">
                  <div className="metric-card-header">
                    <div className="metric-icon metric-icon-open">
                      <Icon name="report" />
                    </div>
                    <span className="metric-chip">30 days</span>
                  </div>
                  <strong>{organizationOverview?.complaints_30d ?? 0}</strong>
                  <span>Reports</span>
                  <small>Dictionary and prediction complaints</small>
                </article>
              </div>

              {organizationDataError ? (
                <p className="inline-feedback inline-feedback-error">{organizationDataError}</p>
              ) : null}

              <div className="organization-insights">
                <article className="panel organization-list-card">
                  <div className="panel-heading">
                    <div>
                      <p className="eyebrow">Roster</p>
                      <h3>
                        {organizationUsers.length === 1
                          ? '1 registered user'
                          : `${organizationUsers.length} registered users`}
                      </h3>
                    </div>
                    {organizationDataLoading ? <span className="loading-chip">Loading...</span> : null}
                  </div>
                  <div className="roster-table">
                    {organizationUsers.length ? (
                      organizationUsers.map((user) => (
                        <div key={user.user_id} className="roster-row">
                          <div>
                            <strong>{user.full_name || user.email || 'Unnamed user'}</strong>
                            <span>{user.email || user.user_id}</span>
                          </div>
                          <span>{user.role === 'org_admin' ? 'Admin' : 'Member'}</span>
                          <span>{Number(user.translation_count)} translations</span>
                          <span>{formatDate(user.last_active_at)}</span>
                        </div>
                      ))
                    ) : (
                      <p className="support-copy">No institute users yet.</p>
                    )}
                  </div>
                </article>

                <article className="panel organization-list-card">
                  <div className="panel-heading">
                    <div>
                      <p className="eyebrow">Top signs</p>
                      <h3>Most translated</h3>
                    </div>
                  </div>
                  <div className="rank-list">
                    {organizationTopSigns.length ? (
                      organizationTopSigns.map((item, index) => (
                        <div key={item.predicted_word_slug} className="rank-row">
                          <span>#{index + 1}</span>
                          <strong>{item.predicted_word_slug}</strong>
                          <em>{Number(item.usage_count)}</em>
                        </div>
                      ))
                    ) : (
                      <p className="support-copy">No synced translations in the last 30 days.</p>
                    )}
                  </div>
                </article>

                <article className="panel organization-list-card">
                  <div className="panel-heading">
                    <div>
                      <p className="eyebrow">Usage trend</p>
                      <h3>Daily translations</h3>
                    </div>
                  </div>
                  <div className="usage-bars">
                    {organizationDailyUsage.length ? (
                      organizationDailyUsage.map((item) => (
                        <div key={item.usage_date} className="usage-row">
                          <span>{formatDate(item.usage_date).split(',')[0]}</span>
                          <div className="progress-track">
                            <div
                              className="progress-value"
                              style={{
                                width: `${
                                  maxDailyTranslations
                                    ? Math.max(
                                        8,
                                        Math.round(
                                          (Number(item.translation_count) / maxDailyTranslations) *
                                            100,
                                        ),
                                      )
                                    : 0
                                }%`,
                              }}
                            />
                          </div>
                          <strong>{Number(item.translation_count)}</strong>
                        </div>
                      ))
                    ) : (
                      <p className="support-copy">No usage trend is available yet.</p>
                    )}
                  </div>
                </article>
              </div>
            </section>
          ) : null}

          <section className="kpi-grid">
            <article className="metric-card metric-card-pending">
              <div className="metric-card-header">
                <div className="metric-icon metric-icon-pending">
                  <Icon name="pending" />
                </div>
                <span className="metric-chip">Active</span>
              </div>
              <strong>{pendingCount}</strong>
              <span>Pending</span>
              <small>Open and reviewing complaints</small>
            </article>

            {(Object.keys(statusMeta) as ComplaintStatus[]).map((status) => {
              const meta = statusMeta[status];

              return (
                <article key={status} className="metric-card">
                  <div className="metric-card-header">
                    <div className={`metric-icon metric-icon-${status}`}>
                      <Icon name={meta.icon} />
                    </div>
                    <span className="metric-chip">{metricDelta(status)}</span>
                  </div>
                  <strong>{complaintMetrics[status]}</strong>
                  <span>{meta.label}</span>
                  <small>{meta.metricHint}</small>
                </article>
              );
            })}
          </section>

          <section className="filter-bar panel">
            <div className="filter-group">
              <label>
                Status
                <div className="select-shell">
                  <select
                    onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
                    value={statusFilter}
                  >
                    {statusOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  <Icon name="expand_more" />
                </div>
              </label>

              <label>
                Source
                <div className="select-shell">
                  <select
                    onChange={(event) => setSourceFilter(event.target.value as SourceFilter)}
                    value={sourceFilter}
                  >
                    {sourceOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  <Icon name="expand_more" />
                </div>
              </label>
            </div>

            <div className="filter-actions">
              <p>{listLoading ? 'Loading complaints...' : filteredResultLabel}</p>
              <button className="secondary-button" onClick={clearFilters} type="button">
                Clear filters
              </button>
              <button
                className="secondary-button"
                onClick={() => setRefreshKey((value) => value + 1)}
                type="button"
              >
                Refresh
              </button>
            </div>
          </section>

          {listError ? <p className="inline-feedback inline-feedback-error">{listError}</p> : null}

          {!listLoading && complaints.length === 0 ? (
            <section className="empty-stage">
              <div className="empty-visual">
                <div className="empty-visual-core">
                  <Icon name="search_off" />
                </div>
                <div className="empty-visual-tag">
                  <Icon name="filter_alt_off" />
                </div>
              </div>
              <h3>No complaints found matching your filters.</h3>
              <p>
                Try adjusting your status or source filters. The current query did not return any
                SignSpeak complaint records.
              </p>
              <div className="empty-actions">
                <button className="primary-button" onClick={clearFilters} type="button">
                  <span>Reset all filters</span>
                  <Icon name="restart_alt" />
                </button>
                <button
                  className="secondary-button"
                  onClick={() => setRefreshKey((value) => value + 1)}
                  type="button"
                >
                  Refresh
                </button>
              </div>
            </section>
          ) : (
            <section className="workspace-grid">
              <aside className="queue-panel">
                <div className="panel-heading">
                  <div>
                    <p className="eyebrow">Complaint queue</p>
                    <h3>{filteredResultLabel}</h3>
                  </div>
                  {listLoading ? <span className="loading-chip">Loading...</span> : null}
                </div>

                <div className="queue-list">
                  {complaints.map((complaint) => (
                    <button
                      key={complaint.id}
                      className={`queue-item ${complaint.id === selectedComplaintId ? 'queue-item-active' : ''}`}
                      onClick={() => setSelectedComplaintId(complaint.id)}
                      type="button"
                    >
                      <div className="queue-item-meta">
                        <span className="queue-item-id">#{complaint.id.slice(0, 8)}</span>
                        <span>{formatRelativeDate(complaint.created_at)}</span>
                      </div>
                      <h4>{complaintHeadline(complaint)}</h4>
                      <p>{complaintSnippet(complaint)}</p>
                      <div className="queue-item-footer">
                        <span>{reporterLabel(complaint)}</span>
                        <StatusBadge status={complaint.status} />
                      </div>
                    </button>
                  ))}
                </div>
              </aside>
              <section className="detail-panel">
                {!selectedComplaint ? (
                  <article className="detail-empty panel">
                    <h3>Select a complaint to inspect it.</h3>
                    <p>The detail panel will show reporter context, word context, and admin actions.</p>
                  </article>
                ) : (
                  <>
                    <div className="detail-header">
                      <div>
                        <div className="detail-header-row">
                          <h3>Complaint ID: #{selectedComplaint.id.slice(0, 8)}</h3>
                          <StatusBadge status={selectedComplaint.status} />
                        </div>
                        <p>
                          Initiated on {formatDate(selectedComplaint.created_at)} • Priority{' '}
                          {complaintPriority(selectedComplaint.status)}
                        </p>
                      </div>
                      <div className="detail-header-actions">
                        <button
                          className="secondary-button"
                          onClick={() => setRefreshKey((value) => value + 1)}
                          type="button"
                        >
                          Refresh record
                        </button>
                      </div>
                    </div>

                    <div className="detail-layout">
                      <div className="detail-main">
                        <article className="visual-card panel">
                          <div className="visual-stage">
                            <div className="visual-gradient" />
                            <div className="visual-badge">Reported sign</div>
                            <div className="visual-chip-row">
                              <span className="visual-chip">{complaintHeadline(selectedComplaint)}</span>
                              {selectedComplaint.expected_word ? (
                                <span className="visual-chip visual-chip-secondary">
                                  Expected: {selectedComplaint.expected_word}
                                </span>
                              ) : null}
                            </div>
                            <div className="visual-icon-wrap">
                              <Icon name={sourceMeta[selectedComplaint.source_type].icon} />
                            </div>
                          </div>

                          <div className="visual-copy">
                            <p className="eyebrow">User note</p>
                            <blockquote>
                              {selectedComplaint.note ||
                                'No user note was submitted with this complaint.'}
                            </blockquote>
                          </div>
                        </article>

                        <div className="insight-grid">
                          <article className="insight-card">
                            <div className="insight-heading">
                              <div className="metric-icon metric-icon-reviewing">
                                <Icon name="analytics" />
                              </div>
                              <span>Confidence score</span>
                            </div>
                            <strong>{confidenceLabel(selectedComplaint.history_confidence)}</strong>
                            <div className="progress-track">
                              <div
                                className="progress-value"
                                style={{
                                  width: `${selectedComplaint.history_confidence ? Math.round(selectedComplaint.history_confidence * 100) : 0}%`,
                                }}
                              />
                            </div>
                          </article>

                          <article className="insight-card">
                            <div className="insight-heading">
                              <div className="metric-icon metric-icon-pending">
                                <Icon name="label" />
                              </div>
                              <span>Predicted label</span>
                            </div>
                            <strong>{selectedComplaint.history_predicted_word_slug || 'Unavailable'}</strong>
                            <small>Model version: {modelVersionLabel(selectedComplaint.history_model_version)}</small>
                          </article>
                        </div>

                        <div className="context-grid">
                          <article className="context-card panel">
                            <p className="eyebrow">Word context</p>
                            <dl>
                              <div>
                                <dt>Source</dt>
                                <dd>{sourceMeta[selectedComplaint.source_type].label}</dd>
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

                          <article className="context-card panel">
                            <p className="eyebrow">Prediction context</p>
                            <dl>
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
                                <dd>{confidenceLabel(selectedComplaint.history_confidence)}</dd>
                              </div>
                              <div>
                                <dt>Model version</dt>
                                <dd>{modelVersionLabel(selectedComplaint.history_model_version)}</dd>
                              </div>
                              <div>
                                <dt>Prediction time</dt>
                                <dd>{formatDate(selectedComplaint.history_created_at)}</dd>
                              </div>
                              <div>
                                <dt>Resolved</dt>
                                <dd>{formatDate(selectedComplaint.resolved_at)}</dd>
                              </div>
                            </dl>
                          </article>
                        </div>
                      </div>

                      <aside className="detail-side">
                        <article className="reporter-card panel">
                          <p className="eyebrow">Reporter information</p>
                          <div className="reporter-identity">
                            <div className="reporter-avatar">{initialsFor(selectedComplaint)}</div>
                            <div>
                              <strong>{reporterLabel(selectedComplaint)}</strong>
                              <span>{selectedComplaint.reporter_id}</span>
                            </div>
                          </div>
                          <dl>
                            <div>
                              <dt>Email</dt>
                              <dd>{selectedComplaint.reporter_email || 'Not available'}</dd>
                            </div>
                            <div>
                              <dt>Status</dt>
                              <dd>{role === 'admin' ? 'Verified admin review' : 'Unknown'}</dd>
                            </div>
                            <div>
                              <dt>Complaint source</dt>
                              <dd>{sourceMeta[selectedComplaint.source_type].label}</dd>
                            </div>
                            <div>
                              <dt>Last updated</dt>
                              <dd>{formatDate(selectedComplaint.updated_at)}</dd>
                            </div>
                          </dl>
                        </article>

                        <article className="resolution-card panel">
                          <p className="eyebrow">Admin resolution</p>

                          <label>
                            Update status
                            <div className="select-shell">
                              <select
                                onChange={(event) =>
                                  setStatusDraft(event.target.value as ComplaintStatus)
                                }
                                value={statusDraft}
                              >
                                {statusOptions
                                  .filter((option) => option.value !== 'all')
                                  .map((option) => (
                                    <option key={option.value} value={option.value}>
                                      {option.label}
                                    </option>
                                  ))}
                              </select>
                              <Icon name="expand_more" />
                            </div>
                          </label>

                          <label>
                            Internal admin notes
                            <textarea
                              onChange={(event) => setAdminNoteDraft(event.target.value)}
                              placeholder="Add notes about the resolution, investigation, or follow-up."
                              rows={6}
                              value={adminNoteDraft}
                            />
                          </label>

                          <div className="resolution-actions">
                            <button
                              className="primary-button"
                              disabled={saveBusy}
                              onClick={saveComplaint}
                              type="button"
                            >
                              <span>{saveBusy ? 'Saving...' : 'Save changes'}</span>
                              <Icon name="save" />
                            </button>
                            <button className="secondary-button" onClick={clearFilters} type="button">
                              Reset view
                            </button>
                          </div>

                          {saveError ? (
                            <p className="inline-feedback inline-feedback-error">{saveError}</p>
                          ) : null}
                          {saveNotice ? (
                            <p className="inline-feedback inline-feedback-success">{saveNotice}</p>
                          ) : null}
                        </article>
                      </aside>
                    </div>
                  </>
                )}
              </section>
            </section>
          )}
        </main>
      </div>
    </div>
  );
}
