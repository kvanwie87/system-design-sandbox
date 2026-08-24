# Data Normalization

A study guide for database normalization — what it is, why it matters, and how to do it step by step.

## Contents

| File | Description |
|------|-------------|
| [database-normalization.md](database-normalization.md) | Core concepts — what normalization is, why we need it, the three anomalies it prevents, and the normal forms (1NF through BCNF) |
| [normalization-example-enrollment.md](normalization-example-enrollment.md) | Worked example: normalizing a university enrollment table from raw data to BCNF |
| [normalization-example-spotify.md](normalization-example-spotify.md) | Worked example: normalizing Spotify's song catalog (PostgreSQL) from a flat ingestion table to BCNF with final DDL |

## How to Use

Start with `database-normalization.md` for the theory, then work through the examples to see the process applied to real-world domains.

Each example follows the same progression:
1. Start with raw, unnormalized data
2. Identify entities and keys
3. Remove repeating groups (1NF)
4. Remove partial dependencies (2NF)
5. Remove transitive dependencies (3NF)
6. Verify BCNF compliance
