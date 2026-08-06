# PitchMind LinkedIn MCP Server

Small local MCP server for publishing normal LinkedIn feed posts through the official LinkedIn API.

It intentionally lives outside the Spring/React product code. The server is an automation tool, not part of the PitchMind runtime.

## What It Supports

- Build a LinkedIn OAuth authorization URL.
- Exchange an OAuth authorization code for an access token.
- Dry-run a text post payload.
- Dry-run an image post payload.
- Publish a text post only when `dryRun` is `false` and `confirmPublish` is `true`.
- Upload one PNG/JPG/GIF image and publish an image post only when `dryRun` is `false` and `confirmPublish` is `true`.

The extra publish confirmation is deliberate. A LinkedIn post is an external side effect and should never happen by accident.

## LinkedIn Requirements

Create a LinkedIn Developer App and request the required scopes:

- `w_member_social`: publish as the authenticated member. This comes from the Share on LinkedIn product.
- `w_organization_social`: publish as an organization/page where the authenticated member has the required page role. This usually requires an app/product upgrade or additional LinkedIn approval.

For organization posting, LinkedIn restricts access to company pages where the authenticated member has one of the supported roles such as administrator/content admin. Use an author URN like:

```text
urn:li:organization:123456
```

For personal posting, use:

```text
urn:li:person:PERSON_ID
```

## Configuration

Copy `.env.example` to a local `.env` file or pass the same values through your MCP client environment:

```text
LINKEDIN_CLIENT_ID=...
LINKEDIN_CLIENT_SECRET=...
LINKEDIN_REDIRECT_URI=http://localhost:3000/callback
LINKEDIN_SCOPES=openid profile w_member_social
LINKEDIN_ACCESS_TOKEN=...
LINKEDIN_API_VERSION=202605
```

Do not commit real tokens or secrets. `.linkedin-token.json` is ignored by Git.

## Run

```bash
cd tools/linkedin-mcp-server
npm start
```

The server loads `tools/linkedin-mcp-server/.env` automatically when the file exists. MCP clients can still pass the same values through their own `env` block.

Example MCP client configuration:

```json
{
  "mcpServers": {
    "linkedin": {
      "command": "node",
      "args": [
        "C:/Users/Msi/Downloads/ai-coach-backend/tools/linkedin-mcp-server/src/server.mjs"
      ],
      "env": {
        "LINKEDIN_CLIENT_ID": "...",
        "LINKEDIN_CLIENT_SECRET": "...",
        "LINKEDIN_REDIRECT_URI": "http://localhost:3000/callback",
        "LINKEDIN_ACCESS_TOKEN": "...",
        "LINKEDIN_API_VERSION": "202605"
      }
    }
  }
}
```

## OAuth Flow

Fast local helper:

```bash
cd tools/linkedin-mcp-server
npm run auth
```

The helper:

1. Loads `.env`.
2. Starts a local callback server at `LINKEDIN_REDIRECT_URI`.
3. Prints a local start URL, normally `http://localhost:3000/start`.
4. Exchanges the returned authorization code.
5. Saves the token response to `.linkedin-token.json`.
6. Prints the access token so you can put it into `LINKEDIN_ACCESS_TOKEN`.

Open the local start URL instead of opening the callback URL directly. The start URL creates a fresh OAuth `state` and redirects to LinkedIn.

Manual MCP-tool flow:

1. Call `linkedin.oauth_url`.
2. Open the returned URL in a browser.
3. Approve the LinkedIn scopes.
4. Copy the `code` query parameter from the redirect URL.
5. Call `linkedin.exchange_code` with `persistToken: true`.
6. Put the returned access token into `LINKEDIN_ACCESS_TOKEN`.

## Posting Flow

After OAuth, call `linkedin.me` first. It returns the authenticated member profile and a `personUrn` value:

```text
urn:li:person:PERSON_ID
```

Use dry-run first:

```json
{
  "authorUrn": "urn:li:person:PERSON_ID",
  "commentary": "Hello from PitchMind",
  "dryRun": true
}
```

Then publish explicitly:

```json
{
  "authorUrn": "urn:li:person:PERSON_ID",
  "commentary": "Hello from PitchMind",
  "dryRun": false,
  "confirmPublish": true
}
```

## Official API Notes

LinkedIn Posts API requires:

- `Authorization: Bearer ...`
- `Linkedin-Version: YYYYMM`
- `X-Restli-Protocol-Version: 2.0.0`

Image posts use:

1. `POST https://api.linkedin.com/rest/images?action=initializeUpload`
2. `PUT` the image bytes to the returned upload URL
3. `POST https://api.linkedin.com/rest/posts` with the returned `urn:li:image:...`
