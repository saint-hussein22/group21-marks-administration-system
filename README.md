# Group 21 — Marks Administration System

**Course:** SDN260S — Software Design II  
**Project Title:** Marks Administration System  
**Group Number:** 21

## Project Description

The **Marks Administration System** is a secure academic tool that allows lecturers to capture, import, and manage student marks. It automatically calculates totals and final grades, supports CSV upload/export, flags missing entries, and generates summary reports. The system implements role-based access control with two roles:

- **Lecturer** — Can enter/edit marks, import/export CSV, generate reports, and view flagged entries.
- **Head of Department (HoD)** — Full access including user management, viewing all marks across subjects, and overriding grades.

---

## Features

### Core Features
- **Problem Definition & Requirements** — Streamlines academic record-keeping for lecturers and administrators.
- **Student Management** — Add, edit, delete, and search students by ID or name.
- **Subject Management** — Define subjects with credit weights and assessment categories.
- **Marks Entry** — Enter marks per student per subject with validation.
- **Automatic Grade Calculation** — Calculate totals, weighted averages, and letter grades.
- **CSV Template & Import/Export** — Download a ready-to-fill marks template, bulk upload marks, and export data that can be edited and re-imported.
- **Missing Entry Flagging** — Highlight students with incomplete mark submissions.
- **Summary Reports** — Generate statistical summaries (class average, pass rate, etc.).
- **Role-Based Access Control** — Secure login with Lecturer and HoD roles.
- **GUI & CLI Interface** — Swing-based graphical interface and command-line menu.

### Advanced Features
- **Multithreading** — Background processing for CSV import/export.
- **SQLite Database** — One shared database for the CLI and GUI, with immediate persistence of students, subjects, marks, and users.
- **Recursion** — Directory traversal for batch CSV processing.
- **Generics & Collections** — Type-safe data structures.
- **Streams API** — Data filtering and aggregation.

---

## Prerequisites

- **Java 17 or higher**
- **Maven** (for dependency management and build)
- **NetBeans or VS Code** (as specified in project brief)

---

## Build & Run

```bash
# Compile and run the tests
mvn clean test

# Build the runnable JAR
mvn package

# Start the application (choose CLI or GUI)
java -jar target/marks-administration-system-1.0.0.jar
```

On first run the application creates `data/marks.db` and seeds it with CPUT Bellville
engineering programmes, modules and a class list with randomised 9-digit student numbers
(for example `253243782`). Delete `data/marks.db` to regenerate the demo roster.
All edits made in the CLI or GUI are written to that database immediately, so both
interfaces always read the same data.

Use a different database file (for example for a demo):

```bash
java -Dmarksystem.db=data/demo.db -jar target/marks-administration-system-1.0.0.jar
```

Default login: `lecturer1` / `pass123` (lecturer) or `hod1` / `admin123` (HoD).

---

## CSV Marks Template

Download the template from **CSV → Download CSV Template...** in the GUI, or
**CSV Import/Export → 3. Download Marks CSV Template** in the CLI. A copy is also
available at `templates/CPUT_Marks_Template.csv`.

The template contains one row per student with the class list already filled in:

```
StudentID,FirstName,LastName,Email,Program,Year,SubjectCode,SubjectName,
Assessment1,Assessment2,Assessment3,Assessment4,Assessment5
```

Fill in `Assessment1`, `Assessment2`, ... in the same order as the module's assessment
categories (shown in the Subjects tab) and import the file. The importer:

- matches columns by header name, so column order can change;
- creates new students and updates existing students from the file;
- replaces the marks for each student/module row;
- reports warnings for unknown subject codes or invalid marks.

The export function writes the same layout, so an exported file can be edited in Excel
and imported again.

---

## Project Structure

```
src/
├── main/
│   ├── java/com/group21/marksystem/
│   │   ├── models/        # Data model classes (Student, Subject, Mark, etc.)
│   │   ├── services/      # Business logic services (AuthenticationService)
│   │   ├── db/            # SQLite database layer (MarkRepository)
│   │   ├── utils/         # Utility classes (CSV, CPUT seed data, hashing)
│   │   ├── gui/           # Swing GUI
│   │   ├── cli/           # Command-line interface
│   │   └── App.java       # Main entry point
│   └── resources/         # Config files, sample data
└── test/
    └── java/com/group21/marksystem/  # Unit tests
```

---

## Team Members

| Name | Student ID | Role | Contribution |
|------|-----------|------|---:|
| Nhlonipho Sishi | | Database & Domain Architecture | 12.5% |
| Scelo Mthethwa | | Database & Domain Architecture | 12.5% |
| Lucas Mfulwane | | Security & Business Logic | 12.5% |
| Millano Redcliffe | | Security & Business Logic | 12.5% |
| Bopaki Montjane | | File Processing & CSV Engine | 12.5% |
| Phakamile Tshenyego | | File Processing & CSV Engine | 12.5% |
| Hussein Akim | | User Interface & Quality Assurance | 12.5% |
| Sinazo Radana | | User Interface & Quality Assurance | 12.5% |

Each member contributed an equal **12.5%** of the project. The full task split,
file-level evidence, milestone ownership and sign-off table are in
[`docs/WORK_DIVISION.md`](docs/WORK_DIVISION.md).

---

## Deliverables

1. ✅ Source Code (Java project)
2. ✅ ReadMe (this file)
3. ✅ System Design Document (PDF)
4. ✅ Graduate Attribute Reflection (in report)
5. ✅ Demo Presentation (Week 12)

---

## License

This project is developed for educational purposes at Cape Peninsula University of Technology (CPUT).
