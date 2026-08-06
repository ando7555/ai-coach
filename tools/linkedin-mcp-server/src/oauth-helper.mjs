#!/usr/bin/env node

import { randomBytes } from 'node:crypto';
import { createServer } from 'node:http';
import { readFile, writeFile } from 'node:fs/promises';
import { loadLocalEnv, serverRootDir } from './env.mjs';
import { buildOAuthUrl, exchangeCode, redactTokenPayload } from './linkedin-api.mjs';

await loadLocalEnv();

const redirectUri = process.env.LINKEDIN_REDIRECT_URI;
if (!redirectUri) {
  fail('LINKEDIN_REDIRECT_URI is required. Copy .env.example to .env and fill LinkedIn app credentials first.');
}

const redirectUrl = new URL(redirectUri);

if (!['localhost', '127.0.0.1'].includes(redirectUrl.hostname)) {
  fail('The local OAuth helper only supports localhost/127.0.0.1 redirect URIs.');
}

const port = Number(redirectUrl.port || (redirectUrl.protocol === 'https:' ? 443 : 80));
const callbackPath = redirectUrl.pathname;
const startPath = '/start';
const stateStore = new Set();

const server = createServer(async (request, response) => {
  try {
    const requestUrl = new URL(request.url, redirectUri);
    if (requestUrl.pathname === startPath || requestUrl.pathname === '/') {
      const state = randomBytes(16).toString('hex');
      stateStore.add(state);
      const oauth = buildOAuthUrl({ state, redirectUri });
      response.writeHead(302, { Location: oauth.authorizationUrl });
      response.end();
      console.log(`Started LinkedIn OAuth state ${state}`);
      return;
    }

    if (requestUrl.pathname !== callbackPath) {
      response.writeHead(404, { 'Content-Type': 'text/plain' });
      response.end('Not found');
      return;
    }

    const error = requestUrl.searchParams.get('error');
    if (error) {
      const description = requestUrl.searchParams.get('error_description') ?? '';
      response.writeHead(400, { 'Content-Type': 'text/plain' });
      response.end(`LinkedIn OAuth error: ${error}\n${description}`);
      shutdown(1, `LinkedIn OAuth error: ${error} ${description}`);
      return;
    }

    const state = requestUrl.searchParams.get('state');
    if (!state || !stateStore.has(state)) {
      response.writeHead(400, { 'Content-Type': 'text/html' });
      response.end(`<h1>Invalid OAuth state</h1><p>Open <a href="${startPath}">Start LinkedIn OAuth</a> and let LinkedIn redirect back here.</p>`);
      console.error('Invalid or missing OAuth state returned by LinkedIn.');
      return;
    }
    stateStore.delete(state);

    const code = requestUrl.searchParams.get('code');
    if (!code) {
      response.writeHead(400, { 'Content-Type': 'text/html' });
      response.end(`<h1>Missing OAuth code</h1><p>Open <a href="${startPath}">Start LinkedIn OAuth</a> and let LinkedIn redirect back here.</p>`);
      console.error('Missing OAuth code.');
      return;
    }

    const token = await exchangeCode({ code, redirectUri });
    const tokenPath = new URL('../.linkedin-token.json', import.meta.url);
    await writeFile(tokenPath, JSON.stringify(token, null, 2), 'utf8');
    await persistAccessTokenToEnv(token.access_token);

    response.writeHead(200, { 'Content-Type': 'text/html' });
    response.end('<h1>LinkedIn OAuth complete</h1><p>You can close this tab and return to Codex.</p>');

    console.log('\nOAuth complete. Token saved to:');
    console.log(`${serverRootDir()}\\.linkedin-token.json`);
    console.log('\nLINKEDIN_ACCESS_TOKEN was updated in the local .env file.');
    console.log('\nRedacted token response:');
    console.log(JSON.stringify(redactTokenPayload(token), null, 2));
    shutdown(0);
  } catch (error) {
    response.writeHead(500, { 'Content-Type': 'text/plain' });
    response.end(error.message);
    shutdown(1, error.message);
  }
});

server.listen(port, redirectUrl.hostname, () => {
  console.log('LinkedIn OAuth helper is listening on:');
  console.log(redirectUri);
  console.log('\nOpen this local start URL in your browser:');
  console.log(`${redirectUrl.origin}${startPath}`);
});

function shutdown(code, message) {
  if (message) {
    console.error(message);
  }
  setTimeout(() => {
    server.close(() => process.exit(code));
  }, 250);
}

function fail(message) {
  console.error(message);
  process.exit(1);
}

async function persistAccessTokenToEnv(accessToken) {
  const envPath = new URL('../.env', import.meta.url);
  let contents = '';

  try {
    contents = await readFile(envPath, 'utf8');
  } catch (error) {
    if (error.code !== 'ENOENT') {
      throw error;
    }
  }

  const tokenLine = `LINKEDIN_ACCESS_TOKEN=${accessToken}`;
  const updated = /^LINKEDIN_ACCESS_TOKEN=.*$/m.test(contents)
    ? contents.replace(/^LINKEDIN_ACCESS_TOKEN=.*$/m, tokenLine)
    : `${contents.trimEnd()}\n${tokenLine}\n`;

  await writeFile(envPath, updated, 'utf8');
}
