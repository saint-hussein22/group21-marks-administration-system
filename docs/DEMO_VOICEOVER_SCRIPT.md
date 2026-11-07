# Group 21 — Week 12 Demo Voiceover Script (~9:30)

**Note:** Record narration with your own voices (or Nhlonipho’s, with his consent). Do not use a cloned voice of a teammate without permission.

Suggested logins (Maven / SQLite build): `lecturer1` / `pass123`, `hod1` / `admin123`  
Nhlonipho NetBeans / MySQL build: `admin` / `admin123`, `lect` / `lect123`

---

## 1. Member 1 — Introduction and architecture (0:00–0:45)

> Good day. We are Group 21, and this is our Marks Administration System for SDN260S.  
> Lecturers capture and manage student marks; the Head of Department oversees modules and reports.  
> The system has a layered design: models, database, services, then CLI and Swing GUI.  
> Both interfaces share one database, so a mark entered in the console is available after restart.

**On screen:** title slide or app banner; brief package diagram / folder tree.

---

## 2. Member 2 — Database and starting the app (0:45–1:45)

> Persistence uses JDBC. Our submission build uses SQLite in `data/marks.db`, created on first run with seeded CPUT modules and students.  
> The teammate NetBeans build uses MySQL via XAMPP and `schema.sql` with tables for users, modules, students, assessments, marks, and an audit log.  
> We start with `mvn package` and run the jar, or Run Project in NetBeans. Choose GUI or CLI at the prompt.

**On screen:** schema / tables (sqlite3 or phpMyAdmin); app start → mode menu.

---

## 3. Member 3 — Accounts, login and security (1:45–2:45)

> Access is role-based: Lecturer and HoD. Passwords are hashed — PBKDF2 in the Maven build, salted SHA-256 in the MySQL build.  
> Logging in as a lecturer shows student, subject, marks, reports and CSV tools. HoD features stay hidden.  
> Logging in as HoD unlocks user or module management, depending on the build.

**On screen:** login dialog or CLI login; wrong password once; successful lecturer login.

---

## 4. Member 4 — Lecturer dashboard and marks table (GUI) (2:45–4:00)

> After login, the lecturer sees the dashboard with class overview, then opens the Marks tab.  
> Selecting a student and module loads existing assessments. Weighted total and letter grade update from the subject weights.

**On screen:** GUI dashboard → Marks → select student `253243782` / `CIV260S` (or SDN260S on MySQL build).

---

## 5. Member 5 — Adding students, assessments and manual marks (GUI) (4:00–5:15)

> Lecturers can add or edit students and subjects. Assessment weights must total one hundred percent.  
> We enter marks for Test 1, Test 2 and Exam — for example eighty-two, seventy-one and ninety.  
> The system calculates eighty-three point two five and grade B-plus.

**On screen:** Students / Subjects dialogs; mark entry; success message with total and grade.

---

## 6. Member 6 — CSV import, export and missing entries (GUI) (5:15–6:30)

> From the CSV panel we download a template, export marks, and import a filled file. Import can also scan a folder recursively.  
> The missing-marks report flags students with incomplete assessments so nothing is silently left blank.

**On screen:** CSV export path; import summary; Missing Marks report.

---

## 7. Member 7 — HoD dashboard, modules and reports (6:30–7:45)

> As Head of Department we see all modules, assign lecturers, and open summary and missing-entry reports across the faculty.  
> Module averages and missing counts appear in one place. User management or module management is HoD-only.

**On screen:** HoD GUI dashboard / User Management (Maven) or HoD module summary (NetBeans); report text.

---

## 8. Member 8 — Command-line mode (7:45–8:45)

> The same services drive the CLI. We choose option one or two at startup, log in, open a subject, enter marks, run reports, and export CSV.  
> After restart, marks remain in the database — proof of persistence without the GUI.

**On screen:** CLI menus; mark entry; subject report; optional restart check.  
*(Existing footage: `Screen Recording 2026-11-06 231730.MP4` covers NetBeans HoD CLI.)*

---

## 1. Member 1 — Testing, audit / conclusion (8:45–9:30)

> We verified the build with thirty-one automated JUnit tests covering grades, passwords, persistence, CSV and snapshots.  
> The MySQL build also writes an audit log of important actions.  
> In short: secure login, shared storage, weighted processing, CSV and reports, CLI and GUI. Thank you.

**On screen:** `mvn test` summary; optional audit_log rows; closing title.

---

## Recording checklist

| # | Segment | Capture source |
|---|---------|----------------|
| 1 | Intro | Title card + architecture slide |
| 2 | Database | sqlite3 `.tables` / schema.sql + app start |
| 3 | Login | GUI LoginDialog or CLI auth |
| 4–6 | Lecturer GUI | Maven Swing or NetBeans GuiApp |
| 7 | HoD | Maven Users tab **or** NetBeans HoD CLI recording |
| 8 | CLI | Maven CLI **or** existing NetBeans HoD CLI clip |
| 9 | Close | Test output + end card |

Merged working file so far: `docs/Group21_CLI_and_HoD_Demo.mp4` (lecturer SQLite CLI + NetBeans HoD CLI only — not the full 9:30 plan).
