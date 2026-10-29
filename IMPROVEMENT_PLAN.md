# Hazard & Reagent Tracker Improvement Record

This file records the implementation present in the repository after the dashboard and UI review. Status labels describe work in source and verification; they do not imply that a live MySQL session was available.

## Current architecture and functionality

- **Completed:** Java Swing GUI → service → DAO → MySQL structure remains in place. Authentication still opens role-specific dashboards. Chemical, request, inventory, read-only storage-safety, and teacher-management screens retain their workflows.
- **Completed:** `Theme` centralizes the application palette, typography, cards, input styling, table styling, and screen sizing. `RoundedButton` is used for visible buttons on login, dashboards, forms, dialogs, tables, and search/filter controls.
- **Completed:** Java2D bar, grouped-bar, and donut charts display empty states and repaint on resize. No dependency changes were introduced.
- **Partially completed:** Dashboard database reads now use `SwingWorker`; refresh results are applied on the Swing Event Dispatch Thread. Other screens still make service calls synchronously.

## Problems found and changes implemented

- **Completed:** “Most requested” previously summed item quantities, which made values incomparable across units and measured quantity rather than frequency. `DashboardService.topRequestedChemicals` now counts each chemical once per request and ranks by request occurrence.
- **Completed:** Teacher availability previously ranked raw quantity across unlike units. It now charts inventory-record counts by mutually exclusive status instead.
- **Completed:** Admin’s “Approved This Month” metric uses `request_date`, which is submission time; its label and tooltip now state this proxy. There is no approval timestamp in the current schema.
- **Completed:** Admin stock/minimum comparisons use a table with separate current and minimum columns, each with its unit. Rows are ordered by per-chemical quantity-to-minimum ratio; quantities from different chemicals are not added.
- **Completed:** Admin and teacher dashboards maximize after login. Teacher request data continues to be fetched by authenticated teacher ID. Duplicate logout/new-request controls were removed from the dashboard layouts.
- **Partially completed:** Login accepts free-form email input and offers up to eight matching database email addresses in a dropdown after a short typing pause. Suggestions do not constrain or rewrite input; selecting one fills the field. Lookup runs off the Event Dispatch Thread and lookup failures leave manual typing/login available. Database-backed suggestion behavior was not interactively verified.
- **Partially completed:** Login was changed to a 600×360 landscape layout. Suggestions update in a persistent list popup after a debounce rather than hiding/rebuilding it on each keystroke; popup behavior still needs interactive verification on macOS.
- **Completed:** New chemical forms use fixed location choices: Flammable, Acid Corrosive, Base Corrosive, Oxidizer, Others, and None. Legacy cabinet labels are displayed using their stored storage group; editing and saving maps them to the new category. The seed data uses the new labels.
- **Completed:** Active inventory names are checked case-insensitively before add and edit. This is an application-level check; there is no database unique constraint, so concurrent independent app instances could still race.
- **Completed:** Chemical search includes generated `C###` codes. Hazard values are displayed and saved with a space after commas. The request form explicitly shows the selected chemical name.
- **Completed:** Compatibility-rule CRUD has been removed. Fixed application rules are displayed read-only: Acid–Base, Acid–Flammable, Base–Flammable, Oxidizer–Flammable, Acid–Cyanide, and Oxidizer–Organic Material. Cyanide and Organic Material can be selected as hazard classifications. The table was removed from the fresh schema; a same-named table in an existing database is left untouched and is no longer read or modified by the app.
- **Completed:** Admins can request a teacher safety clarification email for pending requests containing a fixed incompatible pair; the request stays pending. New rejection transitions and the reject button are removed. Historical `REJECTED` status values remain displayable.
- **Completed:** Teacher-cancelled requests are final in the admin UI and service; admins cannot change their status.
- **Completed:** Donut charts allocate more space between the ring and legend.
- **Completed:** Needs Attention retains expiration, low-stock, and pending-request entries, prioritizing expired → low stock → expiring soon → pending.
- **Partially completed:** Dashboard refresh errors show a database-related message. They are not tested against an unavailable live server. Existing non-dashboard screens retain their existing error handling.

## KPI, chart, and business-rule definitions

- **Total chemicals:** number of active chemical records returned by `ChemicalService.getAll()`.
- **Low stock:** `quantity <= minimum_stock`; zero quantity is included. The KPI can include expired records because it measures the stock threshold independently.
- **Expired:** expiration date strictly before today. **Expiring soon:** today through today + 30 days inclusive. A missing date is neither.
- **Inventory health:** one category per chemical using precedence Expired → Expiring Soon → Low Stock → OK; categories do not overlap.
- **Pending requests:** requests whose status is `PENDING`.
- **Approved this month:** currently approved requests whose submission date falls in this calendar month. This is not the month of approval.
- **Request status:** `PENDING`, `APPROVED`, legacy `REJECTED`, `CANCELLED`, counted from loaded requests. New rejection actions are disabled.
- **Most requested chemical:** number of distinct requests containing that chemical; request quantities are not summed. Admin includes current PENDING and APPROVED requests; teacher includes that teacher’s own requests.
- **Hazards:** comma-separated `hazard_type` entries are trimmed and counted as classifications. Duplicate labels within one field are currently counted more than once.
- **Stock movement:** actual ADD and RELEASE transaction rows in the last 30 days, grouped by week. ADJUSTMENT is excluded; this chart describes recorded additions/releases, not net change.
- **Teacher availability:** number of active chemical records in each mutually exclusive inventory-health category. This avoids comparing quantities with different units.
- **Request trends:** grouped by request submission month. The schema has no approval/completion timestamp.

## UI standards and rounded buttons

- **Completed:** shared colors and readable fonts are defined in `Theme`; tables use zebra rows, clear headers, and selection styling. Inputs receive shared padding and borders.
- **Completed:** `RoundedButton` supports primary, secondary, and danger styles, hover/pressed/disabled appearances, pointer cursor, and visible keyboard focus ring.
- **Completed:** Login is centered at 600×360; other operational windows use 90% of screen dimensions; dashboards use maximized windows with scrollable content.
- **Completed:** The fixed storage pairs are labeled as lab policy. DOLE D.O. 136-14, Section 6(4), requires segregation under GHS physical-hazard criteria and the SDS storage requirements; it does not itself enumerate these exact pairs.
- **Partially completed:** Visual behavior was source-reviewed, not interactively verified across operating systems or display sizes. Some chart labels necessarily truncate long names.
- **Not implemented:** Specialized numeric/date table alignment and per-column width tuning on every screen.

## Testing and verification

- **Completed:** `mvn -q compile` passed after the login, chemical, chart, and request workflow changes.
- **Completed:** `mvn -q test` passed. The repository contains no `src/test` test sources, so this confirms the Maven test phase completes but does not provide behavioral test coverage.
- **Completed:** `git diff --check` passed during the dashboard changes; the source tree continues to use shared rounded buttons.
- **Not implemented:** Interactive Admin and Teacher workflow tests, chart rendering checks with a live GUI, database-backed metric comparison, empty-database validation, and database outage validation.
- **Not verified:** Runtime startup and MySQL behavior; credentials and a configured server were not exercised in this pass.

## Limitations and future recommendations

- **Completed:** `database/schema.sql` and `database/seed.sql` are present. The schema has submission time but no approval timestamp. The seed retains the existing admin and teacher records and inserts 25 chemical rows.
- **Partially completed:** `CREATE TABLE IF NOT EXISTS` does not alter tables in an existing database. Existing installations need a reviewed migration to adopt the new storage enums; no migration was run or added.
- **Recommended for future work:** Add automated aggregation tests covering date boundaries, null values, duplicate hazard labels, request frequency, and status precedence.
- **Recommended for future work:** Add approval/completion timestamps only through an approved schema migration if month-of-approval reporting is required.
- **Recommended for future work:** Consider table renderers for urgency/status and column sizing after interactive review on supported platforms.
- **Recommended for future work:** Move longer service calls on non-dashboard screens to background workers if observed to block the interface.
- **Completed:** The database schema now constrains storage group/location values to the current application categories and no longer creates the obsolete compatibility-rule table. No stock transaction history or caching was added.
