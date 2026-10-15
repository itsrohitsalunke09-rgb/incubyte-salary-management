# ACME Salary Management

A web app that lets ACME's HR Manager manage salary data for 10,000 employees across six countries
and answer **"how does the org pay people?"**. It replaces a set of Excel sheets.

- **Employees**: search, filter by country / department / job title, sort, page through 10k records;
  add, edit and delete with validation.
- **Insights**: per-country headcount, median salary and payroll, plus a drill-down by job title or
  department (min, median, average, max, payroll, pay-range bars). Every figure is in local currency.

**Live demo:** https://acme-salary-management-ncyf.onrender.com
(Render free tier: the first request after ~15 idle minutes wakes the instance and can take a minute or two,
because it restarts and re-seeds the 10,000 employees.)

**Video:** _link added after recording_

## Docs (start here)
| Doc | What's in it |
|---|---|
| [Requirements](docs/REQUIREMENTS.md) | Goal, persona, scope, what's deliberately left out and why |
| [Design](docs/DESIGN.md) | Architecture, data model, API, trade-offs, and decisions revised during the build |
| [AI workflow](docs/AI_WORKFLOW.md) | How AI was used at each step, and what I corrected or rejected |

The commit history follows the plan in the design doc (requirements → design → test-first domain logic →
persistence → API → seed → UI), so it can be read step by step.

## Tech
Spring Boot 4.1 (Java 21) · SQLite + Flyway · Angular 22 (standalone, signals, zoneless) · Angular Material ·
JUnit 5 / Mockito / MockMvc · Vitest + Material test harnesses · Docker.

## Run it locally
Prerequisites: Java 21, Node 24.

```bash
# Terminal 1 - API on :8080 (seeds 10,000 employees into ./salary.db on first start)
cd backend
./mvnw spring-boot:run

# Terminal 2 - UI on :4200, proxying /api to :8080
cd frontend
npm ci
npm start
```
Open http://localhost:4200.

## Tests
```bash
cd backend && ./mvnw test          # 66 tests: domain, persistence (real SQLite), API, seed, SPA routing
cd frontend && npx ng test --watch=false   # 55 tests: API client, validators, list, form, insights
```

## Production build / deploy
One Docker image: Angular is built and bundled into the Spring Boot jar, and API and UI are served from
the same origin.
```bash
docker build -t acme-salary .
docker run -p 8080:8080 acme-salary   # http://localhost:8080
```
`render.yaml` deploys the same image on Render. Configuration is via environment variables: `PORT`,
`SALARY_DB_PATH`, and `SALARY_SEED_ENABLED` / `SALARY_SEED_COUNT`.

## Known limitations (see Design)
- Salaries are compared **within** a country only; there is no currency conversion.
- Insight statistics include part-time employees, which lowers the min and median for some roles.
- No authentication; there is a single HR persona in v1.
- On free hosting the SQLite file is ephemeral, so data resets to the seed on redeploy.
