# 3. What is done (Bhavya, 3 to 4 November)

Everything below is committed and pushed. Backend tests: **141, all passing** (`cd backend && ./mvnw test`).

## Data layer (`backend/.../data`)

| Piece | What it does | Tests |
|---|---|---|
| `CsvParser` + `CsvReader` | Reads people, contacts, check-ins and cases. Every rule from plan section 5: empty file or bad header is a 400; self-contacts, unknown people, malformed rows and invalid values are rejected and reported with their line number; duplicate contacts (either direction) are merged, keeping the most recent date and longest duration. Bad rows in cases.csv are errors, not warnings, because a case must never be silently dropped. Optional `eligible` and `refused` columns support feature A. Warnings are capped per kind, with a summary of the rest. | `CsvParserTest` (19) |
| `SyntheticNetworkGenerator` | Seeded realistic towns: households of 2 to 6, schools and workplaces of 20 to 200, and community links. Average contacts within 5% of the target. A 1,000,000-contact network in under a second. | `SyntheticNetworkGeneratorTest` (8) |
| `ContactQualityCheck` (feature G) | Warns about cases with no contacts, outliers above the 99th percentile and over 3 times the median (both via Quickselect), future contacts, contacts after confirmation, and missing details | `ContactQualityCheckTest` (9) |
| Sample data `data/sample/` | `outbreak-small` (40 people, 2 cases, infant, pregnant person, refusal, check-ins), `validation-demo` (one of every error), `star`, `complete-bipartite-3-3`, `greedy-worst-case-12` and `-18` | `SampleDataTest` (7) |

## Algorithm engine (`backend/.../engine`)

| Algorithm | File | Key facts | Tests |
|---|---|---|---|
| Contact network | `graph/ContactGraph` | Dense int indices, growable int adjacency arrays, O(1) amortised add, sized for 1M contacts | `ContactGraphTest` (9) |
| Ring builder | `ring/RingBuilder` | Multi-source BFS, O(V + E). Ring 1 is mandatory. Target links are those touching ring 2 or beyond, unless an endpoint is a case, already vaccinated, or a vaccinated ring 1 member. A ring 1 member who cannot be vaccinated leaves their links as targets. Takes an optional contact filter (hook for feature E). | `RingBuilderTest` (10), including a check against a reference on 60 random networks |
| Shared solver contract | `cover/VertexCoverSolver`, `CoverProblem`, `CoverResult`, `CoverChoice` | `CoverProblem` splits target links into unprotectable, forced and free once, so every solver handles exclusions the same way. `CoverResult` carries choices with reasons, a lower bound, run time and search nodes. | `CoverProblemTest` (3) |
| 2-approximation | `cover/ApproxVertexCover` | Maximal matching: forced people first, then both ends of each uncovered free link. Proven ≤ 2 × OPT even with exclusions; returns the lower bound \|forced\| + \|matching\|. O(V + E). | `ApproxVertexCoverTest` (11): star and K(3,3) tight cases, ≤ 2 × brute force on 200 graphs, 1M contacts in about 0.85 s |
| Exact solver | `cover/ExactVertexCover` | Branch and bound over 64-bit masks: branch on the most connected person (take them, or take all their neighbours), degree-1 rule, matching lower bound, separate groups solved separately. Refuses more than 40 undecided people, or after 5 s (`ExactSolverLimitException` → 422). | `ExactVertexCoverTest` (12): matches brute force on 120 graphs |
| Randomized Quickselect | `select/RandomizedQuickselect` | Seeded random pivot, three-way partition (fast with many equal values), iterative, in place. `int[]` and `long[]`, percentile helper (nearest rank). Expected O(n). | `SelectionTest` (16, shared) |
| Median-of-medians | `select/MedianOfMedians` | Groups of 5, recursive pivot, worst-case O(n) | `SelectionTest` |
| Vaccination planner (feature 8) | `plan/VaccinationPlanner` | Full plan = ring 1 + the solver's cover. Budget mode: ring 1 by degree, then the top 10% by unprotected links (90th percentile via Quickselect), then the rest of the full plan; skips people whose links are already safe; lists unprotected links and ring 1 members left out. Budget 0 gives an empty plan; negative is rejected. | `VaccinationPlannerTest` (9) |

Field features A (exclusions) and D (reasons) live in `CoverProblem`, `ApproxVertexCover`, `ExactVertexCover` and `VaccinationPlanner`.

## REST API (`backend/.../api`, `service`)

Networks are kept in memory (`NetworkService`), with ids like `net-1a2b3c4d`. Errors always come back as `{ error, message, details }`.

| Method and path | Purpose |
|---|---|
| `GET /api/health` | Liveness |
| `GET /api/samples` | Sample networks |
| `POST /api/networks/samples/{id}` | Load a sample (201) |
| `POST /api/networks` (multipart: `people`, `contacts`, optional `checkins`, `cases`, `name`) | Upload (201) |
| `POST /api/networks/generate` `{population, avgContacts, seed?, name?}` | Synthetic network (201) |
| `GET /api/networks` | Loaded networks, newest first |
| `GET /api/networks/{id}/summary` | Counts, case ids, rejected rows, quality warnings |
| `GET /api/networks/{id}` | Nodes and links for drawing (BFS sample of 1,500 around the cases if larger) |
| `GET /api/methods` | Available solver names (`APPROX`, `EXACT`; `GREEDY` once added) |
| `POST /api/networks/{id}/ring` `{caseIds, depth?}` | Rings and target links |
| `POST /api/networks/{id}/plan` `{caseIds, depth?, method?, budget?, seed?}` | Vaccination plan with reasons and guarantee text |
| `POST /api/networks/{id}/compare` `{caseIds, depth?}` | All methods side by side; a refusal is reported in the row, not thrown |

Error codes: 400 (`INVALID_FILE`, `MISSING_FILE`, `UNKNOWN_PERSON`, `INVALID_REQUEST`), 404 (`NOT_FOUND`), 413 (`FILE_TOO_LARGE`), 422 (`EXACT_SOLVER_TOO_LARGE`, `EXACT_SOLVER_TIMED_OUT`). Tests: `ApiTest` (21) covers the success, 400, 404 and 422 paths.

## Benchmarks (`backend/.../bench`, results in `docs/benchmarks`)

- **Harness:** warm-up runs, then 5 to 7 timed runs, keeping the median (via our Quickselect). Seeded graph families (random and random d-regular), a brute-force baseline, and CSV output. `HarnessTest` (6).
- **Experiment 1:** approximation against exact on 420 networks. Mean ratio 1.36 to 1.65, never above 2.
- **Experiment 2:** exact solver on 4-regular networks from 16 to 64 people.
  - Brute force doubles with every extra person.
  - Branch and bound doubles about every 7 people (15 → 1,309 search nodes).
  - The approximation stays at a few microseconds.
- **Experiment 3:** selection on 10K to 10M values. Quickselect is about 2.5 times faster than median-of-medians and about 8 times faster than a full sort on random input; both selection methods grow linearly.

## Frontend (`frontend/src`)

- **Look and feel:** a calm "spatial" glass design, inspired by a visionOS-style health monitor and a light dashboard. Light and dark themes, phone layout.
- **Shell:** one slim header with the brand, a network switcher (all loaded networks), Load data / Network / All tools, and the theme switch. "All tools" opens a launcher listing every screen; screens not built yet show a styled placeholder.
- **Load screen (`/load`):** sample cards, drag-and-drop CSV upload with a slot per file, a town generator (people, average contacts, seed), and a load report listing every rejected row.
- **Network console (`/network`):**
  - Left: a 3-step guide, a confirmed cases card (add by ID or name, remove, ring depth 1 to 3), and "People in the rings" with Ring 1, Ring 2+, Excluded and Checks tabs.
  - Middle: the contact network (Cytoscape with fCoSE) with summary chips, a legend, zoom, fit and expand. Click a person for a popover with a callout line to their node, which can mark or remove them as a case and shows why they are in the plan.
  - Right: the plan card (Approximation or Exact, Generate, doses, run time, proven ratio bar, guarantee text) and the ring breakdown doughnut chart.
- **Shared state:** `state/workspace.jsx` holds the network, cases, depth, ring, plan and method. Rings rebuild automatically when the cases or depth change; the plan clears when its inputs change. The last network id is remembered across refreshes.

## What Bhavya did not do (on purpose)

- **Tanushree's algorithms and screens:** her algorithms and screens are left for her, so each person's commits show their own work.
- **Report and proposal:** on hold.
