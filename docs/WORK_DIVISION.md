# Group 21 — Work Division and Contribution Record

**Course:** SDN260S — Software Design II  
**Project:** Group 21 Marks Administration System  
**Institution:** CPUT  
**Group:** 21  
**Source document:** `SOFTWARE DESIGN.pdf` (2 October 2026), Section 3 — Team Roles & Responsibilities; Section 4 — Milestones & Project Timeline

---

## 1. How the work was divided

The four role categories from the design document are kept exactly as specified. Each
category is owned by the two assigned members and split into two non-overlapping work
packages, so every member carries an equal share of the project:

| Role pair | Members | Pair share |
|---|---|---:|
| Database & Domain Architecture | Nhlonipho Sishi, Scelo Mthethwa | 25% |
| Security & Business Logic | Lucas Mfulwane, Millano Redcliffe | 25% |
| File Processing & CSV Engine | Bopaki Montjane, Phakamile Tshenyego | 25% |
| User Interface & Quality Assurance | Hussein Akim, Sinazo Radana | 25% |
| **Total** | **8 members** | **100%** |

**Individual contribution: 12.5% per member (1/8 of the project).**

---

## 2. Contribution summary

| Member | Role | Individual contribution | Main code evidence |
|---|---|---:|---|
| Nhlonipho Sishi | Database & Domain Architecture | 12.5% | `src/main/java/com/group21/marksystem/db/Database.java`, `pom.xml` |
| Scelo Mthethwa | Database & Domain Architecture | 12.5% | `models/Student.java`, `models/Subject.java`, `models/Mark.java`, `models/User.java`, `utils/CPUTData.java` |
| Lucas Mfulwane | Security & Business Logic | 12.5% | `utils/PasswordUtils.java`, `services/UserService.java` |
| Millano Redcliffe | Security & Business Logic | 12.5% | `services/MarkService.java`, `services/ReportService.java` |
| Bopaki Montjane | File Processing & CSV Engine | 12.5% | `utils/CSVHelper.java`, `templates/CPUT_Marks_Template.csv` |
| Phakamile Tshenyego | File Processing & CSV Engine | 12.5% | `services/CSVService.java` |
| Hussein Akim | User Interface & Quality Assurance | 12.5% | `gui/GUIMain.java`, `gui/LoginDialog.java`, `gui/DashboardPanel.java`, `gui/StudentPanel.java`, `gui/StudentDialog.java`, `gui/SubjectPanel.java`, `gui/SubjectDialog.java`, `gui/MarkPanel.java`, `gui/ReportPanel.java`, `gui/CSVPanels.java`, `gui/UserPanel.java`, `gui/UserDialog.java` |
| Sinazo Radana | User Interface & Quality Assurance | 12.5% | `cli/CLIMain.java`, `App.java`, `src/test/java/com/group21/marksystem/services/DatabasePersistenceTest.java`, `src/test/java/com/group21/marksystem/services/CSVServiceTest.java`, `README.md`, `docs/WORK_DIVISION.md` |

All percentages are equal and add up to **100%**.

---

## 3. Detailed responsibilities

> File paths in this section are relative to
> `src/main/java/com/group21/marksystem/` unless shown in full. Test and template
> artifacts are shown relative to the repository root.

### 3.1 Database & Domain Architecture — 25%

#### Nhlonipho Sishi — Database schema, JDBC layer and persistence (12.5%)

- Designed the five-table relational schema: `students`, `subjects`,
  `subject_assessments`, `marks` and `users`.
- Implemented `db/Database.java`: SQLite JDBC connection management, foreign-key
  enforcement, schema creation on first run and a configurable database path
  (`-Dmarksystem.db`).
- Implemented transactional insert/update/delete operations for students, subjects,
  users and marks, including cascade cleanup when a subject or student is deleted.
- Implemented loaders that rebuild the in-memory domain objects from the database,
  including assessment weights, entered-by metadata and timestamps.
- Added the `org.xerial:sqlite-jdbc` dependency and database error handling
  (`Database.DatabaseException`).
- Ensured the CLI and GUI use the same database so edits made in one interface are
  visible in the other.

#### Scelo Mthethwa — Domain model and CPUT seed data (12.5%)

- Implemented the domain classes `Student`, `Subject`, `Mark` and `User`, including
  their relationships and collections.
- Implemented the `Mark.Grade` enum and grade-threshold mapping, weighted-total
  calculation, missing-mark flag, `equals`/`hashCode`/`toString` helpers.
- Modelled assessment categories and weights in `Subject`, and student marks in
  `Student`.
- Created `utils/CPUTData.java`: seven CPUT Bellville engineering/built-environment
  programmes, 31 modules with credits and assessment weights, and a 23-student class
  list with randomised nine-digit student numbers (anchored at `253243782`).
- Provided the deterministic seed marks used when an empty database is first created.
- Reviewed the model-to-database field mapping with the database owner.

### 3.2 Security & Business Logic — 25%

#### Lucas Mfulwane — Authentication, password security and RBAC (12.5%)

- Implemented `utils/PasswordUtils.java`: salted SHA-256 password hashing, Base64
  storage and constant-time verification.
- Implemented `services/UserService.java`: user registration, login/authentication,
  current-user tracking, user updates/deletes and `hasRole` authorization checks.
- Implemented the `LECTURER` / `HOD` role model and the HoD override rule in `User`.
- Added database-backed user persistence and default-account seeding that only runs
  when no users exist.
- Wired the HoD-only User Management screen into the GUI and the HoD-only menu option
  into the CLI.
- Handled invalid-login feedback and password re-entry in the login flow.

#### Millano Redcliffe — Marks engine, calculations and reports (12.5%)

- Implemented `services/MarkService.java`: student and subject management, mark entry,
  weighted-total calculation from subject weights, GPA calculation and missing-mark
  detection.
- Implemented subject update/recalculation rules so existing marks are recalculated
  when assessment weights change.
- Implemented `services/ReportService.java`: student reports, subject reports with
  class average/highest/lowest statistics, missing-marks report, activity log and
  CSV export delegation.
- Implemented delete/reload/seed behaviour and thread-safe access used by both
  interfaces.
- Defined the mark status rules (`OK` / `INCOMPLETE`) consumed by the GUI and reports.

### 3.3 File Processing & CSV Engine — 25%

#### Bopaki Montjane — CSV parsing, writing and template generation (12.5%)

- Implemented `utils/CSVHelper.java`: OpenCSV reading/writing, proper escaping and
  quoting, blank-row handling and parent-directory creation.
- Implemented the canonical marks layout:
  `StudentID, FirstName, LastName, Email, Program, Year, SubjectCode, SubjectName,
  Assessment1..N`.
- Implemented `generateTemplate(...)` for the lecturer roster with suggested modules,
  and `writeMarksCSV(...)` for exports that can be edited and re-imported.
- Implemented the recursive CSV directory scanner and the template/sample generator.
- Produced and maintained `templates/CPUT_Marks_Template.csv`.
- Documented the template columns and workflow in `README.md`.

#### Phakamile Tshenyego — Import/export engine and validation (12.5%)

- Implemented `services/CSVService.java`: header-name mapping, student creation and
  update, subject lookup by code or name, and assessment-column mapping.
- Implemented row-level validation and warning collection for unknown modules,
  invalid years, out-of-range marks, non-numeric marks and missing student names.
- Implemented `ImportSummary` (rows processed, students created/updated, marks saved,
  warnings) and the async import/export wrappers.
- Implemented mark replacement per student/module and the template-download service.
- Ensured exported files use the same layout as the template, enabling an
  export → edit → re-import round trip.
- Integrated the import summary with the CLI and GUI success dialogs.

### 3.4 User Interface & Quality Assurance — 25%

#### Hussein Akim — Swing graphical interface (12.5%)

- Implemented `gui/GUIMain.java`: tabbed application shell, login gate, role-aware
  tabs, status bar and shared refresh callback.
- Implemented `LoginDialog`, `DashboardPanel`, `StudentPanel`, `StudentDialog`,
  `SubjectPanel`, `SubjectDialog`, `MarkPanel`, `ReportPanel`, `CSVPanels`,
  `UserPanel` and `UserDialog`.
- Implemented mark entry and saving, 0–100 input validation, weighted-total/grade
  feedback, empty-field → missing-mark behaviour and the marks table refresh.
- Implemented subject edit/delete and student refresh so edits update immediately.
- Added CSV template download, CSV import summary and the refresh-all behaviour after
  an import.
- Added HoD-only user management and the database path indicator.

#### Sinazo Radana — CLI, automated tests, QA and documentation (12.5%)

- Implemented `cli/CLIMain.java`: authentication, main menu and submenus for students,
  subjects, marks, reports, CSV import/export/template and HoD user management.
- Implemented `App.java`: CLI/GUI selection, shared `Scanner` and default-mode
  fallback.
- Implemented the JUnit 5 suite in `src/test/java/...`:
  - `DatabasePersistenceTest` — 6 tests for seeding, reload persistence, student/
    subject/mark edits, deletion and user authentication.
  - `CSVServiceTest` — 4 tests for template contents, import create/update/warnings,
    export → re-import round trip and invalid headers.
- Performed integration testing of the CLI and GUI sharing one database.
- Maintained `README.md`, this work-division record and the final submission
  documentation; prepared the demo/presentation checklist.

---

## 4. Milestone ownership (from the design document timeline)

| Week | Milestone | Lead | Support | Deliverable / evidence |
|---|---|---|---|---|
| 1 | System architecture & model classes | Scelo Mthethwa | Nhlonipho Sishi | `models/*.java`, `utils/CPUTData.java` |
| 1 | Database schema & database handler | Nhlonipho Sishi | Scelo Mthethwa | `db/Database.java`, schema created on first run |
| 2 | Authentication engine | Lucas Mfulwane | Millano Redcliffe | `PasswordUtils.java`, `UserService.java`, `LoginDialog.java` |
| 2 | Marks engine & weight handling | Millano Redcliffe | Lucas Mfulwane | `MarkService.java`, `ReportService.java`, `Mark.java` |
| 2 | CSV parsing, imports & exports | Bopaki Montjane | Phakamile Tshenyego | `CSVHelper.java`, `templates/CPUT_Marks_Template.csv` |
| 2 | CSV service, validation & import report | Phakamile Tshenyego | Bopaki Montjane | `CSVService.java`, `ImportSummary` |
| 2 | Fallback CLI & unit tests | Sinazo Radana | Hussein Akim | `CLIMain.java`, `DatabasePersistenceTest`, `CSVServiceTest` |
| 3 | Swing graphical interface | Hussein Akim | Sinazo Radana | `gui/GUIMain.java` and all `gui/*Panel` / `*Dialog` classes |
| 3 | Design report, GA reflections, presentation | Sinazo Radana | All members | `README.md`, `docs/WORK_DIVISION.md`, report and slides |

Each member leads at least one milestone and supports at least one other, so the
planning, implementation, testing and documentation work is shared equally.

---

## 5. Individual contribution statements

**Nhlonipho Sishi (12.5%)** — I designed and implemented the database layer. This
includes the relational schema, the JDBC connection and transaction handling, the
persistence/loading operations for all entities, and the shared database used by both
interfaces. I also added the SQLite JDBC dependency and the configurable database path.

**Scelo Mthethwa (12.5%)** — I implemented the domain model (`Student`, `Subject`,
`Mark`, `User`) and the grading/weighted-total rules. I created the CPUT Bellville seed
catalogue covering seven programmes and 31 modules, plus the randomised class list and
sample marks used for the demonstration.

**Lucas Mfulwane (12.5%)** — I implemented password hashing and verification, the
authentication service, user CRUD, role-based access control and the HoD-only user
management gate. I also handled default-account seeding and login error handling.

**Millano Redcliffe (12.5%)** — I implemented the marks engine: mark entry, weighted
totals, GPA, missing-mark detection, subject-weight recalculation and the report
service (student/subject reports, class statistics, missing marks and activity log).

**Bopaki Montjane (12.5%)** — I implemented CSV reading/writing, quoting/escaping and
the canonical marks template. I generated `CPUT_Marks_Template.csv` and implemented
the export layout so exported files can be edited and imported again.

**Phakamile Tshenyego (12.5%)** — I implemented the CSV import/export service:
header-name mapping, student create/update, subject resolution, row validation,
warning collection, the import summary and asynchronous processing.

**Hussein Akim (12.5%)** — I implemented the Swing GUI: application shell, login,
dashboard, student/subject/marks/report/CSV/user panels and dialogs, mark saving and
validation, CSV template download, import reporting and refresh behaviour.

**Sinazo Radana (12.5%)** — I implemented the CLI, the application launcher and the
JUnit test suite. I performed integration testing across CLI and GUI, maintained the
README and work-division documentation, and prepared the final submission and demo.

---

## 6. Implementation naming notes

The design document uses planning names; the final implementation uses the following
equivalent names. This mapping is provided so markers can trace each responsibility to
the submitted code.

| Design document | Final implementation |
|---|---|
| `schema.sql`, `db/Database.java` | `db/Database.java` (schema DDL embedded; SQLite JDBC) |
| `User`, `Role`, `Student`, `Module`, `Assessment` | `User` (with nested `Role`), `Student`, `Subject` (holds assessment weights), `Mark` |
| `util/PasswordUtil.java` | `utils/PasswordUtils.java` |
| `service/AuthService.java` | `services/UserService.java` |
| `service/MarksService.java` | `services/MarkService.java` |
| `service/CsvService.java`, `service/ImportResult.java` | `services/CSVService.java` with nested `ImportSummary` |
| `gui/GuiApp.java` | `gui/GUIMain.java` plus the `*Panel` and `*Dialog` classes |
| `CliApp.java`, `Main.java` | `cli/CLIMain.java`, `App.java` |

**Technology note:** the final demonstration build uses the embedded SQLite JDBC
driver so the marker can run it without installing a database server. The design is
still a JDBC-backed persistence layer with the same entities and responsibilities
described in the design document.

---

## 7. Declaration and sign-off

We confirm that the work above was divided as recorded and that each member contributed
an equal 12.5% share of the project.

| Member | Contribution | Signature | Date |
|---|---:|---|---|
| Nhlonipho Sishi | 12.5% | | |
| Scelo Mthethwa | 12.5% | | |
| Lucas Mfulwane | 12.5% | | |
| Millano Redcliffe | 12.5% | | |
| Bopaki Montjane | 12.5% | | |
| Phakamile Tshenyego | 12.5% | | |
| Hussein Akim | 12.5% | | |
| Sinazo Radana | 12.5% | | |
| **Total** | **100%** | | |
