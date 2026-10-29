# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Households and small businesses, on equal footing (user answer 2026-10-29). Households keep their pantry and cleaning supplies stocked and shop at chains such as D1, Olímpica and Éxito. Small businesses (shops, restaurants, workshops) track supplies and suppliers. Mobile-first for quick entry at the shelf or the store. Desktop tables for longer lists.

## Product Purpose

Inventoco tracks the inventory of anything, in any unit (units, weight, volume):
- minimum stock per item
- price history per product and supplier
- a "what's missing" shopping list that turns into a purchase
- a forecast of when each item will run out, from its recorded consumption

Users can share prices publicly and anonymously, so everyone finds cheaper suppliers. Success means fewer items running out and money saved.

## Positioning

It is a member of the Synapco family (the name ends in "co", like Finco, Restauco, Barberco and Spaco). It shares one authco account and exposes an MCP server, so Claude can record consumption and build the shopping list in plain language. Shared public prices are the network effect: what one user recorded helps the others.

## Operating Context

- The user records a purchase or a consumption on the phone. They check what to buy before going to the store.
- They complete the shopping list after the trip, with the prices paid.
- The same user may also run an inventory of a small business.
- International: times are stored as instants and shown in the user's time zone. The currency comes from the user's country. Prices are compared only within the same currency.

## Capabilities and Constraints

- One SPA for all users (like Finco). Everything is scoped to the signed-in user.
- Angular 19 at `inventoco-app/`. API at `/inventoco-api` (Spring Boot 4). MCP at `/inventoco-api/mcp`.
- Units: count (unit, dozen), mass (mg, g, kg, lb, oz), volume (ml, cl, l, gal, fl oz), length (mm, cm, m, in, ft, yd: fabric, cable, hose) and area (cm², m², ft²). Stored in base units (unit, g, ml, mm, cm²).
- Public prices never reveal who recorded them.
- Forecast confidence is LOW, MEDIUM or HIGH. It is LOW with fewer than 5 consumption records, and the UI must say so.

## Brand Commitments

- It follows the Synapco visual language (`~/code/synapco-design/DESIGN.md`, `final/shared/synapco.css`), as the user asked on 2026-10-29: "sigue el squema visual de synapco".
- It is always dark and dyed in its own product hue. There is one product light, used only in data. The primary button is white.
- Fonts: Antonio Light, Schibsted Grotesk, Martian Mono. The syllable "co" is set in `pulse`. A signal field sits behind every screen.
- No flags or national symbols, and no rosette or mandala ornaments.
- The Inventoco palette is proposed by Claude and approved by Leonard.
- Bilingual like the family: Spanish (es-CO) and English, chosen at the bottom of the sidebar (shared "synapco-lang" choice). API error messages are English.

## Evidence on Hand

There is no real data yet. Nothing may be invented: no testimonials, user counts or prices. Store-chain prices will only come from the planned catalog ingest (task T8).

## Product Principles

1. What to buy, and when, is visible at a glance.
2. Prices compare fairly: per unit, same currency, recent.
3. Recording takes seconds on a phone.
4. Sharing is opt-in and anonymous.
5. The assistant is one connection away (MCP), never in the way.
