# Salary Management — Requirements (v1)

## Goal
Replace ACME's Excel-based salary tracking with a web app that lets the HR Manager
**maintain salary data for ~10,000 employees across multiple countries** and
**answer "how does the org pay people?"** without spreadsheet gymnastics.

## User
One persona: the **HR Manager**. Comfortable with Excel, not technical.
Typical questions they need answered quickly:
- What do we pay a *Software Engineer* in *India* — min / median / max?
- Which countries / departments cost us the most?
- Is this new offer in line with what we pay others in the same role and country?

## In scope (v1)
1. **Employee salary records**
   - Fields: employee code (unique), full name, job title, department, country,
     currency (derived from country), annual base salary, employment type, hire date.
   - Add, view, edit, delete an employee.
   - Paginated list (10k rows) with search by name/code and filters by
     country, department, job title. Sort by salary / name / hire date.
   - Server-side validation (required fields, salary > 0, unique code).
2. **Salary insights**
   - Per country: headcount, min / max / average / median salary, total annual payroll.
   - Per job title within a country: same metrics → answers "what do we pay role X in country Y".
   - Per department within a country: headcount and total payroll.
   - All figures shown **in the country's local currency**.
3. **Seed data** — a deterministic script that generates 10,000 realistic employees
   across ~6 countries, ~8 departments, ~20 job titles with plausible salary bands.

## Deliberately out of scope (and why)
| Left out | Reason |
|---|---|
| Authentication / roles | Single persona; adds no signal for this exercise. Would be the first thing added for production (noted as a risk). |
| Currency conversion / org-wide totals in one currency | Needs an FX source and a "as-of date" policy; comparing within a country answers the real questions without it. |
| Salary history, revisions, bonuses, equity | Valuable, but doubles the data model. v1 stores current base salary only. |
| Payroll processing, tax, payslips | Different product. This is a salary *management/insight* tool. |
| Excel/CSV import & export | Natural next step for an Excel-heavy team; seed script covers the data need for now. |
| Approval workflows, audit log | Needed at scale; not required to prove the core value. |

## Success criteria
- HR Manager can find any employee and update their salary in under 30 seconds.
- Each insight question above is answerable in **one screen, one or two clicks**.
- List and insight pages respond in < 500 ms with 10,000 employees.
- Core business logic (validation, salary statistics incl. median) covered by fast unit tests.

## Open questions for Incubyte
- Should salary insights ever be compared across countries (needs FX), or is per-country enough?
- Is "AngularJS" in the brief meant literally (1.x) or modern Angular? Assuming modern Angular.
