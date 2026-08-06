#!/usr/bin/env node

import { writeFile } from 'node:fs/promises';
import { createInterface } from 'node:readline';
import { loadLocalEnv } from './env.mjs';
import {
  buildOAuthUrl,
  createImagePost,
  createTextPost,
  deletePost,
  exchangeCode,
  getCurrentMember,
  getOrganizationAcls,
  mcpError,
  redactTokenPayload,
  validateConfig
} from './linkedin-api.mjs';

const SERVER_NAME = 'pitchmind-linkedin-mcp-server';
const SERVER_VERSION = '0.1.0';
const TOKEN_FILE = new URL('../.linkedin-token.json', import.meta.url);

await loadLocalEnv();

const tools = [
  {
    name: 'linkedin.validate_config',
    description: 'Check required LinkedIn MCP environment variables without exposing secret values.',
    inputSchema: {
      type: 'object',
      properties: {},
      additionalProperties: false
    }
  },
  {
    name: 'linkedin.oauth_url',
    description: 'Build the LinkedIn 3-legged OAuth authorization URL for the configured app and scopes.',
    inputSchema: {
      type: 'object',
      properties: {
        state: { type: 'string', description: 'Optional CSRF state value. A random value is generated when omitted.' },
        scopes: {
          type: 'array',
          items: { type: 'string' },
          description: 'Optional LinkedIn scopes. Defaults to LINKEDIN_SCOPES.'
        },
        redirectUri: { type: 'string', description: 'Optional redirect URI override.' }
      },
      additionalProperties: false
    }
  },
  {
    name: 'linkedin.exchange_code',
    description: 'Exchange a LinkedIn OAuth authorization code for an access token.',
    inputSchema: {
      type: 'object',
      required: ['code'],
      properties: {
        code: { type: 'string' },
        redirectUri: { type: 'string', description: 'Optional redirect URI override. Must match the authorization request.' },
        persistToken: {
          type: 'boolean',
          description: 'When true, writes the token response to .linkedin-token.json. Defaults to false.'
        }
      },
      additionalProperties: false
    }
  },
  {
    name: 'linkedin.me',
    description: 'Read the authenticated LinkedIn member profile from OIDC userinfo and return the person URN for posting.',
    inputSchema: {
      type: 'object',
      properties: {
        accessToken: { type: 'string', description: 'Optional token override. Defaults to LINKEDIN_ACCESS_TOKEN.' }
      },
      additionalProperties: false
    }
  },
  {
    name: 'linkedin.create_text_post',
    description: 'Create a normal LinkedIn feed text post. Defaults to dry-run and requires confirmPublish for live publish.',
    inputSchema: {
      type: 'object',
      required: ['authorUrn', 'commentary'],
      properties: {
        authorUrn: { type: 'string', description: 'urn:li:person:{id} or urn:li:organization:{id}' },
        commentary: { type: 'string' },
        accessToken: { type: 'string', description: 'Optional token override. Defaults to LINKEDIN_ACCESS_TOKEN.' },
        dryRun: { type: 'boolean', description: 'Defaults to true.' },
        confirmPublish: { type: 'boolean', description: 'Must be true when dryRun is false.' },
        visibility: { type: 'string', enum: ['PUBLIC'], description: 'Only PUBLIC is supported for this small server.' }
      },
      additionalProperties: false
    }
  },
  {
    name: 'linkedin.create_image_post',
    description: 'Upload one local image and create a normal LinkedIn feed image post. Defaults to dry-run and requires confirmPublish for live publish.',
    inputSchema: {
      type: 'object',
      required: ['authorUrn', 'commentary', 'imagePath'],
      properties: {
        authorUrn: { type: 'string', description: 'urn:li:person:{id} or urn:li:organization:{id}' },
        ownerUrn: { type: 'string', description: 'Optional image owner URN. Defaults to authorUrn.' },
        commentary: { type: 'string' },
        imagePath: { type: 'string', description: 'Absolute or workspace-relative PNG/JPG/GIF path.' },
        altText: { type: 'string' },
        accessToken: { type: 'string', description: 'Optional token override. Defaults to LINKEDIN_ACCESS_TOKEN.' },
        dryRun: { type: 'boolean', description: 'Defaults to true.' },
        confirmPublish: { type: 'boolean', description: 'Must be true when dryRun is false.' },
        visibility: { type: 'string', enum: ['PUBLIC'], description: 'Only PUBLIC is supported for this small server.' }
      },
      additionalProperties: false
    }
  },
  {
    name: 'linkedin.delete_post',
    description: 'Delete a LinkedIn post by URN. Defaults to dry-run and requires confirmDelete for live deletion.',
    inputSchema: {
      type: 'object',
      required: ['postUrn'],
      properties: {
        postUrn: { type: 'string', description: 'Post URN returned by LinkedIn, for example urn:li:share:{id}.' },
        accessToken: { type: 'string', description: 'Optional token override. Defaults to LINKEDIN_ACCESS_TOKEN.' },
        dryRun: { type: 'boolean', description: 'Defaults to true.' },
        confirmDelete: { type: 'boolean', description: 'Must be true when dryRun is false.' }
      },
      additionalProperties: false
    }
  },
  {
    name: 'linkedin.organization_acls',
    description: 'Read organization ACLs visible to the authenticated member, useful for finding org URNs for organization posting.',
    inputSchema: {
      type: 'object',
      properties: {
        accessToken: { type: 'string', description: 'Optional token override. Defaults to LINKEDIN_ACCESS_TOKEN.' },
        role: { type: 'string', description: 'Optional role filter, for example ADMINISTRATOR.' }
      },
      additionalProperties: false
    }
  }
];

const rl = createInterface({
  input: process.stdin,
  crlfDelay: Infinity
});

rl.on('line', async (line) => {
  if (!line.trim()) {
    return;
  }

  let request;
  try {
    request = JSON.parse(line);
  } catch (error) {
    writeJson(errorResponse(null, -32700, `Invalid JSON: ${error.message}`));
    return;
  }

  if (request.method?.startsWith('notifications/')) {
    return;
  }

  try {
    const result = await handleRequest(request);
    writeJson({ jsonrpc: '2.0', id: request.id, result });
  } catch (error) {
    writeJson(errorResponse(request.id, error.code ?? -32000, error.message));
  }
});

async function handleRequest(request) {
  switch (request.method) {
    case 'initialize':
      return {
        protocolVersion: request.params?.protocolVersion ?? '2024-11-05',
        capabilities: { tools: {} },
        serverInfo: { name: SERVER_NAME, version: SERVER_VERSION }
      };
    case 'tools/list':
      return { tools };
    case 'tools/call':
      return callTool(request.params?.name, request.params?.arguments ?? {});
    default:
      throw mcpError(-32601, `Unsupported method: ${request.method}`);
  }
}

async function callTool(name, args) {
  switch (name) {
    case 'linkedin.validate_config':
      return textResult(validateConfig());
    case 'linkedin.oauth_url':
      return textResult(buildOAuthUrl(args));
    case 'linkedin.exchange_code':
      return textResult(await exchangeCodeTool(args));
    case 'linkedin.me':
      return textResult(await getCurrentMember(args));
    case 'linkedin.create_text_post':
      return textResult(await createTextPost(args));
    case 'linkedin.create_image_post':
      return textResult(await createImagePost(args));
    case 'linkedin.delete_post':
      return textResult(await deletePost(args));
    case 'linkedin.organization_acls':
      return textResult(await getOrganizationAcls(args));
    default:
      throw mcpError(-32602, `Unknown tool: ${name}`);
  }
}

async function exchangeCodeTool(args) {
  const payload = await exchangeCode(args);
  const safePayload = redactTokenPayload(payload);

  if (args.persistToken === true) {
    await writeFile(TOKEN_FILE, JSON.stringify(payload, null, 2), 'utf8');
    safePayload.persistedTo = '.linkedin-token.json';
  }

  return safePayload;
}

function textResult(value) {
  return {
    content: [
      {
        type: 'text',
        text: typeof value === 'string' ? value : JSON.stringify(value, null, 2)
      }
    ]
  };
}

function writeJson(value) {
  process.stdout.write(`${JSON.stringify(value)}\n`);
}

function errorResponse(id, code, message) {
  return {
    jsonrpc: '2.0',
    id,
    error: { code, message }
  };
}
