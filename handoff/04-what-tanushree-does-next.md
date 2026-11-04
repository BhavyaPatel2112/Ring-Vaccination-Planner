# 4. What Tanushree does next

This is the plan from [WORK_SPLIT.md](../WORK_SPLIT.md), updated for what already exists. Each task says what "done" means. How to do each one, step by step, is in [07-where-your-code-plugs-in.md](07-where-your-code-plugs-in.md).

## Before Monday (30 minutes)

- [ ] Accept the GitHub invite (`tgbhatt`) and clone the repository.
- [ ] Follow [05-run-it-locally.md](05-run-it-locally.md): run the tests (141 should pass), start both servers, open a sample, generate a plan.
- [ ] Handover call with Bhavya before Sunday 8 November: go through this folder and ask anything unclear.

## Your algorithms (each needs Javadoc with purpose, time and space complexity, plus at least 5 tests including an edge case)

| # | Algorithm | Package | Used by | Done when |
|---|---|---|---|---|
| 1 | Disjoint-set union (union by rank, path compression, cluster stats: size, cases, vaccinated) | `engine/dsu` | Features 2 and 12 | Cluster sizes match a BFS reference on random graphs; union of an already-joined pair is a no-op |
| 2 | Indexed binary max-heap with `decreaseKey` | `engine/heap` | Greedy method, feature B | Extract order matches a sorted reference on a long random sequence; extract from empty throws |
| 3 | Greedy vertex cover ("most connected first") | `engine/cover` | Feature 5 | Implements `VertexCoverSolver`; valid cover on random graphs, including with exclusions; star → 1; on `greedy-worst-case-12` it picks 23 against a minimum of 12 |
| 4 | Own hash functions (FNV-1a, MurmurHash3) and Bloom filter (double hashing, theoretical rate, optimal k) | `engine/bloom` | Features 6, 9, 10 | No false negatives; measured false positive rate within 20% of theory over 100,000 queries; invalid m or k rejected |
| 5 | Outbreak simulator (seeded SIR, transmission probability, recovery steps, efficacy, N trials; mean, median and 90th percentile via `RandomizedQuickselect`) | `engine/sim` | Feature 11 | The same seed gives the same result; a plan reduces mean infections against random vaccination with the same doses |

## Your backend features (each needs API tests in the style of `ApiTest`: success, 400, 404)

| Feature | Endpoint (suggested) | Done when |
|---|---|---|
| 2 Clusters | `GET /api/networks/{id}/clusters` | Lists clusters with size, cases and vaccinated count |
| 12 Live contact | `POST /api/networks/{id}/contacts` `{personA, personB, date?, durationMinutes?}` | Adds the link to the stored graph, merges clusters, rejects self-contacts, unknown people and duplicates |
| 5 Greedy in compare | (none: register the solver) | `/api/methods` lists `GREEDY`; compare shows three rows |
| 6 Exposure check | `POST /api/exposure/check` `{networkId, checkins: [{venueId, start, end}]}` | Returns "possibly exposed" from the Bloom filter, then the exact confirmed result; reveals venue and date only, never who the case was |
| 9 Double-dose guard | `POST /api/registry/check` `{networkId, personIds}` | Bloom hit, then exact confirmation; already-vaccinated people are flagged |
| 10 Bloom tuning | `GET /api/bloom/stats`, `POST /api/bloom/config` `{m, k}` | Measured and theoretical false positive rate, memory against `HashSet`; m or k of 0 or absurd values → 400 |
| 11 Simulation | `POST /api/networks/{id}/simulate` `{caseIds, depth?, p, recoverySteps, trials, seed, efficacy?}` | Infections under approx, greedy, random (same doses) and no vaccination |
| E Contact settings | Add `minDurationMinutes` and `windowDays` to the ring, plan and compare requests | The ring builder gets a filter; the response says how many contacts were excluded and why |
| B Deadlines | Add to the plan response, or `POST .../follow-up` | Each dose has a vaccinate-by date (last exposure + 72 h default); ordered with the heap; people past the window are flagged |
| C Monitoring | Same endpoint as B | Ring members not in the plan (and people past the window), each with a monitoring end date (last exposure + 21 days default) |

## Your screens (in the existing design; see the design system in [06](06-codebase-guide.md))

These are already in the "All tools" launcher as placeholders. Set `ready: true` in `frontend/src/lib/tools.js` and add the route in `App.jsx` as each one is built.

| Screen | Path | Must show |
|---|---|---|
| Plan details | `/plan` | Sortable, searchable table of doses with reasons (D), guarantee, **budget slider** (backend done: `budget` field), unprotected links, unprotectable links, ring 1 left out, method switch including Greedy |
| Compare | `/compare` | The three methods side by side: doses, time, ratio to exact, guarantee; the exact refusal message on big rings |
| Clusters | `/clusters` | Cluster list; click to highlight on the network; form to add a live contact |
| Deadlines and monitoring | `/follow-up` | B and C lists; contact definition settings (E) |
| Exposure self-check | `/exposure` | Simple, phone-friendly public page; "possibly exposed", then the confirmed result; privacy-safe |
| Bloom filter tuning | `/bloom` | m and k sliders; live false positive rate and memory against a hash set |
| Simulation | `/simulation` | Infections under each plan; headline "infections reduced by X%" |
| Benchmarks | `/benchmarks` | Charts (Chart.js) from all experiments, Bhavya's and yours |

The network console (`/network`) plays the role of the "dashboard" mentioned in the original work split. It is done.

## Your benchmarks (add to `BenchmarkRunner`, results into `docs/benchmarks`)

1. Approximation against greedy run time at about 1K, 10K, 100K and 1M contacts (`SyntheticNetworkGenerator`, `CoverProblem.allContacts`).
2. Disjoint-set union updates against recomputing with BFS after every insert (10K, 100K, 1M insertions).
3. Bloom filter against `HashSet` (10K, 100K, 1M keys): memory and measured against theoretical false positive rate.

Then add your tables to `docs/benchmarks/README.md` in the same style.

## Finishing (from 16 November)

- [ ] Convert one SocioPatterns contact network into `data/sociopatterns/`, with its licence and citation (in the README too).
- [ ] Walk through every edge case in plan section 5 in the app, and save screenshots to `docs/screenshots/`.
- [ ] Final README: screenshots, dataset attribution, full API examples, fresh-clone test.
- [ ] Rehearse the 8 minute demo twice (script in plan section 14), including an examiner changing the input.
- [ ] Viva prep for your algorithms: the Bloom filter false positive formula and optimal k, why there are no false negatives, heap decrease-key, why greedy has no constant guarantee, the disjoint-set union bound, and the simulation.
- [ ] Report: on hold for now; both write their sections later.

## Suggested day plan

| Day | Work |
|---|---|
| Mon 9 | Set up, disjoint-set union, clusters and live contact endpoints |
| Tue 10 | Heap, greedy, register GREEDY (compare gets its third row) |
| Wed 11 | Hash functions, Bloom filter, exposure check |
| Thu 12 | Double-dose guard, Bloom tuning endpoints, simulator |
| Fri 13 | Simulate endpoint, B, C and E, API tests |
| Sat 14 | Screens: plan details, compare |
| Sun 15 | Screens: clusters, follow-up, exposure |
| Mon 16 | Screens: Bloom tuning, simulation, benchmarks |
| Tue 17 | Your benchmarks, SocioPatterns data, edge-case screenshots |
| Wed 18 | Polish, bug fixes, README |
| Thu 19 | Fresh-clone test, demo rehearsal twice |
| Fri 20 | Submit, demo, viva |
