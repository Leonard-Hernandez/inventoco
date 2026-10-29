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
| ID | Specs | Route | Status | Commit |
|----|-------|-------|--------|--------|
| T0 | S1 | inline | pending | — | Platform onboarding (../platform, not git) |
| T1 | S1,S11 | inline | pending | — | Scaffold API + gateway + compose |
| T2 | S2,S3,S6,S7 | inline | pending | — | Profile, units, items, movements |
| T3 | S4,S5 | inline | pending | — | Suppliers, prices, catalog, public view |
| T4 | S9 | inline | pending | — | Shopping list + complete purchase |
| T5 | S8,S10 | inline | pending | — | Stats + prediction |
| T6 | S11 | inline | pending | — | MCP tools |
| T7 | S1-S10 | inline | pending | — | SPA |
| T8 | S12 | inline | pending | — | Catalog ingest (Colombia) |

## Log
- L1 (2026-10-29, verbatim): "quiro crear una app llamada Inventoco basada en las demas que estan en este repositorio va a hacer multi usuario lo ideal es que tenga mcp y permita llevar el inventario de lo que sea, unidades, pesos litros etc, que se pueda poner stock minimo y tambien hacer un seguimiento de precios de manera que se pueda registrar los diferentes precios de un producto a lo largo del tiempo como tambien sus proveedores, de manera que el user pueda elejir si hacer esa info publica y asi ayudar a los demas a conseguir mejores precios de sus productos, como es internacional maneja las fechas con instancias y representa segun la utc del cliente, monedas basadas en pais y estadisticacs de ahorros, mejores proveedores y cual seria el ahorro estimado, poder manejar stock minimo y una funcion para pedir que falta y hacer la compra, si se puede agregar alguna funcion predictiva basaada en los consumos antes registrados para predecir cuando se vav a acabar algo y asi tomar ventaja antes, seria genial, este projecto tiene que ser como finco un solo fornt end para todos los usuarios y preferiblemente que tenga mcp"
- L2 (2026-10-29, verbatim): "Después de la ejecución puedes hacer scrappind de webs como el d1, olímpica, exito y demás para alimentar el catálogo en colombia, en el server Hermes tiene un mcp de inventario en el que puedes ayudarte con cierta información"
- L3: Stack decision — spaco skeleton (Boot 4.0.6, Java 25, shared platform Postgres + authco, hand-rolled MCP), finco's per-user model. API :8095, gateway :8094.
