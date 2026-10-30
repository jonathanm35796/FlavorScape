# Lletya website

Static, responsive community website using original assets from `main/branding`. Publish `dist/` with any static host, including Cloudflare Pages. No build command or dependencies are required.

## Configure before launch

Edit `dist/config.js` with the public HTTPS launcher download URL and official voting links. Discord currently uses the invite in the main project README. Highscores intentionally show an empty state until a public, read-only leaderboard API is implemented. Never expose game databases, player save files or credentials to the browser.

## Cloudflare domain

Create a Cloudflare Pages project and upload the contents of `dist`, or connect a repository with this directory as the output. Add `lletya.com` under that project's Custom domains and follow Cloudflare's DNS instructions. Nameservers alone do not host the site. Do not overwrite existing mail or unrelated DNS records.

The Sites preview is separate from the production domain. `.openai/hosting.json` stores its identity.

## Local preview

Serve `dist` with any static HTTP server. Navigation uses hash URLs so it works without server rewrite rules. No live player counts or fabricated rankings are displayed.
