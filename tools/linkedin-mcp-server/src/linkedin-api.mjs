import { readFile } from 'node:fs/promises';
import { randomBytes } from 'node:crypto';
import { extname, resolve } from 'node:path';

export const LINKEDIN_API_BASE = 'https://api.linkedin.com';

export function validateConfig() {
  const required = ['LINKEDIN_CLIENT_ID', 'LINKEDIN_CLIENT_SECRET', 'LINKEDIN_REDIRECT_URI'];
  const optional = ['LINKEDIN_ACCESS_TOKEN', 'LINKEDIN_SCOPES', 'LINKEDIN_API_VERSION'];
  const missing = required.filter((key) => !process.env[key]);
  const configured = [...required, ...optional].reduce((acc, key) => {
    acc[key] = Boolean(process.env[key]);
    return acc;
  }, {});

  return {
    ok: missing.length === 0,
    missing,
    configured,
    apiVersion: apiVersion(),
    defaultScopes: defaultScopes()
  };
}

export function buildOAuthUrl(args = {}) {
  const clientId = requiredEnv('LINKEDIN_CLIENT_ID');
  const redirectUri = args.redirectUri ?? requiredEnv('LINKEDIN_REDIRECT_URI');
  const scopes = args.scopes ?? defaultScopes();
  const state = args.state ?? cryptoRandomState();

  const url = new URL('https://www.linkedin.com/oauth/v2/authorization');
  url.searchParams.set('response_type', 'code');
  url.searchParams.set('client_id', clientId);
  url.searchParams.set('redirect_uri', redirectUri);
  url.searchParams.set('scope', scopes.join(' '));
  url.searchParams.set('state', state);

  return {
    authorizationUrl: url.toString(),
    redirectUri,
    scopes,
    state,
    nextStep: 'Open authorizationUrl, approve the scopes, then pass the returned code to linkedin.exchange_code.'
  };
}

export async function exchangeCode(args) {
  if (!args.code) {
    throw mcpError(-32602, 'code is required.');
  }

  const body = new URLSearchParams();
  body.set('grant_type', 'authorization_code');
  body.set('code', args.code);
  body.set('redirect_uri', args.redirectUri ?? requiredEnv('LINKEDIN_REDIRECT_URI'));
  body.set('client_id', requiredEnv('LINKEDIN_CLIENT_ID'));
  body.set('client_secret', requiredEnv('LINKEDIN_CLIENT_SECRET'));

  const response = await fetch('https://www.linkedin.com/oauth/v2/accessToken', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body
  });

  return readLinkedInResponse(response);
}

export async function createTextPost(args) {
  assertPostArgs(args);
  const payload = postPayload(args.authorUrn, args.commentary, { visibility: args.visibility });

  if (isDryRun(args)) {
    return {
      dryRun: true,
      endpoint: `${LINKEDIN_API_BASE}/rest/posts`,
      method: 'POST',
      payload
    };
  }

  assertConfirmed(args);
  const response = await linkedInFetch('/rest/posts', {
    method: 'POST',
    accessToken: accessToken(args),
    body: payload
  });

  return postResponse(response);
}

export async function getCurrentMember(args = {}) {
  const token = accessToken(args);
  const response = await fetch('https://api.linkedin.com/v2/userinfo', {
    headers: { Authorization: `Bearer ${token}` }
  });
  const payload = await readLinkedInResponse(response);

  return {
    ...payload,
    personUrn: payload?.sub ? `urn:li:person:${payload.sub}` : null
  };
}

export async function createImagePost(args) {
  assertPostArgs(args);
  if (!args.imagePath) {
    throw mcpError(-32602, 'imagePath is required.');
  }

  const ownerUrn = args.ownerUrn ?? args.authorUrn;
  const resolvedImagePath = resolve(args.imagePath);
  const contentType = imageContentType(resolvedImagePath);
  const createPayload = postPayload(args.authorUrn, args.commentary, {
    visibility: args.visibility,
    media: {
      id: 'urn:li:image:{created-after-upload}',
      ...(args.altText ? { altText: args.altText } : {})
    }
  });

  if (isDryRun(args)) {
    return {
      dryRun: true,
      steps: [
        {
          method: 'POST',
          endpoint: `${LINKEDIN_API_BASE}/rest/images?action=initializeUpload`,
          payload: { initializeUploadRequest: { owner: ownerUrn } }
        },
        {
          method: 'PUT',
          endpoint: '{uploadUrl returned by initializeUpload}',
          imagePath: resolvedImagePath,
          contentType
        },
        {
          method: 'POST',
          endpoint: `${LINKEDIN_API_BASE}/rest/posts`,
          payload: createPayload
        }
      ]
    };
  }

  assertConfirmed(args);
  const token = accessToken(args);
  const imageUrn = await uploadImage({ ownerUrn, imagePath: resolvedImagePath, contentType, accessToken: token });
  const publishPayload = postPayload(args.authorUrn, args.commentary, {
    visibility: args.visibility,
    media: {
      id: imageUrn,
      ...(args.altText ? { altText: args.altText } : {})
    }
  });

  const response = await linkedInFetch('/rest/posts', {
    method: 'POST',
    accessToken: token,
    body: publishPayload
  });

  return {
    image: imageUrn,
    post: postResponse(response)
  };
}

export async function deletePost(args) {
  if (!args.postUrn) {
    throw mcpError(-32602, 'postUrn is required.');
  }

  const endpoint = `/rest/posts/${encodeURIComponent(args.postUrn)}`;
  if (isDryRun(args)) {
    return {
      dryRun: true,
      method: 'DELETE',
      endpoint: `${LINKEDIN_API_BASE}${endpoint}`,
      postUrn: args.postUrn
    };
  }

  if (args.confirmDelete !== true) {
    throw mcpError(-32602, 'Live deletion requires confirmDelete: true.');
  }

  const response = await linkedInFetch(endpoint, {
    method: 'DELETE',
    accessToken: accessToken(args)
  });

  return {
    ok: response.ok,
    status: response.status,
    postUrn: args.postUrn
  };
}

export async function getOrganizationAcls(args = {}) {
  const params = new URLSearchParams({ q: 'roleAssignee' });
  if (args.role) {
    params.set('role', args.role);
  }

  const response = await linkedInFetch(`/rest/organizationAcls?${params.toString()}`, {
    method: 'GET',
    accessToken: accessToken(args)
  });

  return response.payload;
}

export function redactTokenPayload(payload) {
  if (!payload || typeof payload !== 'object') {
    return payload;
  }

  return {
    ...payload,
    access_token: payload.access_token ? `${payload.access_token.slice(0, 8)}...redacted` : undefined,
    refresh_token: payload.refresh_token ? `${payload.refresh_token.slice(0, 8)}...redacted` : undefined,
    id_token: payload.id_token ? `${payload.id_token.slice(0, 8)}...redacted` : undefined
  };
}

export function mcpError(code, message) {
  const error = new Error(message);
  error.code = code;
  return error;
}

async function uploadImage({ ownerUrn, imagePath, contentType, accessToken }) {
  const initialize = await linkedInFetch('/rest/images?action=initializeUpload', {
    method: 'POST',
    accessToken,
    body: { initializeUploadRequest: { owner: ownerUrn } }
  });

  const uploadUrl = initialize.payload?.value?.uploadUrl;
  const imageUrn = initialize.payload?.value?.image;
  if (!uploadUrl || !imageUrn) {
    throw mcpError(-32000, 'LinkedIn initializeUpload response did not include uploadUrl and image URN.');
  }

  const imageBytes = await readFile(imagePath);
  const uploadResponse = await fetch(uploadUrl, {
    method: 'PUT',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': contentType
    },
    body: imageBytes
  });

  if (!uploadResponse.ok) {
    throw mcpError(-32000, `LinkedIn image upload failed: ${uploadResponse.status} ${await uploadResponse.text()}`);
  }

  return imageUrn;
}

async function linkedInFetch(path, { method, accessToken, body }) {
  const response = await fetch(`${LINKEDIN_API_BASE}${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
      'Linkedin-Version': apiVersion(),
      'X-Restli-Protocol-Version': '2.0.0'
    },
    body: body == null ? undefined : JSON.stringify(body)
  });

  return {
    ok: response.ok,
    status: response.status,
    restliId: response.headers.get('x-restli-id'),
    payload: await readLinkedInResponse(response)
  };
}

async function readLinkedInResponse(response) {
  const text = await response.text();
  const payload = text ? safeJsonParse(text) : null;

  if (!response.ok) {
    const detail = typeof payload === 'string' ? payload : JSON.stringify(payload);
    throw mcpError(-32000, `LinkedIn API error ${response.status}: ${detail}`);
  }

  return payload;
}

function postPayload(authorUrn, commentary, { visibility = 'PUBLIC', media } = {}) {
  return {
    author: authorUrn,
    commentary,
    visibility,
    distribution: {
      feedDistribution: 'MAIN_FEED',
      targetEntities: [],
      thirdPartyDistributionChannels: []
    },
    ...(media ? { content: { media } } : {}),
    lifecycleState: 'PUBLISHED',
    isReshareDisabledByAuthor: false
  };
}

function postResponse(response) {
  return {
    ok: response.ok,
    status: response.status,
    postUrn: response.restliId,
    postUrl: response.restliId ? `https://www.linkedin.com/feed/update/${response.restliId}/` : null,
    payload: response.payload
  };
}

function assertPostArgs(args) {
  if (!args.authorUrn) {
    throw mcpError(-32602, 'authorUrn is required.');
  }
  if (!args.commentary) {
    throw mcpError(-32602, 'commentary is required.');
  }
}

function assertConfirmed(args) {
  if (args.confirmPublish !== true) {
    throw mcpError(-32602, 'Live publishing requires confirmPublish: true.');
  }
}

function isDryRun(args) {
  return args.dryRun !== false;
}

function accessToken(args) {
  return args.accessToken ?? requiredEnv('LINKEDIN_ACCESS_TOKEN');
}

function apiVersion() {
  return process.env.LINKEDIN_API_VERSION || '202605';
}

function defaultScopes() {
  return (process.env.LINKEDIN_SCOPES || 'w_member_social w_organization_social')
    .split(/\s+/)
    .filter(Boolean);
}

function imageContentType(filePath) {
  const ext = extname(filePath).toLowerCase();
  if (ext === '.png') return 'image/png';
  if (ext === '.jpg' || ext === '.jpeg') return 'image/jpeg';
  if (ext === '.gif') return 'image/gif';
  throw mcpError(-32602, 'imagePath must point to a PNG, JPG, JPEG, or GIF file.');
}

function requiredEnv(name) {
  const value = process.env[name];
  if (!value) {
    throw mcpError(-32602, `${name} environment variable is required.`);
  }
  return value;
}

function cryptoRandomState() {
  return randomBytes(16).toString('hex');
}

function safeJsonParse(text) {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}
