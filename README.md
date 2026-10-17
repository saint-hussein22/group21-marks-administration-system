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
- **CSV Import/Export** — Bulk upload/download mark data.
- **Missing Entry Flagging** — Highlight students with incomplete mark submissions.
- **Summary Reports** — Generate statistical summaries (class average, pass rate, etc.).
- **Role-Based Access Control** — Secure login with Lecturer and HoD roles.
- **GUI & CLI Interface** — Swing-based graphical interface and command-line menu.

### Advanced Features
- **Multithreading** — Background processing for CSV import/export.
- **Object Serialization** — Persist user session and mark data.
- **SQLite Database Persistence** — Embedded relational storage for users, students, subjects, and marks.
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
# Compile the project
mvn clean compile

# Run the CLI interface
mvn exec:java -Dexec.mainClass="com.group21.marksystem.cli.CLIMain"

# Run the GUI interface
mvn exec:java -Dexec.mainClass="com.group21.marksystem.gui.GUIMain"
```

On first run, the app creates `/home/runner/work/group21-marks-administration-system/group21-marks-administration-system/data/marks_system.db` automatically and persists data there on exit.

---

## Project Structure

```
src/
├── main/
│   ├── java/com/group21/marksystem/
│   │   ├── models/        # Data model classes (Student, Subject, Mark, etc.)
│   │   ├── services/      # Business logic services
│   │   ├── utils/         # Utility classes (CSV, hashing, grading)
│   │   ├── gui/           # Swing GUI
│   │   ├── cli/           # Command-line interface
│   │   └── App.java       # Main entry point
│   └── resources/         # Config files, sample data
└── test/
    └── java/com/group21/marksystem/  # Unit tests
```

---

## Team Members

| Name | Student ID | Role |
|------|-----------|------|
| CHAGEDI BOPAKI MONTJANE | N/A | Student |
| HUSSEIN AKIM | N/A | Student |
| LUCAS MFULWANE | N/A | Student |
| MILLANO ZAVIER REDCLIFFE | N/A | Student |
| NHLONIPHO NZUZO SISHI | N/A | Student |
| PHAKAMILE TSHENYEGO | N/A | Student |
| SIBONGAKONKE MTHETHWA | N/A | Student |
| SINAZO BEATRICE RADANA | N/A | Student |

## Team Roles & Responsibilities

| Job Category | Assigned Members | Primary Code & Component Responsibilities |
|--------------|------------------|-------------------------------------------|
| 1. Database & Domain Architecture | Nhlonipho Sishi, Scelo Mthethwa | Database setup (`schema.sql`, `db/Database.java`) and core domain models (`User`, `Role`, `Student`, `Module`, `Assessment`). |
| 2. Security & Business Logic | Lucas Mfulwane, Millano Redcliffe | Security and authentication (`util/PasswordUtil.java`, `service/AuthService.java`) alongside mark processing, missing mark flags, and weight validations (`service/MarksService.java`). |
| 3. File Processing & CSV Engine | Bopaki Montjane, Phakamile Tshenyego | File parsing, batch CSV mark imports, error logging, and export functionality (`service/CsvService.java`, `service/ImportResult.java`). |
| 4. User Interface & Quality Assurance | Hussein Akim, Sinazo Radana | Graphical UI (`gui/GuiApp.java`), terminal CLI testing interface (`CliApp.java`, `Main.java`), test cases, and final documentation. |

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
