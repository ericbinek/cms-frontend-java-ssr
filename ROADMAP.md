# Roadmap

The goal is a CMS whose content is structured and schema.org-native, rendered as semantic, predictable HTML over a strict API. The data stays machine-readable by design, which makes the whole system a clean substrate for automation and LLM-driven workflows.

This is a work-in-progress project (v0.3.0). The roadmap is deliberately loose, will grow, and the order can change based on what proves useful. Nothing here is a promise.

## Recently shipped

- Server-rendered, read-only list and detail views (content management lives in the admin interface)
- Previous and next page navigation that preserves the active sort and filter
- Safe links: only http, https, mailto, and site-relative URLs become clickable, anything else renders as inert text
- Semantic, accessible markup

## Planned

- More entity views as the vocabulary grows
- An accessibility pass over tables

## Considering

- A framework-based variant of this server
- A configurable page size

Have a need or an idea? Open an issue. This is built in public and feedback shapes the order.
