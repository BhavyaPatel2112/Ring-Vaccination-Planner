# 2. Full scope: everything the finished project needs

The status column is accurate as of 4 November.
- ✅ done
- 🟡 backend done, screen still to build
- ⬜ to do

The owner is who builds it.

## Main features (12)

| # | Feature | What it does | Algorithm | Owner | Status |
|---|---|---|---|---|---|
| 1 | Network upload and validation | Upload CSVs; duplicates, self-contacts, unknown people and empty files are reported with line numbers, never silently dropped | Parser | Bhavya | ✅ |
| 2 | Outbreak clusters | Connected groups with size, cases and vaccinated count; click to highlight | Disjoint-set union | Tanushree | ⬜ |
| 3 | Ring builder | Mark cases; ring 1 and ring 2 in colour; depth 1 to 3 | Breadth-first search | Bhavya | ✅ |
| 4 | Vaccination plan | Who to vaccinate, dose count, the "at most 2 times the minimum" guarantee | 2-approximation | Bhavya | ✅ (summary on the network screen; full details screen 🟡) |
| 5 | Method comparison | Approximation, greedy and exact side by side: doses, time, ratio to exact; exact refuses large rings | All three solvers | Shared | 🟡 endpoint done with approx and exact; greedy and screen to do |
| 6 | Exposure self-check | Public enters venue visits; "possibly exposed" (Bloom filter), then the confirmed answer (exact check) | Bloom filter | Tanushree | ⬜ |
| 7 | Benchmark and proof page | Run time charts at 3 or more sizes, ratio against exact, Bloom filter measured against theoretical rate | All | Shared | 🟡 Bhavya's 3 experiments done; Tanushree's experiments and the page to do |
| 8 | Budget mode | Fewer doses than needed: ring 1 first, then the top 10% most connected; lists unprotected links | Quickselect | Bhavya | 🟡 backend done (`budget` in the plan request); controls go on the plan details screen |
| 9 | Double-dose guard | Already-vaccinated people are skipped and shown as such | Second Bloom filter plus exact registry | Tanushree | ⬜ |
| 10 | Bloom filter tuning panel | Sliders for m and k; live false positive rate and memory against an exact hash set | Bloom filter | Tanushree | ⬜ |
| 11 | Outbreak simulation | Infections under each plan with the same doses: "infections reduced by X%" | Simulation, Quickselect | Tanushree | ⬜ |
| 12 | Live contact updates | Add a contact report; clusters merge instantly; plan can be recomputed | Disjoint-set union | Tanushree | ⬜ |

## Field features (small, real-world, 3 each)

| | Feature | Owner | Status |
|---|---|---|---|
| A | Can't-vaccinate handling: ineligible or refused people are never chosen, the other person on their links is forced, and links where both are excluded are listed for quarantine | Bhavya | ✅ in the algorithms and API (from the CSV `eligible` and `refused` columns). Marking a refusal from the screen still needs a small endpoint ([07](07-where-your-code-plugs-in.md)) |
| D | "Why this person?": every dose has a reason | Bhavya | ✅ (shown in the person card on the map) |
| G | Contact tracing quality warnings: cases with no contacts, outliers above the 99th percentile, contacts dated in the future or after confirmation | Bhavya | ✅ (the "Checks" tab, and the load report) |
| B | "Vaccinate by" deadline: last exposure plus the post-exposure window (default 72 hours), ordered by urgency with the heap | Tanushree | ⬜ |
| C | Symptom monitoring list: ring members not in the plan, plus people past the window, each with an end date (default 21 days) | Tanushree | ⬜ |
| E | Contact definition settings: minimum duration (default 15 minutes) and infectious window filter contacts before the rings are built | Tanushree | ⬜ (the ring builder already accepts a filter) |

## Algorithms and who owns them (for the viva)

| Bhavya | Tanushree |
|---|---|
| Vertex cover 2-approximation (Unit 5): headline algorithm, with proof | Bloom filter with own FNV-1a and MurmurHash3 (Unit 6, CO5): headline algorithm, false positive formula |
| Exact vertex cover, branch and bound, plus NP-hardness (Unit 6) | Indexed binary max-heap with decrease-key, plus greedy vertex cover (Unit 2) |
| Randomized Quickselect (Unit 6, CO5) | Disjoint-set union with union by rank and path compression (Unit 2) |
| Median-of-medians (Unit 1) | Outbreak simulator (seeded SIR model) |

## Deliverables (from the assessment guidelines)

| | Deliverable | Status |
|---|---|---|
| A | Project proposal (1 to 2 pages) | On hold until the faculty asks |
| B | Source code repository: Java backend, frontend, tests, sample data, build files, readable commit history with attribution | In progress (each person commits their own work) |
| C | Technical report (5 to 8 pages) | On hold for now; both write their own sections later |
| D | Demonstration evidence: working app, example inputs and outputs, error handling, performance charts | Partly: the app works, and the first benchmark results are in `docs/benchmarks` |
| E | README and reproducibility | Partly: run commands and API table are in the root README; screenshots and attribution still to add |

## Rules that cost marks if broken

1. Every assessed algorithm is written by us in Java. No library implementations (no Guava BloomFilter, no JGraphT and so on). Java collections are fine as support.
2. Java runs the algorithms. The frontend only collects input and shows results.
3. Never call the approximation optimal: state "at most 2 times the minimum" and the measured ratio.
4. A Bloom filter hit is always "possibly present" until an exact check confirms it.
5. Every random process takes a seed.
6. Each algorithm class has Javadoc with its purpose, time complexity and space complexity.
7. At least 5 JUnit tests per core algorithm, including at least one edge case.
8. Each person commits their own work from their own account (the rubric grades contribution).

## Rubric (100 marks)

| Criterion | Marks |
|---|---|
| Real-world problem formulation | 10 |
| Algorithm selection and original implementation | 20 |
| Algorithm correctness and test coverage | 20 |
| Complexity and empirical performance | 15 |
| Java backend and API quality | 10 |
| Frontend integration and usability | 5 |
| Documentation and reproducibility | 5 |
| Live demo and individual viva | 15 |

## Key dates

| Date | What |
|---|---|
| Up to Sun 8 Nov | Bhavya available (handover call before he leaves) |
| Mon 9 to Thu 19 Nov | Tanushree builds her half, then benchmarks, data, README and demo practice |
| Fri 20 Nov | Final submission, live demo (about 8 minutes) and individual viva (about 2 minutes each) |

Faculty checkpoints (design review, prototype review) after 8 November are presented by Tanushree. The app already has a working end-to-end flow: load, mark cases, rings, plan.
