# Hazard & Reagent Tracker (Java + MySQL + JDBC + Swing)

A small school-laboratory app. **Admin + Teacher → Chemicals → Requests → Approval → Inventory → Gmail.**

## 1. System overview
One desktop program on one computer. It starts at the login page. The role decides the screen:
ADMIN → Admin Dashboard, TEACHER → Teacher Dashboard.
Users log in with their **email + password**. The email field offers matching account addresses while remaining free-form.
Teachers request chemicals for a class; the admin approves (stock is released, transaction recorded, email sent),
or asks for a safety clarification when a request contains a fixed incompatible pair. Requests remain pending while clarified.

## 2. Database design (MySQL, `database/schema.sql`)
```
users(id, name, password[SHA-256], email[UNIQUE = the login], role)
chemicals(id, name, quantity, unit, hazard_type, storage_group, storage_temperature,
          location, minimum_stock, expiration_date, active)
chemical_requests(id, teacher_id→users, class_name, experiment, status[PENDING|APPROVED|REJECTED|CANCELLED], rejection_reason, request_date)
request_items(id, request_id→chemical_requests, chemical_id→chemicals, quantity)
transactions(id, chemical_id→chemicals, user_id→users, type[ADD|RELEASE|ADJUSTMENT], quantity, transaction_date)
```
Teacher 1──* chemical_requests 1──* request_items *──1 chemicals

`chemical_requests.status` retains `REJECTED` for historical records. The current application does not create rejected requests. Fixed storage incompatibility rules are maintained in application code.

## 3. Class relationships
```
model     User (abstract) ◄── Admin, Teacher        Chemical, ChemicalRequest 1──* RequestItem, Transaction
gui       LoginFrame → AdminDashboard / TeacherDashboard → ChemicalFrame, RequestFrame, MyRequestsFrame,
          RequestManagementFrame, InventoryFrame (+ ChemicalFormDialog, CompatibilityDialog)
service   AuthenticationService, ChemicalService, RequestService, InventoryService,
          NotificationService (interface) ◄── EmailService
dao       DatabaseConnection, UserDAO, ChemicalDAO, RequestDAO, TransactionDAO
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
Login ─┬─ Admin Dashboard ─ Add Teacher / Chemicals (add/edit/remove/search) ─ Requests (approve/clarify) ─ Inventory ─ Logout
       └─ Teacher Dashboard ─ New Request ─ My Requests ─ Chemicals (read-only) ─ Logout
```

## 6. Teacher request workflow
Teacher logs in with email → New Request (class, experiment, add several chemicals + quantities) → Submit → **PENDING**
→ Admin opens Requests → **Approve**: all chemicals released in ONE database transaction (all-or-nothing;
"Insufficient inventory for X." if short), status APPROVED, RELEASE transactions saved, email sent
→ **Safety clarification**: if incompatible storage classes were requested together, the admin can email the teacher for the reason and handling plan; the request remains PENDING. The application no longer offers a reject action. Historical REJECTED rows remain readable.

## Setup in VS Code

Follow these steps on each computer that will run the tracker. The database and `config.properties` are local to each computer unless your group separately configures a shared MySQL server.

### 1. Install the required software

- **Visual Studio Code:** [Download VS Code](https://code.visualstudio.com/Download).
- **JDK 25:** The project compiles for Java 25 (`pom.xml` sets `maven.compiler.release` to 25). Install a JDK 25 distribution, such as [Oracle JDK 25](https://www.oracle.com/java/technologies/downloads/#java25). After installation, open a new terminal and check `java -version`.
- **Java support for VS Code:** In VS Code, open Extensions and install **Extension Pack for Java**. It includes Java language support, debugging, Maven integration, and Java project tools. See the [official VS Code Java guide](https://code.visualstudio.com/docs/java/java-tutorial).
- **Apache Maven 3.9 or later:** Install Maven and make sure `mvn -version` works in a new terminal. Use the [official Maven installation guide](https://maven.apache.org/install.html).
- **MySQL Community Server:** Install and start a local MySQL server. [MySQL Community downloads](https://dev.mysql.com/downloads/) provide installers and packages for supported operating systems.
- **MySQL Workbench (recommended):** This GUI makes it easy to run the SQL files. Download [MySQL Workbench](https://dev.mysql.com/downloads/workbench/) or choose it during Windows MySQL Installer setup. Workbench is optional if you use the `mysql` command-line client.
- **Google account with 2-Step Verification (only if sending email):** Each person who configures Gmail sending needs their own account and App Password. Do not share an App Password with the group.

The MySQL Connector/J and Jakarta Mail libraries are already declared in `pom.xml`; Maven downloads them during the first build. Do not install them separately.

### 2. Open the project

Clone the group's repository, or open the project folder in VS Code with **File → Open Folder**. In VS Code's integrated terminal, change to the folder containing `pom.xml`, `database/`, and `config.properties.example`.

### 3. Create the local database

Start the MySQL Server service, then connect to the local server as a MySQL user with permission to create databases and tables.

**Using MySQL Workbench:**

1. Connect to the local MySQL server.
2. Open `database/schema.sql` and execute the full script.
3. Open `database/seed.sql` and execute the full script once.

**Using a terminal with the MySQL command-line client:**

```sh
mysql -u root -p < database/schema.sql
mysql -u root -p hazard_tracker < database/seed.sql
```

Enter the MySQL password when prompted. The schema creates the `hazard_tracker` database and tables. The seed adds the two default accounts and 25 chemical inventory records. It uses `INSERT IGNORE` for the default account emails, so matching existing admin/teacher accounts are kept. Run the chemical seed once per database; it is not an inventory refresh script and running it repeatedly creates duplicate chemical rows.

The default accounts are:

- Admin: `admin@school.edu` / `admin123`
- Teacher: `mcona_250000000763@uic.edu.ph` / `teacher123`

You can change the teacher email in `database/seed.sql` before seeding so approval and clarification messages reach the intended mailbox. You can also add teachers through the Admin Dashboard after setup.

If you already have a database, do not rerun the chemical seed unless you intend to add another copy of those records. `CREATE TABLE IF NOT EXISTS` does not alter existing table definitions; this repository does not include a migration for existing databases.

### 4. Create your private `config.properties`

Copy `config.properties.example` to a new file named `config.properties` in the project root. The application loads this file from its working directory. In VS Code's terminal:

```sh
cp config.properties.example config.properties
```

On Windows PowerShell, use:

```powershell
Copy-Item config.properties.example config.properties
```

Open your local `config.properties` and set these values:

| Key | Value |
|---|---|
| `db.url` | Keep `jdbc:mysql://localhost:3306/hazard_tracker` for a local MySQL server. Change the host, port, or database name only if your MySQL setup differs. |
| `db.user` | The MySQL account used by the application, such as `root` for a local class-project setup or a dedicated MySQL user. |
| `db.password` | That MySQL account's password. |
| `gmail.address` | The Gmail account that will send tracker notifications. |
| `gmail.app.password` | The App Password generated for that Gmail account; this is not your normal Google password. |

Keep the `key=value` format. Do not add quotes around values. The `.gitignore` excludes `config.properties`; commit changes to `config.properties.example` only, never your local credentials.

### 5. Generate a Google App Password (email setup)

The tracker sends approval and safety-clarification email through Gmail SMTP. Google requires 2-Step Verification before an App Password can be created.

1. Sign in to the Google account that you will use as `gmail.address`.
2. Turn on **2-Step Verification** in the account's Security settings if it is not already enabled.
3. Open Google's [App Passwords page](https://myaccount.google.com/apppasswords). Google may ask you to sign in again.
4. Create an App Password with a label such as `Hazard Reagent Tracker` and copy the generated 16-character password.
5. Paste it into `gmail.app.password`. Remove spaces if Google displays the password in groups. Put the same account address in `gmail.address`.

Google may hide the App Password option for some account types or security configurations. If it is unavailable, ask the account administrator or use a Google account that supports App Passwords. If you change the Google account password later, generate a new App Password. See Google's [official App Password instructions](https://support.google.com/accounts/answer/185833).

The app can still start without Gmail credentials, but its email-send actions will report a configuration error. Keep App Passwords private, out of source code, screenshots, chat, and commits. If a credential was already committed or shared, revoke it in Google Account settings and generate a replacement.

### 6. Build and run

In VS Code's integrated terminal at the project root:

```sh
mvn compile exec:java
```

The first run may take longer while Maven downloads dependencies. The command must be run from the folder containing `config.properties`, because the application reads that file from the current working directory. The Login window opens after the app starts.

### Quick setup prompt for VS Code Agent

Paste this into VS Code Copilot Chat in **Agent** mode after opening the project folder. It guides setup without putting credentials into chat or rerunning the chemical seed on an existing database.

```text
Set up this Hazard & Reagent Tracker workspace for local use. Read README.md, pom.xml, database/schema.sql, database/seed.sql, and config.properties.example first.

1. Check that Java 25, Maven, and the MySQL command-line client are available. If any are missing, give me the official installation link and exact next step for my operating system; do not install system software or use elevated commands without asking.
2. Check that the MySQL server is running and whether the hazard_tracker database already contains tables or chemical rows. Do not drop, reset, overwrite, or delete any database, teacher, admin, or inventory records.
3. If config.properties does not exist, copy config.properties.example to config.properties. Never print, request, or commit passwords or App Passwords. Tell me to fill in the local file privately and pause before starting the app if any required values are still placeholders.
4. If hazard_tracker does not exist, run database/schema.sql and database/seed.sql once. If it already exists or contains chemical rows, do not run seed.sql; tell me it is a one-time insert and explain that CREATE TABLE IF NOT EXISTS does not migrate existing tables.
5. Run `mvn compile`. If it succeeds and config.properties is complete, start the app with `mvn exec:java` from the project root. Report the result and any remaining manual step. Do not change Java source, SQL, or credentials as part of setup.
```

## Storage safety rules
New chemicals use one of these storage locations: Flammable, Acid Corrosive, Base Corrosive, Oxidizer, Others, or None. Hazard types display with comma spacing, and chemical search accepts generated codes such as `C018` as well as names and hazards. Active inventory chemical names must be unique, ignoring case.

The fixed rules are Acid–Base, Acid–Flammable, Base–Flammable, Oxidizer–Flammable, Acid–Cyanide, and Oxidizer–Organic Material. Mark Cyanide and Organic Material in the hazard field where the Safety Data Sheet supports those classifications. The rule viewer is read-only. DOLE Department Order No. 136-14, Section 6(4), requires proper segregation according to GHS physical-hazard criteria and each Safety Data Sheet's storage requirements. The listed pairs are the lab's application policy, not a claim that the order enumerates those exact pairings. See the [official text](https://elibrary.judiciary.gov.ph/thebookshelf/showdocs/10/68235).

## Dashboard analytics
Dashboard metrics use active chemical records and the application's request statuses. “Approved This Month” is based on request submission date because the database does not store an approval timestamp. “Most requested” ranks chemicals by the number of requests containing them; quantities are not added across different units. The teacher dashboard shows chemical availability by stock/expiration status rather than ranking unlike units by raw quantity. Stock movement uses recorded ADD and RELEASE transactions only.

## Demo scenario
Admin: review the seeded inventory or add chemicals. Teacher (Maria): New Request → BSCS 1A, Acid-Base Titration,
HCl 200 mL + NaOH 200 mL → Submit. Admin: Requests → select → Approve → inventory drops to 800 mL,
Inventory screen shows RELEASE rows, teacher receives the Gmail message.
Other things to show: LOW STOCK (Ethanol), EXPIRED (Old Reagent X), EXPIRING SOON (Phenolphthalein),
the storage safety rules dialog lists the fixed segregation pairs.

## Added in version 2
- **Email login** for everyone (no username).
- **Teacher: Edit / Cancel** a request from *My Requests* — only while it is PENDING (checked in the SQL, so an admin decision made a moment earlier cannot be overwritten). Cancelled requests get the status CANCELLED.
- **Admin: Change Status** on *Requests*: active requests can be moved to PENDING / APPROVED / CANCELLED; teacher-cancelled requests are final.
  Stock follows one rule: it is released exactly when a request is APPROVED. Moving *to* APPROVED releases stock (blocked if insufficient);
  moving *away from* APPROVED returns it (recorded as ADJUSTMENT transactions). Approval emails go to teachers.
- **Admin: Add Chemical** button directly on the Admin Dashboard (also under Chemicals → Add).
- **Admin: Add Teacher** from the Admin Dashboard, creating a teacher login without editing the database.

## Added features
- **Login by email** (admin and teachers). Emails are unique.
- **Teacher: Edit / Cancel request** (My Requests screen). Only the teacher's own requests, only while PENDING. Cancelled requests show as CANCELLED.
- **Admin: Change Status** on non-cancelled requests (PENDING / APPROVED / CANCELLED), plus Approve and safety clarification actions.
  - Becoming APPROVED releases stock (blocked with "Insufficient inventory" if short; all-or-nothing).
  - Leaving APPROVED returns the stock (recorded as ADJUSTMENT transactions).
  - The teacher is emailed when a request is APPROVED. Incompatible chemical pairs can trigger a clarification email without changing status.
- **Storage safety rules** are fixed in application code and cannot be edited from the interface.
