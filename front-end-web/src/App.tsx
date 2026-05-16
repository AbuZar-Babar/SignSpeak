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
type PortalRoute = 'org-login' | 'request-account' | 'super-admin' | 'setup-password';
type LoginMode = 'organization' | 'super';

type CreateOrgAdminResult = {
  organizationId: string;
  organizationName: string;
  orgAdminEmail: string;
  setupLink: string;
  inviteCode: string;
  linkedExistingUser: boolean;
};

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

const themeStorageKey = 'signspeak-admin-theme';

const statusMeta: Record<ComplaintStatus, { label: string; icon: string; metricHint: string }> = {
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

const contactConfig = {
  email: import.meta.env.VITE_ORG_CONTACT_EMAIL?.trim() || null,
  phone: import.meta.env.VITE_ORG_CONTACT_PHONE?.trim() || null,
  whatsappUrl: import.meta.env.VITE_ORG_CONTACT_WHATSAPP_URL?.trim() || null,
};

function isCreateOrgAdminResult(value: unknown): value is CreateOrgAdminResult {
  return (
    typeof value === 'object' &&
    value !== null &&
    'organizationId' in value &&
    'setupLink' in value &&
    'inviteCode' in value
  );
}

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

  return `${Math.round(deltaHours / 24)}d ago`;
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

function routeFromHash(): PortalRoute {
  if (typeof window === 'undefined') {
    return 'org-login';
  }

  const hash = window.location.hash || '';
  if (hash.includes('type=invite') || hash.includes('type=recovery')) {
    return 'setup-password';
  }

  const route = hash.replace(/^#\/?/, '').split(/[?&]/)[0];
  switch (route) {
    case 'request-account':
    case 'super-admin':
    case 'setup-password':
    case 'org-login':
      return route;
    default:
      return 'org-login';
  }
}

function hashForRoute(route: PortalRoute): string {
  return `#/${route}`;
}

function passwordSetupRedirect(): string {
  if (typeof window === 'undefined') {
    return '';
  }

  return `${window.location.origin}${window.location.pathname}${hashForRoute('setup-password')}`;
}

function Icon({ name, className }: { name: string; className?: string }) {
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
  const [portalRoute, setPortalRoute] = useState<PortalRoute>(routeFromHash);
  const [passwordSetupRequired, setPasswordSetupRequired] = useState(
    routeFromHash() === 'setup-password',
  );
  const [session, setSession] = useState<Session | null>(null);
  const [role, setRole] = useState<UserRole | null>(null);
  const [profileName, setProfileName] = useState('');
  const [roleLoading, setRoleLoading] = useState(true);

  const [organizations, setOrganizations] = useState<OrganizationRow[]>([]);
  const [selectedOrganizationId, setSelectedOrganizationId] = useState('');
  const [organizationsLoading, setOrganizationsLoading] = useState(false);
  const [organizationsError, setOrganizationsError] = useState<string | null>(null);
  const [organizationRefreshKey, setOrganizationRefreshKey] = useState(0);
  const [organizationBusy, setOrganizationBusy] = useState(false);
  const [organizationNotice, setOrganizationNotice] = useState<string | null>(null);
  const [createdOrgAdmin, setCreatedOrgAdmin] = useState<CreateOrgAdminResult | null>(null);

  const [organizationName, setOrganizationName] = useState('');
  const [organizationCity, setOrganizationCity] = useState('');
  const [organizationEmail, setOrganizationEmail] = useState('');
  const [organizationWebsite, setOrganizationWebsite] = useState('');
  const [orgAdminName, setOrgAdminName] = useState('');
  const [orgAdminEmail, setOrgAdminEmail] = useState('');

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

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [setupBusy, setSetupBusy] = useState(false);
  const [setupError, setSetupError] = useState<string | null>(null);
  const [setupNotice, setSetupNotice] = useState<string | null>(null);

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

  const isSuperAdmin = role === 'admin';
  const hasOrgAdminAccess = organizations.some((organization) => organization.user_role === 'org_admin');
  const hasPortalAccess = isSuperAdmin || hasOrgAdminAccess;

  const selectedComplaint = useMemo(
    () => complaints.find((complaint) => complaint.id === selectedComplaintId) ?? null,
    [complaints, selectedComplaintId],
  );

  const selectedOrganization = useMemo(
    () => organizations.find((organization) => organization.id === selectedOrganizationId) ?? null,
    [organizations, selectedOrganizationId],
  );

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

  useEffect(() => {
    document.documentElement.classList.toggle('theme-dark', theme === 'dark');
    document.documentElement.style.colorScheme = theme;
    window.localStorage.setItem(themeStorageKey, theme);
  }, [theme]);

  useEffect(() => {
    const syncRoute = () => {
      const nextRoute = routeFromHash();
      setPortalRoute(nextRoute);
      if (nextRoute === 'setup-password') {
        setPasswordSetupRequired(true);
      }
    };

    window.addEventListener('hashchange', syncRoute);
    return () => window.removeEventListener('hashchange', syncRoute);
  }, []);

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

    const { data: authListener } = client.auth.onAuthStateChange((event, nextSession) => {
      if (!mounted) {
        return;
      }

      if (event === 'PASSWORD_RECOVERY' || routeFromHash() === 'setup-password') {
        setPasswordSetupRequired(true);
      }

      setSession(nextSession);
      if (!nextSession) {
        clearAuthenticatedState();
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
    if (!client || !session?.user.id || roleLoading || organizationsLoading) {
      return;
    }

    if (!isSuperAdmin && !selectedOrganizationId) {
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
    isSuperAdmin,
    roleLoading,
    organizationsLoading,
    selectedOrganizationId,
    session?.user.id,
    statusFilter,
    sourceFilter,
    searchTerm,
    refreshKey,
  ]);

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

      setOrganizationOverview(((overviewResult.data ?? []) as OrganizationOverviewRow[])[0] ?? null);
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

  function navigate(route: PortalRoute) {
    window.location.hash = hashForRoute(route);
    setPortalRoute(route);
  }

  function clearAuthenticatedState() {
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
    setPasswordSetupRequired(false);
  }

  const toggleTheme = () => {
    setTheme((current) => (current === 'light' ? 'dark' : 'light'));
  };

  const handleLogin = async (event: FormEvent<HTMLFormElement>, loginMode: LoginMode) => {
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
    setAuthNotice(
      loginMode === 'super'
        ? 'Signed in. Loading super admin access.'
        : 'Signed in. Loading institute workspace.',
    );
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

    const { error } = await supabase.auth.resetPasswordForEmail(email.trim(), {
      redirectTo: passwordSetupRedirect(),
    });

    if (error) {
      setAuthError(error.message);
      setAuthBusy(false);
      return;
    }

    setAuthNotice('Password setup email sent.');
    setAuthBusy(false);
  };

  const handleSetPassword = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!supabase) {
      return;
    }

    if (newPassword.length < 8) {
      setSetupError('Use at least 8 characters for the password.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setSetupError('Passwords do not match.');
      return;
    }

    setSetupBusy(true);
    setSetupError(null);
    setSetupNotice(null);

    const { error } = await supabase.auth.updateUser({
      password: newPassword,
    });

    if (error) {
      setSetupError(error.message);
      setSetupBusy(false);
      return;
    }

    setNewPassword('');
    setConfirmPassword('');
    setPasswordSetupRequired(false);
    setSetupNotice('Password updated. Loading your workspace.');
    navigate('org-login');
    setSetupBusy(false);
  };

  const handleCreateOrgAdmin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!supabase) {
      return;
    }

    if (!organizationName.trim()) {
      setOrganizationsError('Enter an institute name.');
      return;
    }

    if (!orgAdminEmail.trim()) {
      setOrganizationsError('Enter the organization admin email.');
      return;
    }

    setOrganizationBusy(true);
    setOrganizationsError(null);
    setOrganizationNotice(null);
    setCreatedOrgAdmin(null);

    const { data, error } = await supabase.functions.invoke('admin-create-org-admin', {
      body: {
        organizationName: organizationName.trim(),
        city: organizationCity.trim() || null,
        contactEmail: organizationEmail.trim() || null,
        websiteUrl: organizationWebsite.trim() || null,
        adminFullName: orgAdminName.trim() || null,
        adminEmail: orgAdminEmail.trim(),
        redirectTo: passwordSetupRedirect(),
      },
    });

    if (error) {
      setOrganizationsError(error.message);
      setOrganizationBusy(false);
      return;
    }

    if (!isCreateOrgAdminResult(data)) {
      const response = data as { error?: string } | null;
      setOrganizationsError(response?.error || 'Unable to create the organization admin account.');
      setOrganizationBusy(false);
      return;
    }

    setCreatedOrgAdmin(data);
    setOrganizationName('');
    setOrganizationCity('');
    setOrganizationEmail('');
    setOrganizationWebsite('');
    setOrgAdminName('');
    setOrgAdminEmail('');
    setOrganizationNotice(
      data.linkedExistingUser
        ? 'Existing user linked as organization admin. Send them the setup link below.'
        : 'Organization admin account created. Send the setup link below.',
    );
    setSelectedOrganizationId(data.organizationId);
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
      rotated?.invite_code ? `New student invite code: ${rotated.invite_code}` : 'Invite code rotated.',
    );
    setOrganizationRefreshKey((value) => value + 1);
    setOrganizationBusy(false);
  };

  const copyCreatedSetupLink = async () => {
    if (!createdOrgAdmin?.setupLink) {
      return;
    }

    await navigator.clipboard.writeText(createdOrgAdmin.setupLink);
    setOrganizationNotice('Setup link copied.');
  };

  const handleSignOut = async () => {
    if (!supabase) {
      return;
    }

    await supabase.auth.signOut();
    clearAuthenticatedState();
    setEmail('');
    setPassword('');
    setAuthNotice(null);
    navigate('org-login');
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

  const renderTopbar = () => (
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
  );

  if (supabaseConfigError) {
    return (
      <main className="auth-page">
        {renderTopbar()}
        <section className="auth-stage auth-stage-single">
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
        {renderTopbar()}
        {portalRoute === 'request-account' ? (
          <RequestAccountPage navigate={navigate} />
        ) : portalRoute === 'setup-password' ? (
          <SetupLinkWaitingPage navigate={navigate} />
        ) : (
          <LoginPage
            authBusy={authBusy}
            authError={authError}
            authNotice={authNotice}
            email={email}
            handleLogin={handleLogin}
            handleResetPassword={handleResetPassword}
            loginMode={portalRoute === 'super-admin' ? 'super' : 'organization'}
            navigate={navigate}
            password={password}
            setEmail={setEmail}
            setPassword={setPassword}
            setShowPassword={setShowPassword}
            showPassword={showPassword}
          />
        )}
      </main>
    );
  }

  if (roleLoading || organizationsLoading) {
    return (
      <StatePage
        icon="hourglass_top"
        eyebrow="Checking access"
        title="Loading your admin profile."
        copy="SignSpeak is validating your account role and institute workspace."
        onToggleTheme={toggleTheme}
        theme={theme}
      />
    );
  }

  if (passwordSetupRequired || portalRoute === 'setup-password') {
    return (
      <PasswordSetupPage
        confirmPassword={confirmPassword}
        newPassword={newPassword}
        onSubmit={handleSetPassword}
        onToggleTheme={toggleTheme}
        setConfirmPassword={setConfirmPassword}
        setNewPassword={setNewPassword}
        setupBusy={setupBusy}
        setupError={setupError}
        setupNotice={setupNotice}
        theme={theme}
      />
    );
  }

  if (!hasPortalAccess) {
    return (
      <StatePage
        icon="lock"
        eyebrow="Access not assigned"
        title="This account is not linked to an institute admin workspace."
        copy="Use an organization admin account, or contact the SignSpeak owner to request access."
        onPrimaryAction={handleSignOut}
        onToggleTheme={toggleTheme}
        primaryActionLabel="Sign out"
        theme={theme}
        warning
      />
    );
  }

  return (
    <div className="portal-shell">
      <aside className="portal-sidebar">
        <div className="sidebar-section">
          <PortalBrand />
        </div>

        <nav className="sidebar-nav">
          <button className="nav-item nav-item-active" type="button">
            <Icon name="dashboard" />
            <span>{isSuperAdmin ? 'Platform' : 'Institute'}</span>
          </button>
          <button className="nav-item" type="button">
            <Icon name="forum" />
            <span>Complaints</span>
          </button>
          <button className="nav-item" type="button">
            <Icon name="leaderboard" />
            <span>Analytics</span>
          </button>
        </nav>

        <div className="sidebar-summary panel">
          <p className="sidebar-summary-title">
            {isSuperAdmin ? 'Selected institute' : 'Institute status'}
          </p>
          <div className="progress-track">
            <div
              className="progress-value"
              style={{
                width: `${complaints.length ? Math.round((complaintMetrics.resolved / complaints.length) * 100) : 0}%`,
              }}
            />
          </div>
          <p className="sidebar-summary-copy">
            {selectedOrganization
              ? `${selectedOrganization.name} has ${organizationUsers.length} registered users.`
              : 'No institute selected yet.'}
          </p>
        </div>

        <div className="sidebar-footer">
          <button className="nav-item" onClick={handleSignOut} type="button">
            <Icon name="logout" />
            <span>Sign out</span>
          </button>
        </div>
      </aside>

      <div className="portal-main">
        <header className="portal-topbar">
          <div>
            <p className="eyebrow">{isSuperAdmin ? 'Super admin workspace' : 'Institute workspace'}</p>
            <h2 className="page-title page-title-small">
              {isSuperAdmin ? 'Schools and organization admins' : selectedOrganization?.name || 'School dashboard'}
            </h2>
          </div>

          <div className="topbar-tools">
            <form className="topbar-search" onSubmit={handleSearchSubmit}>
              <Icon name="search" />
              <input
                onChange={(event) => setSearchDraft(event.target.value)}
                placeholder="Search complaints by word, note, name, or email"
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

            <div className="profile-chip">
              <div className="profile-avatar">
                {(profileName || session.user.email || 'S').charAt(0).toUpperCase()}
              </div>
              <div className="profile-copy">
                <strong>{profileName || session.user.email}</strong>
                <span>{isSuperAdmin ? 'Super admin' : session.user.email}</span>
              </div>
            </div>
          </div>
        </header>

        <main className="portal-content">
          {isSuperAdmin ? (
            <CreateOrgAdminPanel
              createdOrgAdmin={createdOrgAdmin}
              handleCreateOrgAdmin={handleCreateOrgAdmin}
              organizationBusy={organizationBusy}
              organizationCity={organizationCity}
              organizationEmail={organizationEmail}
              organizationName={organizationName}
              organizationNotice={organizationNotice}
              organizationWebsite={organizationWebsite}
              organizationsError={organizationsError}
              orgAdminEmail={orgAdminEmail}
              orgAdminName={orgAdminName}
              setOrganizationCity={setOrganizationCity}
              setOrganizationEmail={setOrganizationEmail}
              setOrganizationName={setOrganizationName}
              setOrganizationWebsite={setOrganizationWebsite}
              setOrgAdminEmail={setOrgAdminEmail}
              setOrgAdminName={setOrgAdminName}
              copyCreatedSetupLink={copyCreatedSetupLink}
            />
          ) : null}

          <OrganizationPanel
            isSuperAdmin={isSuperAdmin}
            organizationBusy={organizationBusy}
            organizationNotice={!isSuperAdmin ? organizationNotice : null}
            organizations={organizations}
            organizationsError={!isSuperAdmin ? organizationsError : null}
            organizationsLoading={organizationsLoading}
            rotateInviteCode={rotateInviteCode}
            selectedOrganization={selectedOrganization}
            selectedOrganizationId={selectedOrganizationId}
            setRefreshKey={setRefreshKey}
            setSelectedOrganizationId={setSelectedOrganizationId}
          />

          {selectedOrganization ? (
            <OrganizationDashboard
              isSuperAdmin={isSuperAdmin}
              maxDailyTranslations={maxDailyTranslations}
              organizationDailyUsage={organizationDailyUsage}
              organizationDataError={organizationDataError}
              organizationDataLoading={organizationDataLoading}
              organizationOverview={organizationOverview}
              organizations={organizations}
              organizationTopSigns={organizationTopSigns}
              organizationUsers={organizationUsers}
              selectedOrganization={selectedOrganization}
              setSelectedOrganizationId={setSelectedOrganizationId}
            />
          ) : (
            <section className="empty-stage">
              <div className="empty-visual">
                <div className="empty-visual-core">
                  <Icon name="domain_disabled" />
                </div>
              </div>
              <h3>No institute workspace yet.</h3>
              <p>
                {isSuperAdmin
                  ? 'Create the first school admin account to start onboarding institutes.'
                  : 'Ask the SignSpeak owner to link your account to an institute.'}
              </p>
            </section>
          )}

          <ComplaintWorkspace
            adminNoteDraft={adminNoteDraft}
            clearFilters={clearFilters}
            complaints={complaints}
            complaintMetrics={complaintMetrics}
            filteredResultLabel={filteredResultLabel}
            isSuperAdmin={isSuperAdmin}
            listError={listError}
            listLoading={listLoading}
            pendingCount={pendingCount}
            refreshList={() => setRefreshKey((value) => value + 1)}
            saveBusy={saveBusy}
            saveComplaint={saveComplaint}
            saveError={saveError}
            saveNotice={saveNotice}
            selectedComplaint={selectedComplaint}
            selectedComplaintId={selectedComplaintId}
            setAdminNoteDraft={setAdminNoteDraft}
            setSelectedComplaintId={setSelectedComplaintId}
            setSourceFilter={setSourceFilter}
            setStatusDraft={setStatusDraft}
            sourceFilter={sourceFilter}
            statusDraft={statusDraft}
            statusFilter={statusFilter}
            setStatusFilter={setStatusFilter}
          />
        </main>
      </div>
    </div>
  );
}

function LoginPage({
  authBusy,
  authError,
  authNotice,
  email,
  handleLogin,
  handleResetPassword,
  loginMode,
  navigate,
  password,
  setEmail,
  setPassword,
  setShowPassword,
  showPassword,
}: {
  authBusy: boolean;
  authError: string | null;
  authNotice: string | null;
  email: string;
  handleLogin: (event: FormEvent<HTMLFormElement>, loginMode: LoginMode) => void;
  handleResetPassword: () => void;
  loginMode: LoginMode;
  navigate: (route: PortalRoute) => void;
  password: string;
  setEmail: (value: string) => void;
  setPassword: (value: string) => void;
  setShowPassword: (updater: (current: boolean) => boolean) => void;
  showPassword: boolean;
}) {
  const isSuperLogin = loginMode === 'super';

  return (
    <section className="auth-stage">
      <article className="auth-card auth-card-wide">
        <p className="eyebrow">{isSuperLogin ? 'SignSpeak Platform' : 'SignSpeak Schools'}</p>
        <h2 className="page-title">
          {isSuperLogin ? 'Platform Console' : 'School Admin Console'}
        </h2>
        <p className="support-copy">
          {isSuperLogin
            ? 'Manage school onboarding, assign organization admins, and monitor institute-level activity.'
            : 'Track student usage, manage your institute invite code, and review school-specific reports.'}
        </p>

        <div className="auth-points">
          <div className="auth-point">
            <strong>{isSuperLogin ? 'School Onboarding' : 'Institute Scope'}</strong>
            <span>
              {isSuperLogin
                ? 'Create verified school workspaces and issue one-time setup links.'
                : 'You only see users, analytics, and reports linked to your own school.'}
            </span>
          </div>
          <div className="auth-point">
            <strong>{isSuperLogin ? 'Owner-Level Access' : 'Student Join Flow'}</strong>
            <span>
              {isSuperLogin
                ? 'Reserved for the product owner and trusted platform operators.'
                : 'Students join your institute from the app using your active invite code.'}
            </span>
          </div>
        </div>
      </article>

      <article className="auth-card">
        <p className="eyebrow">{isSuperLogin ? 'Platform Sign In' : 'School Sign In'}</p>
        <h3 className="card-title">
          {isSuperLogin ? 'Sign in as Super Admin' : 'Sign in as School Admin'}
        </h3>

        <form className="auth-form" onSubmit={(event) => handleLogin(event, loginMode)}>
          <label>
            Work Email
            <div className="field-shell">
              <Icon name="alternate_email" />
              <input
                autoComplete="email"
                onChange={(event) => setEmail(event.target.value)}
                placeholder={isSuperLogin ? 'owner@signspeak.pk' : 'teacher@school.edu.pk'}
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
                Reset password
              </button>
            </span>
            <div className="field-shell">
              <Icon name="lock" />
              <input
                autoComplete="current-password"
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Enter password"
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
            <span>{authBusy ? 'Signing in...' : isSuperLogin ? 'Open Platform Console' : 'Open School Console'}</span>
            <Icon name="login" />
          </button>
        </form>

        {authError ? <p className="inline-feedback inline-feedback-error">{authError}</p> : null}
        {authNotice ? <p className="inline-feedback inline-feedback-success">{authNotice}</p> : null}

        {isSuperLogin ? (
          <button className="text-link auth-footer-link" onClick={() => navigate('org-login')} type="button">
            Switch to school admin login
          </button>
        ) : (
          <>
            <button
              className="secondary-button auth-request-button"
              onClick={() => navigate('request-account')}
              type="button"
            >
              <span>Request School Admin Account</span>
              <Icon name="contact_support" />
            </button>
            <button
              className="text-link auth-footer-link"
              onClick={() => navigate('super-admin')}
              type="button"
            >
              Super Admin Login
            </button>
          </>
        )}
      </article>
    </section>
  );
}

function RequestAccountPage({ navigate }: { navigate: (route: PortalRoute) => void }) {
  return (
    <section className="auth-stage">
      <article className="auth-card auth-card-wide">
        <p className="eyebrow">School Onboarding</p>
        <h2 className="page-title">Request a School Admin Account</h2>
        <p className="support-copy">
          School accounts are created after manual verification. Share your school name, city,
          contact person, and preferred admin email to begin onboarding.
        </p>

        <div className="auth-points">
          <div className="auth-point">
            <strong>What You Receive</strong>
            <span>A school admin login, a password setup link, and a student invite code.</span>
          </div>
          <div className="auth-point">
            <strong>Student Access</strong>
            <span>Public app usage stays open; only school-tracked progress requires login.</span>
          </div>
        </div>
      </article>

      <article className="auth-card">
        <p className="eyebrow">Contact Channels</p>
        <h3 className="card-title">Contact SignSpeak Owner</h3>

        <div className="contact-list">
          <ContactLink icon="mail" href={contactConfig.email ? `mailto:${contactConfig.email}` : null} label="Email" value={contactConfig.email} />
          <ContactLink icon="call" href={contactConfig.phone ? `tel:${contactConfig.phone}` : null} label="Phone" value={contactConfig.phone} />
          <ContactLink icon="chat" href={contactConfig.whatsappUrl} label="WhatsApp" value={contactConfig.whatsappUrl ? 'Open WhatsApp' : null} />
        </div>

        {!contactConfig.email && !contactConfig.phone && !contactConfig.whatsappUrl ? (
          <p className="inline-feedback inline-feedback-error">
            Contact details are not configured yet. Set the Vite contact environment variables.
          </p>
        ) : null}

        <button className="secondary-button" onClick={() => navigate('org-login')} type="button">
          <Icon name="arrow_back" />
          <span>Back to School Login</span>
        </button>
      </article>
    </section>
  );
}

function ContactLink({
  href,
  icon,
  label,
  value,
}: {
  href: string | null;
  icon: string;
  label: string;
  value: string | null;
}) {
  const content = (
    <>
      <Icon name={icon} />
      <span>
        <strong>{label}</strong>
        <em>{value ?? 'Not configured'}</em>
      </span>
    </>
  );

  if (!href || !value) {
    return <div className="contact-link contact-link-disabled">{content}</div>;
  }

  return (
    <a className="contact-link" href={href} rel="noreferrer" target={href.startsWith('http') ? '_blank' : undefined}>
      {content}
    </a>
  );
}

function SetupLinkWaitingPage({ navigate }: { navigate: (route: PortalRoute) => void }) {
  return (
    <section className="auth-stage auth-stage-single">
      <article className="auth-card auth-card-wide">
        <p className="eyebrow">Password Setup</p>
        <h2 className="page-title">Open the Latest Setup Link from Email</h2>
        <p className="support-copy">
          If the link expired, request a new setup link from the SignSpeak owner or use password
          reset on the login form.
        </p>
        <button className="secondary-button" onClick={() => navigate('org-login')} type="button">
          Back to School Login
        </button>
      </article>
    </section>
  );
}

function PasswordSetupPage({
  confirmPassword,
  newPassword,
  onSubmit,
  onToggleTheme,
  setConfirmPassword,
  setNewPassword,
  setupBusy,
  setupError,
  setupNotice,
  theme,
}: {
  confirmPassword: string;
  newPassword: string;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onToggleTheme: () => void;
  setConfirmPassword: (value: string) => void;
  setNewPassword: (value: string) => void;
  setupBusy: boolean;
  setupError: string | null;
  setupNotice: string | null;
  theme: ThemeMode;
}) {
  return (
    <main className="auth-page">
      <header className="auth-topbar">
        <PortalBrand />
        <button
          aria-label="Toggle dark mode"
          className="theme-toggle"
          onClick={onToggleTheme}
          type="button"
        >
          <Icon name={theme === 'light' ? 'dark_mode' : 'light_mode'} />
        </button>
      </header>

      <section className="auth-stage auth-stage-single">
        <article className="auth-card">
          <p className="eyebrow">Account Setup</p>
          <h2 className="page-title page-title-small">Set Admin Password</h2>
          <p className="support-copy">
            This password will be used for future school admin sign-ins.
          </p>

          <form className="auth-form" onSubmit={onSubmit}>
            <label>
              New password
              <div className="field-shell">
                <Icon name="lock_reset" />
                <input
                  autoComplete="new-password"
                  onChange={(event) => setNewPassword(event.target.value)}
                  type="password"
                  value={newPassword}
                />
              </div>
            </label>

            <label>
              Confirm password
              <div className="field-shell">
                <Icon name="lock" />
                <input
                  autoComplete="new-password"
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  type="password"
                  value={confirmPassword}
                />
              </div>
            </label>

            <button className="primary-button" disabled={setupBusy} type="submit">
              <span>{setupBusy ? 'Saving...' : 'Save password'}</span>
              <Icon name="check" />
            </button>
          </form>

          {setupError ? <p className="inline-feedback inline-feedback-error">{setupError}</p> : null}
          {setupNotice ? <p className="inline-feedback inline-feedback-success">{setupNotice}</p> : null}
        </article>
      </section>
    </main>
  );
}

function StatePage({
  copy,
  eyebrow,
  icon,
  onPrimaryAction,
  onToggleTheme,
  primaryActionLabel,
  theme,
  title,
  warning,
}: {
  copy: string;
  eyebrow: string;
  icon: string;
  onPrimaryAction?: () => void;
  onToggleTheme: () => void;
  primaryActionLabel?: string;
  theme: ThemeMode;
  title: string;
  warning?: boolean;
}) {
  return (
    <main className="state-page">
      <header className="restricted-nav">
        <PortalBrand />
        <button
          aria-label="Toggle dark mode"
          className="theme-toggle"
          onClick={onToggleTheme}
          type="button"
        >
          <Icon name={theme === 'light' ? 'dark_mode' : 'light_mode'} />
        </button>
      </header>

      <section className="state-card">
        <div className={`state-icon ${warning ? 'state-icon-warning' : ''}`}>
          <Icon name={icon} />
        </div>
        <p className="eyebrow">{eyebrow}</p>
        <h2 className="page-title">{title}</h2>
        <p className="support-copy">{copy}</p>
        {onPrimaryAction && primaryActionLabel ? (
          <div className="state-actions">
            <button className="primary-button" onClick={onPrimaryAction} type="button">
              {primaryActionLabel}
            </button>
          </div>
        ) : null}
      </section>
    </main>
  );
}

function CreateOrgAdminPanel({
  copyCreatedSetupLink,
  createdOrgAdmin,
  handleCreateOrgAdmin,
  organizationBusy,
  organizationCity,
  organizationEmail,
  organizationName,
  organizationNotice,
  organizationWebsite,
  organizationsError,
  orgAdminEmail,
  orgAdminName,
  setOrganizationCity,
  setOrganizationEmail,
  setOrganizationName,
  setOrganizationWebsite,
  setOrgAdminEmail,
  setOrgAdminName,
}: {
  copyCreatedSetupLink: () => void;
  createdOrgAdmin: CreateOrgAdminResult | null;
  handleCreateOrgAdmin: (event: FormEvent<HTMLFormElement>) => void;
  organizationBusy: boolean;
  organizationCity: string;
  organizationEmail: string;
  organizationName: string;
  organizationNotice: string | null;
  organizationWebsite: string;
  organizationsError: string | null;
  orgAdminEmail: string;
  orgAdminName: string;
  setOrganizationCity: (value: string) => void;
  setOrganizationEmail: (value: string) => void;
  setOrganizationName: (value: string) => void;
  setOrganizationWebsite: (value: string) => void;
  setOrgAdminEmail: (value: string) => void;
  setOrgAdminName: (value: string) => void;
}) {
  return (
    <section className="organization-panel panel">
      <div className="organization-header">
        <div>
          <p className="eyebrow">School onboarding</p>
          <h3>Create institute and admin account</h3>
          <p>
            Use this after you verify a school contact. The school admin receives a one-time setup
            link, and students join later with the invite code.
          </p>
        </div>
      </div>

      <form className="organization-form organization-form-admin" onSubmit={handleCreateOrgAdmin}>
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
          Institute contact email
          <div className="field-shell">
            <Icon name="mail" />
            <input
              onChange={(event) => setOrganizationEmail(event.target.value)}
              placeholder="office@school.edu.pk"
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

        <label>
          Admin full name
          <div className="field-shell">
            <Icon name="badge" />
            <input
              onChange={(event) => setOrgAdminName(event.target.value)}
              placeholder="Teacher or principal name"
              value={orgAdminName}
            />
          </div>
        </label>

        <label>
          Admin email
          <div className="field-shell">
            <Icon name="alternate_email" />
            <input
              onChange={(event) => setOrgAdminEmail(event.target.value)}
              placeholder="teacher@school.edu.pk"
              type="email"
              value={orgAdminEmail}
            />
          </div>
        </label>

        <button className="primary-button" disabled={organizationBusy} type="submit">
          <span>{organizationBusy ? 'Creating...' : 'Create account'}</span>
          <Icon name="person_add" />
        </button>
      </form>

      {organizationsError ? (
        <p className="inline-feedback inline-feedback-error">{organizationsError}</p>
      ) : null}
      {organizationNotice ? (
        <p className="inline-feedback inline-feedback-success">{organizationNotice}</p>
      ) : null}

      {createdOrgAdmin ? (
        <article className="setup-result">
          <div>
            <p className="eyebrow">Created account</p>
            <h4>{createdOrgAdmin.organizationName}</h4>
            <p>
              Admin: {createdOrgAdmin.orgAdminEmail} | Student invite code:{' '}
              <strong>{createdOrgAdmin.inviteCode}</strong>
            </p>
          </div>
          <label>
            One-time setup link
            <textarea readOnly rows={3} value={createdOrgAdmin.setupLink} />
          </label>
          <button className="secondary-button" onClick={copyCreatedSetupLink} type="button">
            <Icon name="content_copy" />
            <span>Copy setup link</span>
          </button>
        </article>
      ) : null}
    </section>
  );
}

function OrganizationPanel({
  isSuperAdmin,
  organizationBusy,
  organizationNotice,
  organizations,
  organizationsError,
  organizationsLoading,
  rotateInviteCode,
  selectedOrganization,
  selectedOrganizationId,
  setRefreshKey,
  setSelectedOrganizationId,
}: {
  isSuperAdmin: boolean;
  organizationBusy: boolean;
  organizationNotice: string | null;
  organizations: OrganizationRow[];
  organizationsError: string | null;
  organizationsLoading: boolean;
  rotateInviteCode: () => void;
  selectedOrganization: OrganizationRow | null;
  selectedOrganizationId: string;
  setRefreshKey: (updater: (value: number) => number) => void;
  setSelectedOrganizationId: (value: string) => void;
}) {
  return (
    <section className="organization-panel panel">
      <div className="organization-header">
        <div>
          <p className="eyebrow">{isSuperAdmin ? 'Verified schools' : 'Institute controls'}</p>
          <h3>{selectedOrganization ? selectedOrganization.name : 'Select an institute'}</h3>
          <p>
            {isSuperAdmin
              ? 'Select any institute to review roster, analytics, complaints, and invite code.'
              : 'Share the invite code with students who should appear in your school dashboard.'}
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
              <span>Student invite code</span>
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
  );
}

function OrganizationDashboard({
  isSuperAdmin,
  maxDailyTranslations,
  organizationDailyUsage,
  organizationDataError,
  organizationDataLoading,
  organizationOverview,
  organizations,
  organizationTopSigns,
  organizationUsers,
  selectedOrganization,
  setSelectedOrganizationId,
}: {
  isSuperAdmin: boolean;
  maxDailyTranslations: number;
  organizationDailyUsage: OrganizationDailyUsageRow[];
  organizationDataError: string | null;
  organizationDataLoading: boolean;
  organizationOverview: OrganizationOverviewRow | null;
  organizations: OrganizationRow[];
  organizationTopSigns: OrganizationTopSignRow[];
  organizationUsers: OrganizationUserRow[];
  selectedOrganization: OrganizationRow;
  setSelectedOrganizationId: (value: string) => void;
}) {
  return (
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
        {isSuperAdmin ? (
          <article className="panel organization-list-card">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">Organization directory</p>
                <h3>{organizations.length} institutes</h3>
              </div>
            </div>
            <div className="roster-table">
              {organizations.map((organization) => (
                <button
                  className="roster-row organization-directory-row"
                  key={organization.id}
                  onClick={() => setSelectedOrganizationId(organization.id)}
                  type="button"
                >
                  <div>
                    <strong>{organization.name}</strong>
                    <span>{organization.contact_email || organization.website_url || organization.id}</span>
                  </div>
                  <span>{organization.city || 'No city'}</span>
                  <span>{organization.status}</span>
                  <span>{organization.invite_code || 'No code'}</span>
                </button>
              ))}
            </div>
          </article>
        ) : null}

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
                                  (Number(item.translation_count) / maxDailyTranslations) * 100,
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
  );
}

function ComplaintWorkspace({
  adminNoteDraft,
  clearFilters,
  complaints,
  complaintMetrics,
  filteredResultLabel,
  isSuperAdmin,
  listError,
  listLoading,
  pendingCount,
  refreshList,
  saveBusy,
  saveComplaint,
  saveError,
  saveNotice,
  selectedComplaint,
  selectedComplaintId,
  setAdminNoteDraft,
  setSelectedComplaintId,
  setSourceFilter,
  setStatusDraft,
  setStatusFilter,
  sourceFilter,
  statusDraft,
  statusFilter,
}: {
  adminNoteDraft: string;
  clearFilters: () => void;
  complaints: ComplaintRow[];
  complaintMetrics: Record<ComplaintStatus, number>;
  filteredResultLabel: string;
  isSuperAdmin: boolean;
  listError: string | null;
  listLoading: boolean;
  pendingCount: number;
  refreshList: () => void;
  saveBusy: boolean;
  saveComplaint: () => void;
  saveError: string | null;
  saveNotice: string | null;
  selectedComplaint: ComplaintRow | null;
  selectedComplaintId: string | null;
  setAdminNoteDraft: (value: string) => void;
  setSelectedComplaintId: (value: string) => void;
  setSourceFilter: (value: SourceFilter) => void;
  setStatusDraft: (value: ComplaintStatus) => void;
  setStatusFilter: (value: StatusFilter) => void;
  sourceFilter: SourceFilter;
  statusDraft: ComplaintStatus;
  statusFilter: StatusFilter;
}) {
  return (
    <>
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
          <button className="secondary-button" onClick={refreshList} type="button">
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
          </div>
          <h3>No complaints found matching your filters.</h3>
          <p>
            {isSuperAdmin
              ? 'The selected institute does not have matching complaints.'
              : 'Your institute does not have matching complaints.'}
          </p>
          <div className="empty-actions">
            <button className="primary-button" onClick={clearFilters} type="button">
              <span>Reset all filters</span>
              <Icon name="restart_alt" />
            </button>
            <button className="secondary-button" onClick={refreshList} type="button">
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
              <div className="detail-layout">
                <div className="detail-main">
                  <article className="visual-card panel">
                    <div className="visual-copy">
                      <p className="eyebrow">Complaint</p>
                      <h3>{complaintHeadline(selectedComplaint)}</h3>
                      <blockquote>
                        {selectedComplaint.note ||
                          'No user note was submitted with this complaint.'}
                      </blockquote>
                    </div>
                  </article>

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
                      </dl>
                    </article>

                    <article className="context-card panel">
                      <p className="eyebrow">Prediction context</p>
                      <dl>
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
                        <span>{selectedComplaint.reporter_email || selectedComplaint.reporter_id}</span>
                      </div>
                    </div>
                    <dl>
                      <div>
                        <dt>Organization</dt>
                        <dd>{selectedComplaint.organization_name || 'Not linked'}</dd>
                      </div>
                      <div>
                        <dt>Created</dt>
                        <dd>{formatDate(selectedComplaint.created_at)}</dd>
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
                          onChange={(event) => setStatusDraft(event.target.value as ComplaintStatus)}
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
            )}
          </section>
        </section>
      )}
    </>
  );
}
