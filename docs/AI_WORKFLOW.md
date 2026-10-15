# AI Workflow Log

Tool: Claude Code (agentic coding assistant). This log records how AI was used at each step,
what I asked for, and what I accepted, changed or rejected.

| # | Step | What I asked AI to do | What I decided / changed |
|---|---|---|---|
| 1 | Requirements | Read the brief and draft a one-page requirements doc with explicit out-of-scope items | Confirmed v1 scope: employee CRUD + per-country insights + 10k seed. Kept FX conversion, auth and salary history out, with reasons |
| 2 | Design | Propose architecture, data model, API and test strategy for the agreed scope | Chose Spring Boot + Angular (matches the role), SQLite per the brief, a single deployable, median computed in app code |
| 3 | Backend scaffold | Generate a Spring Boot project and wire up SQLite | Kept the Initializr output; set a single-connection pool (SQLite allows one writer) and in-memory SQLite for tests, so tests run on the same engine as production |
| 4 | SalaryStatistics (TDD) | Write the tests first from my list of rules, then the implementation | I defined the rules: an empty group is an error rather than zeros, median of an even count is the mean of the two middle values, rounding is 2 dp half-up. Committed red tests before the implementation |
