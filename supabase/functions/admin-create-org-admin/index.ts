import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.57.4';
import type { User } from 'https://esm.sh/@supabase/supabase-js@2.57.4';

type CreateOrgAdminRequest = {
  organizationName?: string;
  city?: string | null;
  contactEmail?: string | null;
  websiteUrl?: string | null;
  adminFullName?: string | null;
  adminEmail?: string;
  redirectTo?: string;
};

type CreatedOrganizationRow = {
  organization_id: string;
  name: string;
  invite_code: string;
};

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
};

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      'Content-Type': 'application/json',
    },
  });
}

function cleanString(value: string | null | undefined) {
  const cleaned = value?.trim() ?? '';
  return cleaned.length ? cleaned : null;
}

function isExistingUserError(message: string) {
  const normalized = message.toLowerCase();
  return (
    normalized.includes('already') ||
    normalized.includes('registered') ||
    normalized.includes('exists')
  );
}

async function findUserByEmail(
  adminClient: ReturnType<typeof createClient>,
  email: string,
): Promise<User | null> {
  let page = 1;
  let lastPage = 1;

  do {
    const { data, error } = await adminClient.auth.admin.listUsers({
      page,
      perPage: 1000,
    });

    if (error) {
      throw new Error(error.message);
    }

    const foundUser = data.users.find(
      (user) => user.email?.toLowerCase() === email.toLowerCase(),
    );

    if (foundUser) {
      return foundUser;
    }

    lastPage = data.lastPage || page;
    page += 1;
  } while (page <= lastPage);

  return null;
}

Deno.serve(async (request) => {
  if (request.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }

  if (request.method !== 'POST') {
    return jsonResponse({ error: 'Method not allowed' }, 405);
  }

  const supabaseUrl = Deno.env.get('SUPABASE_URL');
  const anonKey = Deno.env.get('SUPABASE_ANON_KEY');
  const serviceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY');

  if (!supabaseUrl || !anonKey || !serviceRoleKey) {
    return jsonResponse({ error: 'Supabase Edge Function environment is not configured.' }, 500);
  }

  const authorization = request.headers.get('Authorization');
  if (!authorization) {
    return jsonResponse({ error: 'Authentication required.' }, 401);
  }

  let payload: CreateOrgAdminRequest;
  try {
    payload = await request.json();
  } catch {
    return jsonResponse({ error: 'Request body must be valid JSON.' }, 400);
  }

  const organizationName = cleanString(payload.organizationName);
  const adminEmail = cleanString(payload.adminEmail)?.toLowerCase() ?? null;
  const redirectTo = cleanString(payload.redirectTo);

  if (!organizationName) {
    return jsonResponse({ error: 'Organization name is required.' }, 400);
  }

  if (!adminEmail || !emailPattern.test(adminEmail)) {
    return jsonResponse({ error: 'A valid organization admin email is required.' }, 400);
  }

  if (!redirectTo) {
    return jsonResponse({ error: 'A password setup redirect URL is required.' }, 400);
  }

  const userClient = createClient(supabaseUrl, anonKey, {
    global: {
      headers: {
        Authorization: authorization,
      },
    },
    auth: {
      autoRefreshToken: false,
      persistSession: false,
    },
  });

  const adminClient = createClient(supabaseUrl, serviceRoleKey, {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
    },
  });

  const {
    data: { user: caller },
    error: callerError,
  } = await userClient.auth.getUser();

  if (callerError || !caller) {
    return jsonResponse({ error: 'Authentication required.' }, 401);
  }

  const { data: callerProfile, error: profileError } = await adminClient
    .from('profiles')
    .select('role')
    .eq('id', caller.id)
    .maybeSingle();

  if (profileError) {
    return jsonResponse({ error: profileError.message }, 500);
  }

  if (callerProfile?.role !== 'admin') {
    return jsonResponse({ error: 'Platform admin access required.' }, 403);
  }

  const adminFullName = cleanString(payload.adminFullName);
  const inviteMetadata = {
    full_name: adminFullName ?? '',
    organization_name: organizationName,
    account_type: 'organization_admin',
  };

  let targetUserId: string | null = null;
  let setupLink: string | null = null;
  let linkedExistingUser = false;

  const inviteResult = await adminClient.auth.admin.generateLink({
    type: 'invite',
    email: adminEmail,
    options: {
      data: inviteMetadata,
      redirectTo,
    },
  });

  if (inviteResult.error) {
    if (!isExistingUserError(inviteResult.error.message)) {
      return jsonResponse({ error: inviteResult.error.message }, 400);
    }

    const existingUser = await findUserByEmail(adminClient, adminEmail);
    if (!existingUser) {
      return jsonResponse({ error: inviteResult.error.message }, 400);
    }

    linkedExistingUser = true;
    targetUserId = existingUser.id;

    const recoveryResult = await adminClient.auth.admin.generateLink({
      type: 'recovery',
      email: adminEmail,
      options: {
        redirectTo,
      },
    });

    if (recoveryResult.error) {
      return jsonResponse({ error: recoveryResult.error.message }, 400);
    }

    setupLink = recoveryResult.data.properties?.action_link ?? null;
  } else {
    targetUserId = inviteResult.data.user?.id ?? null;
    setupLink = inviteResult.data.properties?.action_link ?? null;
  }

  if (!targetUserId || !setupLink) {
    return jsonResponse({ error: 'Unable to generate organization admin setup link.' }, 500);
  }

  const { data: createdRows, error: createOrganizationError } = await adminClient.rpc(
    'platform_create_organization_for_admin',
    {
      organization_name: organizationName,
      organization_city: cleanString(payload.city),
      organization_contact_email: cleanString(payload.contactEmail),
      organization_website_url: cleanString(payload.websiteUrl),
      organization_admin_user_id: targetUserId,
      created_by_user_id: caller.id,
      organization_admin_full_name: adminFullName,
    },
  );

  if (createOrganizationError) {
    return jsonResponse({ error: createOrganizationError.message }, 400);
  }

  const created = ((createdRows ?? []) as CreatedOrganizationRow[])[0];
  if (!created) {
    return jsonResponse({ error: 'Organization was not created.' }, 500);
  }

  return jsonResponse({
    organizationId: created.organization_id,
    organizationName: created.name,
    orgAdminEmail: adminEmail,
    setupLink,
    inviteCode: created.invite_code,
    linkedExistingUser,
  });
});
