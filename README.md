# Hazard & Reagent Tracker (Java + MySQL + JDBC + Swing)

A small school-laboratory app. **Admin + Teacher → Chemicals → Requests → Approval → Inventory → Gmail.**

## 1. System overview
One desktop program on one computer. It starts at the login page. The role decides the screen:
ADMIN → Admin Dashboard, TEACHER → Teacher Dashboard.
Users log in with their **email + password**; the same email receives the approval/rejection messages.
Teachers request chemicals for a class; the admin approves (stock is released, transaction recorded, email sent)
or rejects (reason saved, email sent).

## 2. Database design (MySQL, `database/schema.sql`)
```
users(id, name, password[SHA-256], email[UNIQUE = the login], role)
chemicals(id, name, quantity, unit, hazard_type, storage_group, storage_temperature,
          location, minimum_stock, expiration_date, active)
chemical_requests(id, teacher_id→users, class_name, experiment, status[PENDING|APPROVED|REJECTED|CANCELLED], rejection_reason, request_date)
request_items(id, request_id→chemical_requests, chemical_id→chemicals, quantity)
transactions(id, chemical_id→chemicals, user_id→users, type[ADD|RELEASE|ADJUSTMENT], quantity, transaction_date)
compatibility_rules(id, group_a, group_b)
```
Teacher 1──* chemical_requests 1──* request_items *──1 chemicals

## 3. Class relationships
```
model     User (abstract) ◄── Admin, Teacher        Chemical, ChemicalRequest 1──* RequestItem, Transaction
gui       LoginFrame → AdminDashboard / TeacherDashboard → ChemicalFrame, RequestFrame, MyRequestsFrame,
          RequestManagementFrame, InventoryFrame (+ ChemicalFormDialog, CompatibilityDialog)
service   AuthenticationService, ChemicalService, RequestService, InventoryService,
          NotificationService (interface) ◄── EmailService
dao       DatabaseConnection, UserDAO, ChemicalDAO, RequestDAO, TransactionDAO, CompatibilityDAO
```
Flow: **GUI → Service → DAO → MySQL**, and RequestService → EmailService → Gmail SMTP.

## 4. Where each OOP principle is shown
| Principle | Where | Explanation |
|---|---|---|
| **Encapsulation** | `Chemical`, `User`, `ChemicalRequest` | Fields are `private`. Quantity can only change through `addStock()` / `release()`, which reject invalid amounts (e.g. releasing more than available). Outside code cannot put the object in a bad state. |
| **Inheritance** | `Admin extends User`, `Teacher extends User` | id, name, username, password, email are written once in `User` and reused. |
| **Abstraction** | `abstract class User` (abstract `showDashboard()`, `getRole()`); `interface NotificationService` ← `EmailService` | Says *what* must be done, not *how*. `RequestService` only knows the `NotificationService` interface, so Gmail could be swapped without changing it. |
| **Polymorphism** | `LoginFrame.login()` → `user.showDashboard()`; `UserDAO.buildUser()` | `User user = authService.login(...)` is really an Admin or a Teacher. The same call opens a different dashboard for each. |

## 5. GUI screen flow
```
Login ─┬─ Admin Dashboard ─ Chemicals (add/edit/remove/search/rules) ─ Requests (approve/reject) ─ Inventory ─ Logout
       └─ Teacher Dashboard ─ New Request ─ My Requests ─ Chemicals (read-only) ─ Logout
```

## 6. Teacher request workflow
Teacher logs in with email → New Request (class, experiment, add several chemicals + quantities) → Submit → **PENDING**
→ Admin opens Requests → **Approve**: all chemicals released in ONE database transaction (all-or-nothing;
"Insufficient inventory for X." if short), status APPROVED, RELEASE transactions saved, email sent
→ **Reject**: reason required, status REJECTED, email sent. If the email fails, the admin sees an error.

## Setup
1. Install JDK 25 (the project's Java target), Maven, and MySQL.
2. Run `database/schema.sql` in MySQL (creates DB `hazard_tracker` + sample data).
   Edit the teacher's email in the INSERT (or later in the `users` table) to an address you can read.
3. Edit `config.properties`: MySQL user/password, and your Gmail address + **Gmail App Password**
   (Google Account → Security → 2-Step Verification → App passwords).
4. From this folder run: `mvn compile exec:java`   (run from the folder containing `config.properties`).

Logins (email / password): `admin@school.edu / admin123`, and Maria's email (set in the SQL) `/ teacher123`.

**Already ran the old schema?** Either re-run the new `schema.sql` (wipes data) or run `database/migration.sql` to keep your data.

## Demo scenario
Admin: Chemicals → Add (or use samples). Teacher (Maria): New Request → BSCS 1A, Acid-Base Titration,
HCl 200 mL + NaOH 200 mL → Submit. Admin: Requests → select → Approve → inventory drops to 800 mL,
Inventory screen shows RELEASE rows, teacher receives the Gmail message.
Other things to show: LOW STOCK (Ethanol), EXPIRED (Old Reagent X), EXPIRING SOON (Phenolphthalein),
storing an Acid chemical in Cabinet B (Base) triggers the incompatibility WARNING.

## Added in version 2
- **Email login** for everyone (no username).
- **Teacher: Edit / Cancel** a request from *My Requests* — only while it is PENDING (checked in the SQL, so an admin decision made a moment earlier cannot be overwritten). Cancelled requests get the status CANCELLED.
- **Admin: Change Status** on *Requests*: any request can be moved to PENDING / APPROVED / REJECTED / CANCELLED.
  Stock follows one rule: it is released exactly when a request is APPROVED. Moving *to* APPROVED releases stock (blocked if insufficient);
  moving *away from* APPROVED returns it (recorded as ADJUSTMENT transactions). Emails go out for APPROVED and REJECTED.
- **Admin: Add Chemical** button directly on the Admin Dashboard (also under Chemicals → Add).

## Added features
- **Login by email** (admin and teachers). Emails are unique.
- **Teacher: Edit / Cancel request** (My Requests screen). Only the teacher's own requests, only while PENDING. Cancelled requests show as CANCELLED.
- **Admin: Change Status** on any request (PENDING / APPROVED / REJECTED / CANCELLED), plus quick Approve / Reject buttons.
  - Becoming APPROVED releases stock (blocked with "Insufficient inventory" if short; all-or-nothing).
  - Leaving APPROVED returns the stock (recorded as ADJUSTMENT transactions).
  - The teacher is emailed when the new status is APPROVED or REJECTED.
- **Admin: Edit / Delete compatibility rules** (select a rule, change the groups, Update; or Delete with confirmation).
