# Database Normalization

## What Is It?

Database normalization is a process of organizing a relational database's structure to reduce data redundancy and improve data integrity. It involves decomposing tables into smaller, well-structured tables and defining relationships between them according to a series of rules called "normal forms."

---

## Why Do We Need It?

- **Eliminate redundant data** — storing the same piece of information in multiple places wastes space and creates maintenance headaches.
- **Ensure data integrity** — when data lives in exactly one place, updates can't leave the database in an inconsistent state.
- **Simplify queries and maintenance** — well-structured tables are easier to query, update, and extend.
- **Enforce logical dependencies** — every column in a table should depend on the table's key, not on other non-key columns.

---

## What Does It Prevent?

Normalization prevents three types of anomalies that arise when data is stored redundantly in a poorly structured table.

Consider this **unnormalized** table as a running example:

| OrderID | CustomerID | CustomerName | CustomerAddress | Product | Price |
|---------|------------|--------------|-----------------|---------|-------|
| 1001 | C1 | Alice | 123 Main St | Widget | 9.99 |
| 1002 | C1 | Alice | 123 Main St | Gadget | 19.99 |
| 1003 | C2 | Bob | 456 Oak Ave | Widget | 9.99 |
| 1004 | C1 | Alice | 123 Main St | Gizmo | 14.99 |

Customer data is duplicated across every order row. This creates the following problems:

---

### Update Anomaly

**What it is:** When the same fact is stored in multiple rows, updating it requires changing *every* copy. If some copies are missed, the data becomes inconsistent.

**Example:** Alice moves to a new address. Her address appears in rows 1001, 1002, and 1004. If a query updates rows 1001 and 1002 but misses 1004, the database now contains two different addresses for Alice — and there's no way to know which is correct.

**Why it matters:**
- Silent data corruption that may go unnoticed for a long time.
- Queries filtering by address will return partial results.
- The more rows that contain the duplicated data, the higher the risk.

**How normalization fixes it:** Store the customer's address in a single row in a `Customers` table. Orders reference the customer by `CustomerID`. Now an address change is a single-row update.

---

### Insertion Anomaly

**What it is:** The table's structure makes it impossible to store certain facts without also storing unrelated facts that may not yet exist.

**Example:** You want to register a new customer (Charlie, 789 Pine Rd) in the system, but this table requires an `OrderID` and `Product`. Since Charlie hasn't placed an order yet, you either:
- Can't insert the row at all (if `OrderID` is part of the primary key), or
- Must insert NULL values for order-related columns, violating key constraints.

**Why it matters:**
- Legitimate data can't be recorded until an unrelated event occurs.
- Workarounds (dummy rows, NULLs in key columns) pollute the data and complicate queries.
- Business processes are artificially coupled — customer registration shouldn't depend on order placement.

**How normalization fixes it:** Separate the `Customers` table from the `Orders` table. Now you can insert a customer independently of whether they've placed an order.

---

### Deletion Anomaly

**What it is:** Removing a row to delete one fact accidentally destroys an unrelated fact that was stored in the same row.

**Example:** Bob's only order (1003) is cancelled and deleted. Since Bob's customer information (name, address) only exists in that row, deleting the order also erases all knowledge of Bob from the database.

**Why it matters:**
- Unintentional data loss — you wanted to remove an order, not a customer.
- Recovery requires re-entering data that may no longer be available.
- The more entities crammed into a single table, the more facts are at risk when any row is deleted.

**How normalization fixes it:** Bob's information lives in the `Customers` table regardless of how many orders he has. Deleting his order leaves his customer record intact.

---

### Summary

| Anomaly | Root Cause | Consequence | Normalization Fix |
|---------|-----------|-------------|-------------------|
| Update | Same fact in multiple rows | Inconsistent data after partial updates | Store each fact once, reference by key |
| Insertion | Unrelated facts in one table | Can't add data without fabricating related data | Decompose into independent tables |
| Deletion | Unrelated facts in one table | Removing one fact destroys another | Decompose into independent tables |

---

## How Is It Done? — The Normal Forms

### 1NF (First Normal Form)

- Each column holds atomic (indivisible) values.
- No repeating groups or arrays inside a single cell.
- Each row is unique (has a primary key).

**Violation:** A `PhoneNumbers` column containing `"555-1234, 555-5678"`.

**Fix:** Move phone numbers to a separate table with one row per number.

---

### 2NF (Second Normal Form)

- Already in 1NF.
- Every non-key column depends on the *entire* primary key, not just part of it.

**Violation:** In a table with composite key `(StudentID, CourseID)`, a `StudentName` column depends only on `StudentID`.

**Fix:** Move `StudentName` to a `Students` table keyed on `StudentID`.

---

### 3NF (Third Normal Form)

- Already in 2NF.
- No non-key column depends on another non-key column (no transitive dependencies).

**Violation:** A table with `EmployeeID → DepartmentID → DepartmentName`. The department name depends on the department ID, not on the employee.

**Fix:** Move `DepartmentName` into a `Departments` table.

---

### BCNF (Boyce-Codd Normal Form)

- A stricter version of 3NF: every determinant (column that other columns depend on) must be a candidate key.

---

### 4NF and 5NF

These deal with multi-valued dependencies and join dependencies — rarely needed in practice but useful for complex scenarios.

---

## In Practice

Most production databases target **3NF or BCNF**. Going further can make queries overly complex with excessive joins, so designers sometimes *denormalize* specific areas intentionally for read performance (e.g., in data warehouses or caching layers), accepting the tradeoff of some redundancy for faster reads.

### The General Approach

1. Start with raw, unnormalized data.
2. Identify the primary key for each entity.
3. Remove repeating groups → 1NF.
4. Remove partial dependencies → 2NF.
5. Remove transitive dependencies → 3NF.
6. Verify all determinants are candidate keys → BCNF.

Each step decomposes tables further, producing a cleaner schema with well-defined foreign key relationships between them.
