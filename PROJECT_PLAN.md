# Ring Vaccination Planner: Full Project Plan

Advanced Algorithm, Continuous Internal Assessment 3 (CIA 3), Semester V, B.Tech CS AI & ML, ATLAS SkillTech University, 2026 to 27.

- **Team:** Bhavya Patel and Tanushree
- **Deadline:** 20 November 2026 (final repository, report, live demo and individual viva)
- **Goal:** Full marks (100 / 100)
- **Source of requirements:** `Information.pdf` in this folder (CIA 3 student project guidelines). Re-read it before any major decision.

---

## 0. Ground rules (read first)

These rules come straight from the assessment guidelines. Breaking any of them costs marks.

1. **Every assessed algorithm is written by us, in Java, from scratch.** Do NOT use library implementations of the assessed algorithms (no JGraphT, no Guava `BloomFilter`, no Apache Commons graph or selection utilities). Java Collections (`ArrayList`, `HashMap`, `ArrayDeque`) are allowed only as supporting structures.
2. **Java executes the algorithms on the server.** The frontend only collects input and displays results. No algorithm logic in JavaScript, and no precomputed results shipped to the browser.
3. **Approximation honesty.** Never claim the 2-approximation is optimal. Always state the guarantee ("at most 2 times the minimum") and the measured ratio.
4. **Bloom filter honesty.** A Bloom filter hit is always "possibly present". Every hit must be confirmed by an exact, authoritative check before it is shown as a final result.
5. **Reproducibility.** Every random process (Quickselect pivots, simulation, synthetic data) takes a seed parameter so tests and benchmarks are repeatable.
6. **Each algorithm class** has Javadoc stating its purpose, time complexity and space complexity.
7. **Tests:** at least 5 JUnit tests per core algorithm, including at least one boundary or edge test.
8. **Commits:** small, meaningful commit messages. Bhavya and Tanushree each commit their own work from their own machines (the rubric grades contribution attribution).
9. **Environment:** Java 17 (Homebrew OpenJDK 17.0.18 on Bhavya's Mac), Maven wrapper, Spring Boot (default stable version on start.spring.io that supports Java 17), Node.js 26 and npm 11, React with Vite.

---

## 1. The problem

### Real-world scenario

When an outbreak starts (measles, mpox, Ebola), public health teams know the confirmed cases and the people those cases have been in contact with. Vaccine doses and vaccinator time are limited. **Ring vaccination** vaccinates a protective "ring" of people around each case so the disease has nowhere to spread. It was central to smallpox eradication and was used in the 2015 Ebola ring vaccination trial in Guinea.

### Target users

- **Primary:** District outbreak response teams and contact tracers who must decide who to vaccinate first.
- **Secondary:** Members of the public who want to check whether they were possibly exposed at a venue.

### The operational decision the app improves

> Given a contact network and the confirmed cases, who exactly should be vaccinated so that every risky contact link is protected, using as few doses as possible?

### Key algorithmic insight

Blocking every contact link means choosing people so that **every link has at least one vaccinated endpoint**. That is the **minimum vertex cover** problem, which is NP-hard. The app uses a provable 2-approximation, compares it with a greedy heuristic, and proves its quality against an exact solver on small networks.

### Modelling assumptions (state these in the proposal and report)

1. A contact link is a possible transmission route. Vaccinating at least one person on a link blocks transmission along it.
2. Confirmed cases are isolated, so they do not need vaccination; their links are handled by vaccinating their direct contacts.
3. People already vaccinated are immune; links touching them are already covered.
4. Ring depth defaults to 2 (contacts, and contacts of contacts) and is configurable.
5. Vaccination is assumed fully protective (simplification; the simulation feature can relax this with an efficacy parameter).

### Measurable success criteria

| Criterion | Target |
|---|---|
| Approximation quality | Always at most 2 times the exact minimum on every exact-solvable test network; report the average measured ratio |
| Approximation speed | Plan for a network of 1,000,000 contacts in under 2 seconds on a laptop (verify and report actual) |
| Exact solver scaling | Show exponential run time growth on networks of 10 to 40 people |
| Bloom filter accuracy | Measured false positive rate within 20% (relative) of the theoretical rate at the configured load |
| Bloom filter memory | At least 5 times less memory than an exact hash set of the same keys (verify and report actual) |
| Final correctness | Zero false positives in final exposure results (every Bloom hit confirmed exactly) |
| Outbreak impact | Plan reduces mean simulated infections compared with random vaccination using the same number of doses |

---

## 2. Syllabus coverage

| Technique | Syllabus unit | Role in the app |
|---|---|---|
| Vertex cover 2-approximation | Unit 5 (network / approximation) | Core vaccination plan |
| Exact vertex cover (bounded search tree) and NP-hardness analysis | Unit 6 (randomization / complexity) | Ground truth on small inputs, tractability study |
| Bloom filter | Unit 6 (Course Outcome 5, CO5) | Venue exposure checks and the "already vaccinated" registry |
| Randomized Quickselect | Unit 6 (CO5) | Degree percentiles for budget mode, simulation percentiles |
| Median-of-medians | Unit 1 (sorting / selection) | Deterministic baseline for Quickselect |
| Disjoint-set union | Unit 2 (priority / DSU) | Outbreak clusters, updated live as contacts arrive |
| Binary heap with decrease-key | Unit 2 | Greedy "most connected first" baseline |
| Stretch: max flow, metric TSP approximation, segment tree, trie | Units 5, 5, 3, 4 | Optional extra features (section 4.3) |

This satisfies: at least two distinct techniques, at least one from Units 4 to 6, and a meaningful CO5 component.

---

## 3. Algorithms in detail

Notation: V = number of people, E = number of contact links, k = size of the cover.

### 3.1 Ring builder (support code, not assessed)

- Breadth-first search from all confirmed cases up to the configured depth.
- **Ring 1** = direct contacts of cases. All susceptible ring 1 members are vaccinated (mandatory, as in real ring vaccination).
- **Ring 2** = contacts of ring 1 members not in ring 1 or cases.
- **Target links** = links with at least one endpoint in ring 2, where neither endpoint is a case, already vaccinated, or in ring 1.
- The vertex cover algorithms run on the target links only.
- Complexity: O(V + E).

### 3.2 Vertex cover 2-approximation (maximal matching)

```
cover = empty set
for each link (u, v) in target links:
    if u not in cover and v not in cover:
        add u and v to cover
return cover
```

- **Proof of the 2 factor (viva):** The links that triggered additions form a matching M (no two share a person). Any valid cover, including the optimum, must contain at least one endpoint of every link in M, so OPT >= |M|. The algorithm returns exactly 2|M| people, so the result is at most 2 OPT.
- **Tight examples:** a star network (approximation 2, optimum 1) and a complete bipartite network K(n, n) (approximation 2n, optimum n).
- Time O(V + E), space O(V).

### 3.3 Greedy "most connected first" (baseline)

- Repeatedly pick the person with the most uncovered links, add them to the cover, and decrease their neighbours' uncovered-link counts.
- Implemented with our own **indexed binary max-heap** supporting `decreaseKey`.
- No constant guarantee: on specially built layered bipartite networks the ratio grows roughly like ln(n). Include a generator for such a network as a test and a demo case.
- Time O((V + E) log V), space O(V).

### 3.4 Exact vertex cover (bounded search tree with branch and bound)

```
solve(graph, partialCover):
    if no uncovered links: update best if smaller; return
    if |partialCover| + lowerBound(graph) >= |best|: prune; return
    apply reduction rules (a degree 1 person: take their neighbour)
    pick an uncovered link (u, v)
    solve(graph with u removed, partialCover + u)
    solve(graph with v removed, partialCover + v)
```

- Initial upper bound = the 2-approximation result. Lower bound = size of a maximal matching on the remaining graph.
- Worst case O(2^k (V + E)). Refuse politely (HTTP 422) above a configurable limit (default 40 people in the target subgraph) or after a timeout.
- Cross-check against brute force over all subsets for networks of up to about 15 people in tests.
- **NP-hardness (report and viva):** Vertex cover is NP-complete (one of Karp's 21 problems). Sketch: a graph has an independent set of size s exactly when it has a vertex cover of size V minus s, and independent set is NP-complete (by reduction from 3-SAT / clique).

### 3.5 Disjoint-set union (outbreak clusters)

- Union by rank plus path compression. Each new contact link calls `union(a, b)`.
- Cluster statistics: size, number of cases, number vaccinated.
- Amortized O(alpha(V)) per operation (nearly constant), space O(V).
- Baseline: recomputing connected components with breadth-first search after every new contact, which is O(V + E) per update.

### 3.6 Bloom filter (CO5)

- Bit array of m bits, k hash functions using double hashing: h_i(x) = h1(x) + i * h2(x) mod m, with h1 and h2 our own implementations (for example FNV-1a and MurmurHash3, written by us).
- **Use 1, exposure check-ins:** key = venueId + hour bucket. Every hour a confirmed case spent at a venue during the infectious window is inserted. A person's check-ins are queried; "possibly exposed" is confirmed against the exact check-in table.
- **Use 2, vaccination registry:** key = personId. Prevents double doses; hits are confirmed against the exact registry.
- Theoretical false positive rate: p = (1 - e^(-k n / m))^k. Optimal k = (m / n) ln 2.
- Insert and query O(k) time; space m bits.
- Baseline: exact `HashSet<String>` (memory and time).

### 3.7 Randomized Quickselect (CO5) and median-of-medians baseline

- Random pivot, partition, recurse into one side. Expected O(n), worst case O(n^2), space O(1) extra (in place).
- **Use 1:** find the 90th-percentile uncovered-link count, so budget mode prioritizes the top 10% most connected people.
- **Use 2:** median and 90th-percentile infections across simulation runs, and median run times in benchmarks.
- Baselines: median-of-medians (worst case O(n)) and full sort (O(n log n)).

### 3.8 Outbreak simulation (evaluation tool)

- Discrete-time susceptible, infected, recovered model on the network. Each step, each infected person infects each susceptible contact with probability p; infected people recover after r steps. Vaccinated people are immune (or protected with an efficacy parameter).
- Run N trials with a seed. Compare plans: approximation, greedy, random vaccination with the same doses, and no vaccination.
- Output mean, median and 90th-percentile total infections.

---

## 4. Features

### 4.1 Core features (must build)

| # | Feature | Algorithms | Acceptance criteria |
|---|---|---|---|
| 1 | **Network upload and validation** | Parser (support) | Upload CSV files (format in section 6). Duplicates, self-contacts, unknown people and empty files are reported clearly, never silently ignored. Interactive network picture. |
| 2 | **Outbreak cluster view** | Disjoint-set union | Lists clusters with size, cases and vaccinated count. Clicking a cluster highlights it in the network. |
| 3 | **Ring builder** | Breadth-first search (support) | Mark confirmed cases; ring 1 and ring 2 shown in distinct colours; depth configurable. |
| 4 | **Vaccination plan** | Vertex cover 2-approximation | Returns the list of people to vaccinate, dose count, covered links, and the "at most 2 times the minimum" guarantee on screen. |
| 5 | **Method comparison** | Approximation, greedy, exact | Same network, side by side: doses, run time, ratio to exact when available. Exact refuses networks above the limit with a clear message. |
| 6 | **Exposure self-check** | Bloom filter plus exact check | Person enters venue visits; result is "not exposed" or "possibly exposed", then the confirmed exact result. |
| 7 | **Benchmark and proof page** | All | Run time charts at 3 or more sizes, approximation ratio versus exact, Bloom filter measured versus theoretical false positive rate. |

### 4.2 Strong additions (build after core)

| # | Feature | Algorithms | Acceptance criteria |
|---|---|---|---|
| 8 | **Budget mode** | Randomized Quickselect, approximation | With fewer doses than needed: ring 1 first, then top 10% most connected; lists links left unprotected. Budget 0 and budget larger than needed both handled. |
| 9 | **Double-dose guard** | Second Bloom filter plus exact registry | Already-vaccinated people are skipped and shown as such. |
| 10 | **Bloom filter tuning panel** | Bloom filter | Sliders for m and k; live false positive rate, memory, and comparison with an exact hash set. |
| 11 | **Outbreak simulation** | Simulation, Quickselect | Infections under each plan with the same doses; headline "infections reduced by X%". |
| 12 | **Live contact updates** | Disjoint-set union | Add a contact report; clusters merge instantly and the plan can be recomputed. |

### 4.3 Stretch features (only if ahead of schedule; pick at most one)

| # | Feature | Algorithm | Unit |
|---|---|---|---|
| 13 | Priority-weighted plan (elderly, health workers) | Weighted vertex cover 2-approximation (pricing method) | 5 |
| 14 | Vaccinator team allocation by daily capacity | Max flow (Edmonds-Karp) | 5 |
| 15 | Door-to-door team route | Metric TSP 2-approximation via minimum spanning tree | 5 |
| 16 | Epidemic curve range queries | Segment tree | 3 |
| 17 | Person search autocomplete | Trie | 4 |

### 4.4 Field features (must build; small, practical additions for real outbreak teams)

| # | Feature | Algorithms | Acceptance criteria |
|---|---|---|---|
| A | **Can't-vaccinate handling** | 2-approximation, exact (with exclusions) | People marked ineligible (infant, pregnant, immunocompromised for live vaccines) or refused are never selected. On a link with one excluded endpoint, the other endpoint is forced. Links with both endpoints excluded are listed as "cannot be protected by vaccination: quarantine or monitor". The guarantee is stated relative to the constrained optimum. |
| B | **"Vaccinate by" deadline** | Indexed heap | Deadline = last exposure + post-exposure window (default 72 hours, configurable). The plan is ordered by urgency. People past the window are flagged "monitor instead". |
| C | **Symptom monitoring list** | Ring builder output | Ring members not in the plan and not vaccinated, plus people past the window, each with an end date (last exposure + incubation period, default 21 days). |
| D | **"Why this person?" explanation** | 2-approximation, greedy, budget mode | Every person in a plan has a human-readable reason. |
| E | **Contact definition settings** | ContactFilter (support) | Minimum duration (default 15 minutes) and infectious window filter contacts before ring building; the count excluded and the reasons are shown. Invalid values are a 400 error. |
| G | **Contact tracing quality warnings** | Randomized Quickselect | Flags cases with zero contacts, people above the 99th percentile of contacts, and contacts dated in the future or after confirmation. These are warnings, not errors. |

Frontend: a proper, consistent interface is in scope (design tokens, guided workflow, dashboard, public mobile-friendly self-check page, designed loading, empty and error states). See `WORK_SPLIT.md` section 4.

---

## 5. Validation and edge cases

The app must handle every one of these without returning silently wrong results.

| Case | Expected behaviour |
|---|---|
| Empty file or no people | 400 error with a clear message |
| People but no contacts | Plan is empty; message "no links to protect" |
| Duplicate contacts (A to B twice, or A to B and B to A) | Merged into one link; warning lists duplicates |
| Self-contact (A to A) | Rejected with a warning |
| Contact mentions an unknown person | Rejected row with line number |
| Confirmed case with no contacts | Ring is empty; reported, not an error |
| Everyone in the ring already vaccinated | Plan is empty; message explains why |
| Unknown case identifier | 400 error naming the identifier |
| Budget 0 / negative / larger than needed | 0 gives empty plan with all links listed as unprotected; negative is a 400 error; larger returns the full plan |
| Exact solver on too large a network | 422 error suggesting the approximation |
| Bloom filter parameters m or k of 0 or absurd values | 400 error with allowed ranges |
| Star network, complete bipartite network, greedy worst case | Used as demo and test cases for approximation behaviour |
| Very large network sent to the browser | Backend returns a sampled view for display; algorithms still run on the full network |

---

## 6. Data

### Real data

- **SocioPatterns** (sociopatterns.org): real face-to-face contact networks recorded with wearable sensors, for example the French high school network and the hospital ward network. Convert to our CSV format with a script in `data/`. Record the licence and citation in the README.

### Synthetic data generator (Java, seeded)

- Households: groups of 2 to 6 people, fully connected.
- Schools and workplaces: groups of 20 to 200, each person linked to a random subset.
- Community: random links between groups.
- Parameters: population size, average contacts, seed.
- Sizes for benchmarks: roughly 1,000, 10,000, 100,000 and 1,000,000 contacts.

### CSV formats

`people.csv`
```
person_id,name,age,role,vaccinated
P001,Asha,34,teacher,false
```

`contacts.csv`
```
person_a,person_b,date,duration_minutes
P001,P002,2026-11-01,45
```

`checkins.csv`
```
person_id,venue_id,start_time,end_time
P001,V010,2026-11-01T09:00,2026-11-01T11:30
```

`cases.csv` (optional; cases can also be marked in the user interface)
```
person_id,confirmed_date
P002,2026-11-02
```

---

## 7. Architecture

| Layer | Responsibility | Choice |
|---|---|---|
| Frontend | Forms, uploads, network picture, charts, results | React (Vite), Cytoscape.js for the network, Chart.js for charts |
| Java application programming interface (API) | Validation, services, Representational State Transfer (REST) endpoints, error handling | Spring Boot (Spring Web, Validation) |
| Algorithm engine | Our own algorithm classes, JUnit tests, benchmarks | Plain Java, JUnit 5, Java Microbenchmark Harness (JMH) optional |
| Persistence (optional) | Uploaded networks, benchmark logs | H2 file database (embedded, nothing to install) |

### Repository layout

```
ring-vaccination-planner/
  README.md
  PROJECT_PLAN.md
  backend/
    mvnw, pom.xml
    src/main/java/com/ringvax/
      api/            controllers, request and response objects, global exception handler
      service/        NetworkService, PlanService, ExposureService, BenchmarkService
      engine/
        graph/        ContactGraph (adjacency lists, people index)
        ring/         RingBuilder
        cover/        ApproxVertexCover, GreedyVertexCover, ExactVertexCover, (WeightedVertexCover)
        heap/         IndexedMaxHeap
        dsu/          DisjointSetUnion
        bloom/        BloomFilter, HashFunctions
        select/       RandomizedQuickselect, MedianOfMedians
        sim/          OutbreakSimulator
      data/           CsvParser, SyntheticNetworkGenerator
    src/test/java/com/ringvax/...   one test class per algorithm, plus API tests
  frontend/
    src/pages/        Upload, Clusters, Plan, Compare, Exposure, Simulation, Benchmarks
    src/api/          client functions calling the backend
  data/
    sample/           small hand-made CSVs for the demo and tests
    sociopatterns/    converted real network(s) plus licence note
  docs/
    proposal.md
    report/           technical report sources, diagrams, pseudocode
    benchmarks/       raw benchmark CSV results and charts
    screenshots/
```

### API contract (draft)

All endpoints under `/api`. Errors return `{ "error": "...", "message": "...", "details": [...] }` with status 400 (invalid input), 404 (unknown network), 422 (valid but not solvable as asked, for example exact solver too large).

| Method and path | Purpose | Body / response summary |
|---|---|---|
| `POST /api/networks` | Upload CSVs | multipart files; returns `networkId`, counts, warnings |
| `POST /api/networks/generate` | Synthetic network | `{ population, avgContacts, seed }`; returns `networkId` |
| `GET /api/networks/{id}` | Network for display | nodes and links (sampled if large) |
| `GET /api/networks/{id}/clusters` | Outbreak clusters | list of clusters with stats |
| `POST /api/networks/{id}/contacts` | Live contact update | `{ personA, personB }`; returns updated cluster info |
| `POST /api/networks/{id}/ring` | Build ring | `{ caseIds, depth }`; returns ring 1, ring 2, target links |
| `POST /api/networks/{id}/plan` | Vaccination plan | `{ caseIds, method: APPROX or GREEDY or EXACT, budget? }`; returns people, doses, uncovered links, guarantee text, run time |
| `POST /api/networks/{id}/compare` | All methods side by side | `{ caseIds }` |
| `POST /api/networks/{id}/simulate` | Outbreak simulation | `{ caseIds, plans, p, recoverySteps, trials, seed }` |
| `POST /api/exposure/check` | Exposure self-check | `{ networkId, checkins }`; returns Bloom result and confirmed result |
| `POST /api/registry/check` | Double-dose guard | `{ personIds }` |
| `GET /api/bloom/stats`, `POST /api/bloom/config` | Tuning panel | `{ m, k }`; returns measured and theoretical false positive rate, memory |
| `POST /api/benchmarks/run` | Benchmarks | `{ algorithm, sizes, repeats, seed }`; returns timings |

---

## 8. Testing plan (at least 5 per algorithm, including an edge case)

| Algorithm | Tests |
|---|---|
| ApproxVertexCover | empty graph; single link (2 people); star network (returns 2, optimum 1); complete bipartite K(3,3); result is always a valid cover on 100 random seeded graphs; result at most 2 times exact on small random graphs |
| GreedyVertexCover | empty graph; star network (returns 1); valid cover on random graphs; greedy worst-case layered network shows ratio above approximation's; heap decrease-key correctness |
| ExactVertexCover | empty graph; triangle (2); path of 4 people (2); matches brute force on 50 random graphs up to 15 people; refuses above limit |
| IndexedMaxHeap | insert and extract order; decrease-key moves element; extract from empty throws; duplicate keys; large random sequence matches a sorted reference |
| DisjointSetUnion | single element; union then find; union of already-joined sets is a no-op; chain of unions gives one cluster; cluster sizes correct on random graph versus breadth-first search reference |
| BloomFilter | no false negatives for all inserted keys; empty filter returns false; measured false positive rate near theory over 100,000 random queries; invalid m or k rejected; every hit confirmed by exact check in the service |
| RandomizedQuickselect | single element; k = 0 and k = n minus 1; all equal values; matches sorted array on random seeded inputs; invalid k throws |
| MedianOfMedians | same cases as Quickselect, plus agreement with Quickselect |
| RingBuilder | case with no contacts; depth 1 versus 2; already vaccinated excluded; unknown case identifier rejected; overlapping rings from two cases merged correctly |
| CsvParser | valid file; empty file; duplicate rows; self-contact; unknown person; bad header |
| API (Spring MockMvc) | each endpoint's success path and its 400 / 404 / 422 paths |

---

## 9. Benchmark plan (15 marks for complexity and performance)

| Experiment | Sizes | Compared against | Expected shape |
|---|---|---|---|
| Approximation run time | about 1k, 10k, 100k, 1M contacts | Greedy | Linear versus n log n |
| Approximation quality | 10 to 40 people | Exact solver | Ratio at most 2, usually 1.2 to 1.6 (report actual) |
| Exact solver run time | 10, 20, 30, 40 people | Approximation | Exponential versus linear |
| Disjoint-set union live updates | 10k, 100k, 1M contact insertions | Breadth-first search recompute per insert | Near constant versus linear per update |
| Bloom filter | 10k, 100k, 1M keys | Exact hash set | Memory far smaller; measured false positive rate matches the formula |
| Quickselect | 10k, 100k, 1M values | Median-of-medians, full sort | Expected linear; median-of-medians has a larger constant |

Method: warm up the Java virtual machine, repeat each measurement 5 or more times, report the median, fix seeds, record raw results as CSV in `docs/benchmarks/`, plot in the benchmark page and the report. Java Microbenchmark Harness is optional for more rigour.

---

## 10. Deliverables checklist

- [ ] **A. Project proposal (1 to 2 pages):** title, team names and identifiers, real-world problem, target user, proposed features, the core techniques and why each fits, data source, measurable success criteria (section 1).
- [ ] **B. Source code repository:** Java backend, frontend, tests, sample data, build files, dependency manifest, readable commit history with contribution attribution.
- [ ] **C. Technical report (5 to 8 pages):** problem statement and constraints; system design and API diagram; algorithm pseudocode; correctness discussion (2-approximation proof, NP-hardness, Bloom filter rate); time and space complexity; benchmark method and results; limitations; individual contributions.
- [ ] **D. Demonstration evidence:** working app, example inputs and outputs, error-handling evidence, performance charts and tables (slides optional).
- [ ] **E. README and reproducibility:** prerequisites, build and run commands for backend and frontend, environment variables (no secrets), API examples, representative test dataset, screenshots, dataset attribution.

---

## 11. Rubric mapping (100 marks)

| Criterion | Marks | How this project earns it |
|---|---|---|
| Real-world problem formulation | 10 | Real public health strategy, clear users, stated constraints and assumptions, measurable criteria (section 1) |
| Algorithm selection and original implementation | 20 | Vertex cover approximation (Unit 5), exact solver and NP-hardness, Bloom filter and Quickselect (Unit 6, CO5), disjoint-set union and heap (Unit 2), all self-implemented |
| Algorithm correctness and test coverage | 20 | Proofs, exact solver as ground truth, tight and worst-case inputs, edge cases, Bloom filter rate matches theory (sections 3, 5, 8) |
| Complexity and empirical performance | 15 | Complexity per algorithm, baselines, scaled reproducible experiments (section 9) |
| Java backend and API quality | 10 | Layered packages, validation, global error handling, clean REST contract (section 7) |
| Frontend integration and usability | 5 | Full upload, configure, run, interpret workflow with clear feedback |
| Documentation and reproducibility | 5 | README, sample data, attribution, organized repository |
| Live demo and individual viva | 15 | Rehearsed 8 minute demo; each student owns and can explain specific algorithms (section 13) |

---

## 12. Timeline (3 November to 20 November 2026)

Confirm actual review dates with the faculty; the guidelines expect proposal approval before building.

| Dates | Milestone | Done when |
|---|---|---|
| 3 to 4 Nov | Proposal and setup | Proposal submitted to faculty; repository created; both laptops run the starter backend and frontend |
| 5 to 6 Nov | Design review | Data model, API contract, pseudocode finalized; CSV parser and synthetic generator working |
| 7 to 10 Nov | Algorithm engine | All core algorithm classes with their tests passing |
| 10 to 12 Nov | Backend API | Core endpoints with validation and error handling; first end-to-end feature (upload, plan, view) |
| 12 to 15 Nov | Frontend and strong additions | Features 1 to 7 complete in the UI; features 8 to 12 added |
| 15 to 17 Nov | Validation and benchmarking | Benchmarks run, charts produced, edge cases demonstrated, SocioPatterns data integrated |
| 17 to 19 Nov | Report, README, rehearsal | Report written, README reproducible from a fresh clone, demo rehearsed twice with examiner-style changes |
| 20 Nov | Final submission and demo | Everything submitted; live demo and viva |

---

## 13. Suggested work split (adjust together)

> **Superseded by `WORK_SPLIT.md`** (Bhavya 3 to 8 Nov: 2-approximation, exact solver, Quickselect, median-of-medians; Tanushree 9 to 20 Nov: Bloom filter, heap and greedy, DSU, simulation). The table below is the original suggestion, kept for reference.

Both members must be able to explain their contribution, one key algorithm, its correctness and its complexity in the 2 minute individual viva.

| Bhavya | Tanushree |
|---|---|
| ContactGraph, RingBuilder, CsvParser, SyntheticNetworkGenerator | DisjointSetUnion and live contact updates |
| ApproxVertexCover, GreedyVertexCover, IndexedMaxHeap, ExactVertexCover | BloomFilter, HashFunctions, exposure check, double-dose guard, tuning panel |
| Spring Boot API layer, validation, error handling | RandomizedQuickselect, MedianOfMedians, budget mode percentiles |
| Benchmark harness | OutbreakSimulator |
| Report: design, vertex cover, NP-hardness | Frontend pages and charts |
| | Report: Bloom filter, disjoint-set union, Quickselect, simulation |

Shared: proposal, README, testing review of each other's code, demo rehearsal.

---

## 14. Demo script (about 8 minutes)

1. Problem in one sentence and the vertex cover insight (30 seconds).
2. Upload the SocioPatterns high school network; show validation warnings (1 minute).
3. Clusters view; add a live contact and watch two clusters merge (1 minute).
4. Mark two confirmed cases; show ring 1 and ring 2 (1 minute).
5. Generate the plan; compare approximation, greedy and exact on a small cluster; point at the guarantee (1.5 minutes).
6. Budget mode with half the doses; unprotected links listed (1 minute).
7. Exposure self-check showing "possibly exposed" then the exact confirmation; tuning panel (1 minute).
8. Simulation result and benchmark charts (1 minute).
9. Invite the examiner to change the input or try an edge case, for example a self-contact or a budget of 0.

---

## 15. Viva preparation checklist

- [ ] 2-approximation proof and the tight star / complete bipartite examples
- [ ] Why vertex cover is NP-complete (independent set / clique reduction)
- [ ] Why greedy has no constant guarantee
- [ ] Bloom filter false positive formula, optimal k, why there are no false negatives
- [ ] Disjoint-set union: union by rank, path compression, inverse Ackermann bound
- [ ] Randomized Quickselect expected linear time versus median-of-medians worst-case linear time
- [ ] Time and space complexity of every class we wrote
- [ ] Walkthrough of one critical Java method each (examiner may ask)

---

## 16. Open decisions

- [ ] Confirm faculty approval of the proposal and the CO5 mapping (Bloom filter plus randomized Quickselect).
- [ ] Choose which SocioPatterns dataset to use and confirm its licence terms.
- [ ] Decide whether to deploy (optional) or demo locally.
- [ ] Pick at most one stretch feature after 15 November, only if features 1 to 12 are complete.
