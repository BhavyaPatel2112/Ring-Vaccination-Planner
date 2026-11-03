# Ring Vaccination Planner: Work Split

Replaces the suggested split in `PROJECT_PLAN.md` section 13. Feature definitions are in `PROJECT_PLAN.md` sections 4.1, 4.2 and 4.4.

- **Bhavya Patel:** Tuesday 3 Nov to Sunday 8 Nov 2026 (6 days, then on vacation)
- **Tanushree:** Monday 9 Nov to Friday 20 Nov 2026 (12 days, through final submission and demo)
- **Repository:** https://github.com/BhavyaPatel2112/Ring-Vaccination-Planner (Tanushree's invite as `tgbhatt` is pending; accept it before Monday)

**Scope:** 12 main features, 6 field features (A, B, C, D, E, G) and a proper frontend.

## Principles behind the split

1. **Algorithms first.** Each person owns an equally important headline algorithm with a proof to defend, an equally hard second algorithm, and a CO5 randomized component. Each person's viva topics come from their own code.
2. **Each feature goes to the person who owns its algorithm.** Whoever writes the algorithm also builds the service and endpoint that use it.
3. **Field features 3 and 3.** Each one goes to the person whose algorithm it extends.
4. **Equal hours.** About 66 hours each. Bhavya's 66 hours fit into 6 days and Tanushree's into 12, so Bhavya's days are about twice as long.
5. **Relay order.** Bhavya builds everything the rest depends on: data model, parser, ring builder, solver interface, API conventions and frontend foundation. Tanushree builds on top of it.

---

## 1. Algorithm split (the most important part)

| | Bhavya | Tanushree |
|---|---|---|
| **Headline algorithm** (central to the app) | **Vertex cover 2-approximation** (Unit 5). Produces the vaccination plan, the core decision of the app. | **Bloom filter with own hash functions** (Unit 6, CO5). Powers the exposure check, the double-dose guard and the tuning panel. |
| **Proof to defend in viva** | 2-approximation proof via maximal matching; tight examples (star, K(n, n)); guarantee still holds with excluded people | False positive formula (1 − e^(−kn/m))^k, optimal k = (m/n) ln 2, why there are no false negatives |
| **Hard algorithm** | **Exact vertex cover**: bounded search tree, branch and bound, degree-1 reduction, matching lower bound; NP-hardness via independent set (Unit 6 complexity) | **Indexed binary max-heap with decrease-key** plus **greedy vertex cover** on top of it; why greedy has no constant guarantee (ln n worst case) (Unit 2) |
| **CO5 randomized component** | **Randomized Quickselect** (expected O(n)) | **Bloom filter** (above), plus a seeded Monte Carlo outbreak simulation |
| **Second algorithm** | **Median-of-medians** (worst case O(n)), the deterministic baseline for Quickselect (Unit 1) | **Disjoint-set union** with union by rank and path compression, inverse Ackermann bound (Unit 2) |
| **Evaluation tool** | Benchmark harness | **Outbreak simulator** (SIR model, efficacy, percentiles) |
| **Support code** | ContactGraph, CsvParser, RingBuilder (BFS), SyntheticNetworkGenerator | ContactFilter (field feature E) |
| **Syllabus units covered** | 1, 5, 6 | 2, 6 |

**Why this is balanced**

- **Importance:** the vertex cover is the app's core decision, and the Bloom filter is its mandatory CO5 component. Neither works without the other: no CO5 means an incomplete submission, and no vertex cover means no plan.
- **Difficulty:** the exact solver (hardest on Bhavya's side) is matched by the heap with decrease-key plus greedy (hardest on Tanushree's side). Median-of-medians is matched by the Bloom filter with self-written FNV-1a and MurmurHash3. Quickselect is matched by the simulator. The 2-approximation (short code, deep proof) is matched by DSU (short code, deep amortized bound).
- **Comparison:** each person has baselines to benchmark. Bhavya compares approximation against exact, and Quickselect against median-of-medians and full sort. Tanushree compares greedy against approximation at scale, Bloom filter against `HashSet`, and DSU against recomputing with BFS.
- **CO5:** both own a randomized component, so either can answer a CO5 question in the viva.

---

## 2. Main features (12)

| # | Feature | Owner (algorithm and backend) | Algorithm |
|---|---|---|---|
| 1 | Network upload and validation | **Bhavya** | Parser, ContactGraph |
| 3 | Ring builder | **Bhavya** | BFS |
| 4 | Vaccination plan | **Bhavya** | 2-approximation |
| 8 | Budget mode | **Bhavya** | Quickselect plus 2-approximation |
| 2 | Outbreak cluster view | **Tanushree** | DSU |
| 12 | Live contact updates | **Tanushree** | DSU |
| 6 | Exposure self-check | **Tanushree** | Bloom filter plus exact check |
| 9 | Double-dose guard | **Tanushree** | Second Bloom filter plus exact registry |
| 10 | Bloom filter tuning panel | **Tanushree** | Bloom filter |
| 11 | Outbreak simulation | **Tanushree** | Simulator, Quickselect percentiles |
| 5 | Method comparison | **Shared:** Bhavya builds the endpoint and the approximation and exact columns; Tanushree adds the greedy column | All three solvers |
| 7 | Benchmark and proof page | **Shared:** Bhavya builds the harness and runs approximation versus exact, exact growth, and Quickselect versus median-of-medians versus sort. Tanushree runs approximation versus greedy at 1k to 1M contacts, DSU versus BFS, and Bloom versus `HashSet`. | All |

Tanushree has more feature rows (6 against 4), but they cost less each: three share the Bloom filter and two share DSU. Bhavya's four are the heaviest in the app. Upload and validation and the ring builder are the foundation everything else needs, the plan is the core output, and budget mode combines two algorithms.

---

## 3. Field features (3 each)

| | Feature | Owner | Extends | What it does |
|---|---|---|---|---|
| A | Can't-vaccinate handling | **Bhavya** | 2-approximation, exact solver | People marked ineligible or refused are never chosen. On a link with one excluded person, the other person is forced into the plan. Links where both people are excluded are listed as "cannot be protected by vaccination: quarantine or monitor". Recomputes after a refusal. |
| D | "Why this person?" explanation | **Bhavya** | 2-approximation, budget mode | Every person in the plan carries a reason, for example "Ring 1 contact of P002", "Matched link P011 to P019", "Only eligible person on link with P020", or "Budget priority: top 10% most connected". |
| G | Contact tracing quality warnings | **Bhavya** | Quickselect | Flags cases with zero recorded contacts, people above the 99th percentile of contacts (found with Quickselect), and contacts dated in the future or after confirmation |
| B | "Vaccinate by" deadline | **Tanushree** | Indexed heap | Deadline = last exposure + post-exposure window (default 72 hours, configurable). The list is ordered by urgency using the heap. People past the window are flagged "outside window: monitor instead". |
| C | Symptom monitoring list | **Tanushree** | Ring builder output | Ring members not in the plan and not vaccinated, plus anyone past the window, each with a monitoring end date (last exposure + incubation period, default 21 days) |
| E | Contact definition settings | **Tanushree** | ContactFilter (new) | Minimum duration (default 15 minutes) and infectious window in days filter contacts before the ring is built. Shows how many contacts were excluded and why. |

---

## 4. Frontend split

**Bhavya builds the foundation, so the app looks like one product. Tanushree builds most screens on top of it.**

| Bhavya (before Sunday) | Tanushree (from 12 Nov) |
|---|---|
| Design tokens: colours with one fixed meaning each (cases red, ring 1 orange, ring 2 yellow, vaccinated green, excluded grey), typography, spacing, light and dark mode | **Dashboard** home: cases, clusters, doses planned, links protected and unprotected, next deadlines |
| App shell, navigation, and the guided workflow stepper (load data, then mark cases, then review ring, then plan, then act) | **Clusters** screen: click to highlight, add a live contact |
| API client with shared error handling | **Plan** screen: sortable and searchable table with reasons (D), the guarantee text, budget control, can't-vaccinate marking (A), data warnings (G) |
| Shared components: table, card, stat tile, loading, empty and error states, toasts | **Compare** screen |
| **Upload** screen: file pickers, validation warnings with line numbers, sample-data button | **Deadlines and monitoring** screen (B, C) and **contact settings** panel (E) |
| **Network view** (Cytoscape): ring colours, zoom, search for a person, sampled-view notice, legend | **Public exposure self-check** page: simple, mobile-friendly, privacy-safe |
| | **Bloom tuning**, **Simulation** and **Benchmarks** (Chart.js) screens |
| | Accessibility pass: keyboard navigation, contrast, colour never the only signal |

By Sunday the app will have one working end-to-end flow in the browser: upload, then the network with rings. This meets the guidelines' "working prototype" checkpoint.

---

## 5. Bhavya: Tuesday 3 Nov to Sunday 8 Nov

| Day | Work | Done when |
|---|---|---|
| **Tue 3 Nov** | Repo setup: Spring Boot backend (Java 17, Maven wrapper, Spring Web, Validation), Vite React frontend, `.gitignore`, folder layout from plan section 7, commit `PROJECT_PLAN.md` and this file. Write the proposal (`docs/proposal.md`) with both names and IDs, both sets of algorithms and the field features; submit it. Start `ContactGraph`. | Both apps start; proposal submitted |
| **Wed 4 Nov** | `ContactGraph`; `CsvParser` with every validation case in plan section 5, including optional `eligible` / `refused` columns for A. Sample CSVs in `data/sample/` (small realistic outbreak, star, K(3,3), greedy worst case). `SyntheticNetworkGenerator` (seeded). | Parser and generator tests pass |
| **Thu 5 Nov** | `RingBuilder`. The `VertexCoverSolver` interface and `CoverResult` (people, reasons, unprotectable links) that Tanushree's greedy will implement. `ApproxVertexCover` with exclusions (A) and reasons (D). `RandomizedQuickselect` and `MedianOfMedians`. | 5+ tests each, edge cases included, all green |
| **Fri 6 Nov** | `ExactVertexCover` with exclusions, size limit and timeout; brute-force checker in tests. Budget mode (ring 1 first, then top 10% by Quickselect, unprotected links listed; budget 0, negative and oversized). Data quality warnings (G). | Exact matches brute force on 50 seeded graphs; budget edge cases tested |
| **Sat 7 Nov** | API layer: global exception handler with `{ error, message, details }`, CORS, `NetworkService`, `PlanService`. Endpoints: `POST /api/networks`, `POST /api/networks/generate`, `GET /api/networks/{id}`, `POST .../ring`, `POST .../plan` (APPROX, EXACT, budget, exclusions), `POST .../compare` (greedy slot left for Tanushree). MockMvc tests for 200, 400, 404 and 422. Frontend foundation (tokens, shell, stepper, API client, shared components). | Upload, ring, plan and compare work with `curl`; app shell renders |
| **Sun 8 Nov** | Upload screen and network view. Benchmark harness (warm-up, 5+ repeats, median, seeded, CSV to `docs/benchmarks/`) and his three experiments. Report sections. `HANDOVER.md`. Handover call. | Everything pushed, `main` green, call done |

**Bhavya's report sections** (in `docs/report/`): system design and API diagram; ring builder and modelling assumptions; 2-approximation pseudocode and proof, including the case with exclusions; exact solver and NP-hardness; Quickselect versus median-of-medians; complexity table for his classes; his benchmark results; his individual contribution.

**`HANDOVER.md` must cover:** build, run and test from a fresh clone; package map; every endpoint with a `curl` example; how greedy plugs into `VertexCoverSolver` and the compare endpoint; where `ContactFilter` (E) plugs in before `RingBuilder`; the plan response fields left for B and C; how to add a benchmark; how to add a screen with the design tokens and shared components; known issues.

---

## 6. Tanushree: Monday 9 Nov to Friday 20 Nov

| Day | Work | Done when |
|---|---|---|
| **Mon 9 Nov** | Clone, build and run on her machine; read `HANDOVER.md`. `DisjointSetUnion` with cluster stats. Endpoints `GET .../clusters` and `POST .../contacts` (features 2 and 12). | Clusters and live merge work with `curl` |
| **Tue 10 Nov** | `IndexedMaxHeap` with `decreaseKey`. `GreedyVertexCover` implementing `VertexCoverSolver` (respects exclusions, gives reasons). Add GREEDY to plan and compare. | 5+ tests each; compare returns all three methods |
| **Wed 11 Nov** | `HashFunctions` (own FNV-1a and MurmurHash3) and `BloomFilter` (double hashing, theoretical rate, optimal k). `ExposureService`: Bloom hit, then exact check-in confirmation; the result reveals venue and date only. | No false negatives; measured rate near theory |
| **Thu 12 Nov** | Double-dose guard (feature 9). Tuning endpoints (feature 10). `OutbreakSimulator` (seeded, efficacy, random-plan baseline, percentiles via Bhavya's Quickselect). | Features 6, 9, 10 work with `curl`; simulator tests pass |
| **Fri 13 Nov** | Simulate endpoint (feature 11). Deadlines (B), monitoring list (C), `ContactFilter` (E). MockMvc tests for all her endpoints. | Every backend feature reachable |
| **Sat 14 Nov** | Frontend: Dashboard, Clusters, Plan screens | Plan workflow complete in the browser |
| **Sun 15 Nov** | Frontend: Compare, Deadlines and monitoring, contact settings | |
| **Mon 16 Nov** | Frontend: public exposure page, Bloom tuning, Simulation, Benchmarks; accessibility pass | All 12 + 6 features work in the UI |
| **Tue 17 Nov** | Her benchmarks; SocioPatterns network into `data/sociopatterns/` with licence and citation; demonstrate every edge case in the UI and capture screenshots into `docs/screenshots/` | All benchmark CSVs and charts exist |
| **Wed 18 Nov** | Report: Bloom filter, heap and greedy, DSU, simulation, her complexity and benchmarks; merge Bhavya's sections; limitations; individual contributions. 5 to 8 pages. | Report complete |
| **Thu 19 Nov** | README (prerequisites, run commands, API examples, sample data, screenshots, attribution); fresh clone test; rehearse the demo twice | Fresh clone runs from the README alone |
| **Fri 20 Nov** | Final submission, live demo and viva | Submitted |

---

## 7. Effort balance

| Bhavya | Hours | Tanushree | Hours |
|---|---|---|---|
| Repo, backend and frontend setup | 3 | Onboarding | 2 |
| Proposal | 3 | DSU | 2 |
| ContactGraph, CsvParser, validation, sample data (F1) | 7 | Indexed heap | 3 |
| Synthetic generator | 3 | Greedy vertex cover (F5 greedy) | 3 |
| RingBuilder (F3) | 3 | Bloom filter and hash functions | 5 |
| 2-approximation (F4) | 3 | Outbreak simulator | 4 |
| Exact solver and brute-force check | 6 | Clusters and live updates endpoints (F2, F12) | 2 |
| Quickselect | 2.5 | Exposure check (F6) | 2 |
| Median-of-medians | 2.5 | Double-dose guard (F9) | 2 |
| Budget mode (F8) | 3 | Tuning endpoints (F10) | 1.5 |
| A: Can't-vaccinate | 3 | Simulate endpoint (F11) | 1.5 |
| D: Why this person | 1.5 | B: Vaccinate-by deadline | 1.5 |
| G: Data quality warnings | 1.5 | C: Monitoring list | 1.5 |
| API layer, his endpoints, MockMvc | 7 | E: Contact definition settings | 2 |
| Frontend foundation | 5 | MockMvc for her endpoints | 2 |
| Upload screen and network view | 4 | Frontend screens | 16 |
| Benchmark harness and his experiments | 4 | Her benchmarks | 3 |
| Report sections and handover | 4 | SocioPatterns data | 2 |
| | | Edge-case evidence and screenshots | 2 |
| | | Report sections and final assembly | 5 |
| | | README and fresh clone check | 3 |
| **Total** | **66** | **Total** | **66** |

Shared: the demo on 20 Nov and each person's own viva prep (plan section 15). Bhavya prepares the 2-approximation, NP-hardness, the exact solver, and Quickselect versus median-of-medians. Tanushree prepares the Bloom filter, the heap and greedy, DSU and the simulation.

---

## 8. Ground rules

1. **Own machine, own commits.** The rubric grades contribution attribution. Each person commits only their own work from their own GitHub account.
2. **Push every evening.** Nothing should be left only on Bhavya's laptop when he leaves.
3. **`main` always builds** and all tests pass before pushing.
4. **If Bhavya falls behind, this is the order work moves to Tanushree:** benchmark harness, then the upload screen and network view, then field feature G. The parser, ring builder, the 2-approximation, the exact solver, Quickselect and median-of-medians, the solver interface and the core API must be done by Sunday. They are his viva algorithms, and everything else depends on them.
5. **Faculty checkpoints after 8 Nov** (design review, prototype review) are presented by Tanushree from the handover notes.
6. **Bhavya's viva:** confirm Bhavya is back for 20 Nov. If not, ask the faculty now how an individual viva can be arranged.
