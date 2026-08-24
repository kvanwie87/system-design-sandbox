# Normalization Walkthrough: From Unnormalized to BCNF

This document walks through a complete normalization example, starting with raw unnormalized data and progressing step by step through each normal form.

---

## The Scenario

A small university tracks student enrollments. An administrator has been keeping everything in a single spreadsheet-style table.

---

## Step 0: Raw, Unnormalized Data

| StudentID | StudentName | Major | AdvisorID | AdvisorName | AdvisorOffice | Courses | Instructors | Grades |
|-----------|-------------|-------|-----------|-------------|---------------|---------|-------------|--------|
| S1 | Alice | CS | A1 | Dr. Smith | Room 201 | CS101, CS202 | Dr. Lee, Dr. Patel | A, B |
| S2 | Bob | Math | A2 | Dr. Jones | Room 305 | MATH101, CS101 | Dr. Adams, Dr. Lee | B, A |
| S3 | Carol | CS | A1 | Dr. Smith | Room 201 | CS101, CS202, MATH101 | Dr. Lee, Dr. Patel, Dr. Adams | A, A, B |

### Problems with this table

- **Repeating groups:** The `Courses`, `Instructors`, and `Grades` columns contain comma-separated lists.
- **Redundancy:** Advisor information is repeated for every CS student.
- **All three anomalies** are possible (update, insertion, deletion).

---

## Step 1: Identify Entities and Keys

Before decomposing, identify the distinct entities and their natural keys:

| Entity | Key | Attributes |
|--------|-----|------------|
| Student | StudentID | StudentName, Major |
| Advisor | AdvisorID | AdvisorName, AdvisorOffice |
| Course | CourseID | (course name, credits, etc.) |
| Enrollment | (StudentID, CourseID) | Grade, InstructorID |
| Instructor | InstructorID | InstructorName |

---

## Step 2: Remove Repeating Groups → 1NF

**Rule:** Every column must hold a single atomic value. No lists, no repeating groups. Each row must be uniquely identifiable.

We "flatten" the repeating groups by creating one row per student-course combination:

| StudentID | StudentName | Major | AdvisorID | AdvisorName | AdvisorOffice | CourseID | InstructorName | Grade |
|-----------|-------------|-------|-----------|-------------|---------------|----------|----------------|-------|
| S1 | Alice | CS | A1 | Dr. Smith | Room 201 | CS101 | Dr. Lee | A |
| S1 | Alice | CS | A1 | Dr. Smith | Room 201 | CS202 | Dr. Patel | B |
| S2 | Bob | Math | A2 | Dr. Jones | Room 305 | MATH101 | Dr. Adams | B |
| S2 | Bob | Math | A2 | Dr. Jones | Room 305 | CS101 | Dr. Lee | A |
| S3 | Carol | CS | A1 | Dr. Smith | Room 201 | CS101 | Dr. Lee | A |
| S3 | Carol | CS | A1 | Dr. Smith | Room 201 | CS202 | Dr. Patel | A |
| S3 | Carol | CS | A1 | Dr. Smith | Room 201 | MATH101 | Dr. Adams | B |

**Composite Primary Key:** `(StudentID, CourseID)`

**What we fixed:**
- Every cell contains exactly one value.
- Each row is unique via the composite key.

**What's still wrong:**
- `StudentName`, `Major`, `AdvisorID`, `AdvisorName`, `AdvisorOffice` depend only on `StudentID` (partial dependency).
- `InstructorName` depends on the course, not on the student-course combination (potentially).

---

## Step 3: Remove Partial Dependencies → 2NF

**Rule:** Every non-key column must depend on the *entire* composite primary key, not just part of it.

Identify the partial dependencies:
- `StudentName`, `Major`, `AdvisorID`, `AdvisorName`, `AdvisorOffice` → depend only on `StudentID`
- `InstructorName` → depends only on `CourseID` (assuming one instructor per course)

**Decompose into:**

### Students Table

| StudentID | StudentName | Major | AdvisorID | AdvisorName | AdvisorOffice |
|-----------|-------------|-------|-----------|-------------|---------------|
| S1 | Alice | CS | A1 | Dr. Smith | Room 201 |
| S2 | Bob | Math | A2 | Dr. Jones | Room 305 |
| S3 | Carol | CS | A1 | Dr. Smith | Room 201 |

**Primary Key:** `StudentID`

### Courses Table

| CourseID | InstructorName |
|----------|----------------|
| CS101 | Dr. Lee |
| CS202 | Dr. Patel |
| MATH101 | Dr. Adams |

**Primary Key:** `CourseID`

### Enrollments Table

| StudentID | CourseID | Grade |
|-----------|----------|-------|
| S1 | CS101 | A |
| S1 | CS202 | B |
| S2 | MATH101 | B |
| S2 | CS101 | A |
| S3 | CS101 | A |
| S3 | CS202 | A |
| S3 | MATH101 | B |

**Primary Key:** `(StudentID, CourseID)`

**What we fixed:**
- `Grade` depends on the full key `(StudentID, CourseID)` — correct.
- Student info is stored once per student.
- Course instructor is stored once per course.

**What's still wrong:**
- In the Students table, `AdvisorName` and `AdvisorOffice` depend on `AdvisorID`, not on `StudentID`. That's a transitive dependency: `StudentID → AdvisorID → AdvisorName, AdvisorOffice`.

---

## Step 4: Remove Transitive Dependencies → 3NF

**Rule:** No non-key column should depend on another non-key column. Every non-key column must depend *directly* on the primary key.

The transitive dependency chain:
```
StudentID → AdvisorID → AdvisorName, AdvisorOffice
```

`AdvisorName` and `AdvisorOffice` don't describe the student — they describe the advisor. Extract them.

**Decompose the Students table into:**

### Students Table (revised)

| StudentID | StudentName | Major | AdvisorID |
|-----------|-------------|-------|-----------|
| S1 | Alice | CS | A1 |
| S2 | Bob | Math | A2 |
| S3 | Carol | CS | A1 |

**Primary Key:** `StudentID`
**Foreign Key:** `AdvisorID` → Advisors table

### Advisors Table

| AdvisorID | AdvisorName | AdvisorOffice |
|-----------|-------------|---------------|
| A1 | Dr. Smith | Room 201 |
| A2 | Dr. Jones | Room 305 |

**Primary Key:** `AdvisorID`

**What we fixed:**
- Advisor info is stored exactly once. Updating Dr. Smith's office is now a single-row change.
- No more transitive dependencies in the Students table.

---

## Step 5: Verify All Determinants Are Candidate Keys → BCNF

**Rule:** For every functional dependency `X → Y`, `X` must be a superkey (i.e., a candidate key or contains one).

Let's check each table:

### Students Table
- `StudentID → StudentName, Major, AdvisorID`
- `StudentID` is the primary key. ✓ BCNF satisfied.

### Advisors Table
- `AdvisorID → AdvisorName, AdvisorOffice`
- `AdvisorID` is the primary key. ✓ BCNF satisfied.

### Courses Table
- `CourseID → InstructorName`
- `CourseID` is the primary key. ✓ BCNF satisfied.

### Enrollments Table
- `(StudentID, CourseID) → Grade`
- `(StudentID, CourseID)` is the primary key. ✓ BCNF satisfied.

All determinants are candidate keys. The schema is in BCNF.

---

## Final Schema

```
Students (StudentID PK, StudentName, Major, AdvisorID FK)
Advisors (AdvisorID PK, AdvisorName, AdvisorOffice)
Courses (CourseID PK, InstructorName)
Enrollments (StudentID PK/FK, CourseID PK/FK, Grade)
```

### Relationships

```
Students >--|| Advisors       (Many students to one advisor)
Students ||--< Enrollments    (One student to many enrollments)
Courses  ||--< Enrollments    (One course to many enrollments)
```

---

## Before and After Comparison

| Concern | Before (Unnormalized) | After (BCNF) |
|---------|----------------------|---------------|
| Update Dr. Smith's office | Change every row where AdvisorID = A1 (risk of inconsistency) | Change one row in Advisors table |
| Add a new student with no enrollments | Impossible without NULL courses or dummy data | Insert into Students table alone |
| Delete Carol's only MATH101 enrollment | Might lose MATH101 course info if she were the only one | Enrollments row deleted; Courses table unaffected |
| Storage | 7 wide rows with heavy duplication | 4 lean tables, no duplication |
| Data integrity | Relies on discipline | Enforced by schema structure and foreign keys |

---

## Key Takeaways

1. **Normalization is incremental** — each step fixes one category of problem.
2. **Decomposition is lossless** — you can always reconstruct the original data by joining the normalized tables.
3. **Foreign keys are the glue** — they replace redundant data with references.
4. **Most systems stop at 3NF or BCNF** — further normalization adds complexity with diminishing returns.
5. **Denormalization is a conscious tradeoff** — sometimes you intentionally reintroduce redundancy for read performance, but you do so knowing the risks.
