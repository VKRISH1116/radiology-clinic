# Changelog

All notable changes to the Radiology Clinic app are recorded here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html):
`MAJOR.MINOR.PATCH` — MAJOR = breaking change, MINOR = new feature (backward
compatible), PATCH = bug fix (backward compatible).

## [Unreleased]

Work in progress lands here until the next tagged release. See the prioritised
backlog in [`docs/Maintenance.html`](docs/Maintenance.html) for what's planned.

### Added
- **Health-check endpoint** — public `GET /health` returns `200 {status:UP, db:UP}`
  when the app and database are reachable, or `503 DOWN` if the database ping
  fails. Enables external uptime monitoring (no auth required). Backend test
  count is now 80.

## [1.0.0] — 2026-07-16

First public release. The Radiology Clinic MVP — an ultrasound-clinic web app
for patients, staff, and admins — live on Render and verified end to end.

### Added
- **Patients** — register with consent, log in, browse the study catalogue,
  book multi-study appointments (with a live bill), view / cancel / reschedule
  appointments, and download report PDFs.
- **Staff** — daily schedule, mark appointments complete, upload report PDFs,
  and walk-in booking with pick-or-type referring-doctor tagging.
- **Admin** — KPI dashboard, catalogue editing with price snapshotting,
  referral rules and ledger with payouts, user management, and an audit log.
- **Referral engine** — configurable, specificity-scored rules where the most
  specific matching rule wins.
- **Platform** — JWT authentication (short-lived access + revocable refresh
  tokens), role-based access control, PostgreSQL via Flyway migrations,
  Dockerised backend and frontend, CI on GitHub Actions, and 100 automated
  tests (79 backend + 20 frontend + 1 Playwright end-to-end).

### Known limitations
- Password reset is not yet available (no email transport). Tracked as AC-F1-3.
- Report PDFs are stored on ephemeral disk on the Render free tier, so they do
  not survive a redeploy. A persistent disk or object storage is planned.
- The first request after ~15 minutes of idle is slow on the free tier while the
  service cold-starts.
- The change-cutoff policy is not yet enforced on the staff "complete" action.

See [`docs/Maintenance.html`](docs/Maintenance.html) for the full backlog and
tech-debt register.

[Unreleased]: https://github.com/VKRISH1116/radiology-clinic/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/VKRISH1116/radiology-clinic/releases/tag/v1.0.0
