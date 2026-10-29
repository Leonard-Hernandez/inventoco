# Inventoco

Inventory of anything (units, weight, volume) for households and small businesses, part of the Synapco family:

- minimum stock and depletion forecasts from recorded consumption
- price history per supplier, shared anonymously by choice
- a "what's missing" shopping list with the cheapest supplier, and savings stats
- an MCP server, so Claude can record and answer in plain language

One SPA serves every user; each user only sees their own inventory.

| Part | Where | Port |
| --- | --- | --- |
| API (Spring Boot 4, Java 25) | `src/` | 8095, path `/inventoco-api` |
| SPA (Angular 19, Synapco design) | `inventoco-app/` | dev server 4500 |
| Gateway (nginx: `/`, `/inventoco-api`, `/authco`) | `deploy/gateway` | 8094 |
| MCP (Streamable HTTP, stateless) | `/inventoco-api/mcp` | via the gateway |

## Platform

Postgres and authco are shared with the other apps and run in `../platform` (database `inventoco`, role `inventoco`, SPA client `inventoco-app`). See `../platform/README.md` to add an origin (a tunnel URL, for example).

## Run

```sh
cp .env.example .env               # PASSWORD_DB = INVENTOCO_DB_PASSWORD of ../platform/.env
(cd ../platform && docker compose up -d)
docker compose up -d --build       # http://localhost:8094
```

Restart one service without touching the others: `docker compose up -d --build --no-deps inventoco-api`.

## Test

There is no Java on the host, so tests run in a container:

```sh
docker run --rm -v "$PWD":/app -v inventoco-gradle:/root/.gradle -w /app amazoncorretto:25-alpine ./gradlew test
(cd inventoco-app && docker run --rm -v "$PWD":/app -w /app node:22-alpine npx ng build)
```

## Connect Claude

In Claude go to Settings → Connectors → Add custom connector, and use `<PUBLIC_URL>/inventoco-api/mcp`. Sign in with the same account.

The tools cover:
- the profile
- items, consumption, purchases, counts and prices
- suppliers and public prices
- the forecast, the shopping list, savings and the best suppliers

## Design

The SPA follows the Synapco language (`../synapco-design`, version of 2026-10-26) with its own palette: an olive-black ground and a lime light (`data-product="inventoco"` in `inventoco-app/src/styles.css`). Product facts are in `PRODUCT.md`. Feature tracking is in `odd/tasks/inventoco.md`.
