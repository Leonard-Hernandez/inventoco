# Inventoco
Objective: multi-user inventory app (any unit) with price history, public prices, shopping list, savings and depletion prediction, MCP server, one SPA for all users.
Branch: `feat/inventoco-v1` · Delivery: ask-on-risk · Runner: `./gradlew test` (in Docker, amazoncorretto:25) and `npx ng build`.
Plan: `~/.claude/plans/quiro-crear-una-app-glistening-biscuit.md`

## Specs
- S1 Multi-user: each user only sees and changes their own inventory; one frontend for all users ("un solo front end para todos los usuarios").
- S2 Inventory of anything: "unidades, pesos litros etc" — items with a dimension (count, mass, volume), quantities converted between units.
- S3 Minimum stock per item; low-stock alert.
- S4 Price history: "registrar los diferentes precios de un producto a lo largo del tiempo como tambien sus proveedores".
- S5 Public prices: "el user pueda elejir si hacer esa info publica y asi ayudar a los demas a conseguir mejores precios"; public data never exposes the owner.
- S6 Time: "maneja las fechas con instancias y representa segun la utc del cliente" — Instant/timestamptz, shown in the user's time zone.
- S7 Currency: "monedas basadas en pais".
- S8 Stats: "estadisticacs de ahorros, mejores proveedores y cual seria el ahorro estimado".
- S9 Shopping: "una funcion para pedir que falta y hacer la compra".
- S10 Prediction: "predecir cuando se vav a acabar algo y asi tomar ventaja antes".
- S11 MCP server ("preferiblemente que tenga mcp").
- S12 (after v1) Seed the Colombian catalog from D1, Olímpica, Éxito; Hermes MCP only with explicit authorization.

## Tasks
| ID | Specs | Route | Status | Commit / evidence |
|----|-------|-------|--------|-------------------|
| T0 | S1 | inline | done | platform (not git): compose/.env/init-databases + live role/db + `inventoco-app` client; authco recreated, backups in ../platform/backups/pre-inventoco |
| T1 | S1,S11 | inline | done | 9e4e24b, 00811ff |
| T2 | S2,S3,S6,S7 | inline | done | fb0bfde |
| T3 | S4,S5 | inline | done | fb0bfde |
| T4 | S9 | inline | done | fb0bfde |
| T5 | S8,S10 | inline | done | fb0bfde |
| T6 | S11 | inline | done | 13eacce |
| T7 | S1-S10 | inline | done | 05046de |
| T8 | S12 | inline | pending | — | Catalog ingest (Colombia) |

## Log
- L1 (2026-10-29, verbatim): "quiro crear una app llamada Inventoco basada en las demas que estan en este repositorio va a hacer multi usuario lo ideal es que tenga mcp y permita llevar el inventario de lo que sea, unidades, pesos litros etc, que se pueda poner stock minimo y tambien hacer un seguimiento de precios de manera que se pueda registrar los diferentes precios de un producto a lo largo del tiempo como tambien sus proveedores, de manera que el user pueda elejir si hacer esa info publica y asi ayudar a los demas a conseguir mejores precios de sus productos, como es internacional maneja las fechas con instancias y representa segun la utc del cliente, monedas basadas en pais y estadisticacs de ahorros, mejores proveedores y cual seria el ahorro estimado, poder manejar stock minimo y una funcion para pedir que falta y hacer la compra, si se puede agregar alguna funcion predictiva basaada en los consumos antes registrados para predecir cuando se vav a acabar algo y asi tomar ventaja antes, seria genial, este projecto tiene que ser como finco un solo fornt end para todos los usuarios y preferiblemente que tenga mcp"
- L2 (2026-10-29, verbatim): "Después de la ejecución puedes hacer scrappind de webs como el d1, olímpica, exito y demás para alimentar el catálogo en colombia, en el server Hermes tiene un mcp de inventario en el que puedes ayudarte con cierta información"
- L3: Stack decision — spaco skeleton (Boot 4.0.6, Java 25, shared platform Postgres + authco, hand-rolled MCP), finco's per-user model. API :8095, gateway :8094.
- L4 (2026-10-29): user answered "Both equally" (households and small businesses); then, verbatim: "sigue el squema visual de synapco"; palette approved: "Lime (Recommended)" (ground #0d1003, accent #cde736).
- L5: Evidence — backend 37/37 tests green in Docker (amazoncorretto:25); API container healthy against shared Postgres, Flyway V1 applied; gateway :8094 serves SPA/config, MCP returns 401 + WWW-Authenticate + RFC 9728 metadata; authco issues `http://localhost:8094/authco`. SPA `ng build` clean; headless screenshots (mocked API) desktop 1440 + phone 390, es/en, no console errors, no horizontal overflow; Impeccable detector: 0 findings. Not verified: a real Google sign-in round trip (needs a browser on Leonard's side).
- L6: Decisions — supplier visibility dropped (prices carry visibility; supplier name shown with public prices); realized savings count only prices tied to a purchase; Synapco font URL `Martian+Mono:wdth,wght@75..112.5,400;500` returns HTTP 400 (fixed here to `400..500`; synapco-design itself not touched). Tests were written alongside the code, not RED-first.
- L7: Next — independent verifier (risk items 2 and 5), then T8 (catalog ingest), which needs Hermes access to be authorized explicitly.
- L8: Engram mirror `odd/inventoco/tasks` PENDING: mem_save returned ambiguous_project (server cwd ~/code, offers only authco / prism-llama.cpp).
