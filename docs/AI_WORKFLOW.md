# AI Workflow Log

Tool: Claude Code (agentic coding assistant). This log records how AI was used at each step,
what I asked for, and what I accepted, changed or rejected.

| # | Step | What I asked AI to do | What I decided / changed |
|---|---|---|---|
| 1 | Requirements | Read the brief and draft a one-page requirements doc with explicit out-of-scope items | Confirmed v1 scope: employee CRUD + per-country insights + 10k seed. Kept FX conversion, auth and salary history out, with reasons |
| 2 | Design | Propose architecture, data model, API and test strategy for the agreed scope | Chose Spring Boot + Angular (matches the role), SQLite per the brief, a single deployable, median computed in app code |
| 3 | Backend scaffold | Generate a Spring Boot project and wire up SQLite | Kept the Initializr output; set a single-connection pool (SQLite allows one writer) and in-memory SQLite for tests, so tests run on the same engine as production |
| 4 | SalaryStatistics (TDD) | Write the tests first from my list of rules, then the implementation | I defined the rules: an empty group is an error rather than zeros, median of an even count is the mean of the two middle values, rounding is 2 dp half-up. Committed red tests before the implementation |
| 5 | Employee entity + repository | Implement the entity, `Country` enum, filter specification and persistence tests against real SQLite | **The tests caught a defect the AI-generated setup had introduced:** Hibernate's SQLite dialect silently skipped the unique index on `employee_code`, and SQLite constraint errors weren't translated. I chose to move the schema to a Flyway migration (explicit DDL, no `ddl-auto`) and add a small dialect subclass, rather than weakening the test to accept a generic exception |
| 6 | Employee CRUD API | Implement service, controller, validation, RFC 7807 errors, and unit + web-slice + end-to-end tests | I decided the employee code is immutable (separate create/update requests) and that sort fields are whitelisted. **Corrected AI-written tests:** (1) they asserted repeatedly on a `MockMvcTester` request builder, which re-sends the request each time (Mockito caught it as a double call), so I switched to a single `exchange()`; (2) tests leaked data through the shared in-memory DB, so each test now cleans up explicitly |
