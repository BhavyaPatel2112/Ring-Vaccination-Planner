# 7. Where your code plugs in

Step by step, for each piece of Tanushree's work. File paths are relative to `backend/src/main/java/com/ringvax/` unless they start with `frontend/`.

## Greedy vertex cover (feature 5)

1. Create `engine/heap/IndexedMaxHeap.java`: keys by person index, `insert`, `extractMax`, `decreaseKey`, `contains`, `isEmpty`.
2. Create `engine/cover/GreedyVertexCover.java implements VertexCoverSolver` with `NAME = "GREEDY"`. Use the shared problem so exclusions work like the other methods:
   - start with `problem.forcedPeople()`, with reason `FORCED` (copy the loop from `ApproxVertexCover`);
   - count uncovered links per person over `problem.freeContacts()` only, put them in the heap, and repeatedly take the person with the most uncovered links;
   - for each of their links still uncovered, `decreaseKey` the other end;
   - reason `CoverChoice.Reason.GREEDY`, explanation like `"Covered 6 unprotected links when chosen"`;
   - return `new CoverResult(NAME, choices, problem.unprotectableContacts(), -1, elapsed, 0)`. Use -1 as the lower bound because greedy has no guarantee.
3. Register it in `service/PlanService` constructor: `methods.put(GreedyVertexCover.NAME, GreedyVertexCover::new);`. The plan and compare endpoints and `/api/methods` then include it automatically.
4. In `PlanService.guarantee(...)` the `default` branch already says "No fixed guarantee for this method."
5. Frontend: add `{ value: 'GREEDY', label: 'Greedy' }` to the method switch in `frontend/src/components/console/PlanCard.jsx`.
6. Tests: copy the patterns from `ApproxVertexCoverTest`, and use `TestGraphs` and `bruteForceMinimum`. Check validity with `problem.isValidCover(result.people())`. The sample `data/sample/greedy-worst-case-12` should give 23 against a minimum of 12.

## Disjoint-set union, clusters and live contacts (features 2 and 12)

1. `engine/dsu/DisjointSetUnion.java`: `find`, `union`, `size`, and per-cluster counts of cases and vaccinated.
2. A service, for example `service/ClusterService`, keeps one DSU per network id (build it from `networks.get(id).graph()` the first time it is needed).
3. `GET /api/networks/{id}/clusters`: a list of `{ clusterId, size, cases, vaccinated, members? }`. Cases come from the stored network's `cases()` or from the request.
4. `POST /api/networks/{id}/contacts`:
   - resolve both ids (`graph.indexOf`; -1 → throw `UnknownPersonException`);
   - reject a self-contact or `graph.hasContact(a, b)` with a 400;
   - otherwise `graph.addContact(a, b, date, minutes)` and `dsu.union(a, b)`.

   The graph object is shared, so the network view, rings and plans see the new link straight away. The frontend should then refetch the view (`getNetworkView`) and rebuild the rings.
5. Error format: throw `IllegalArgumentException` for a 400. `GlobalExceptionHandler` already turns it into `{ error: "INVALID_REQUEST", message }`.

## Bloom filter, exposure and registry (features 6, 9, 10)

- **Check-ins:** they are already parsed and stored in `StoredNetwork.checkIns()`. Each has personId, venueId, start and end. The sample `outbreak-small` has 13 check-ins; `data/sample/README.md` lists which visits overlap a case.
- **Exposure filter:** for each confirmed case, insert `venueId + "|" + hourBucket` for every hour they spent at a venue (`StoredNetwork.cases()` gives the case ids and confirmation dates). Optionally limit this to an infectious window before confirmation.
- **Exposure check:** query each hour of the person's visits.
  - A miss means "not exposed".
  - A hit means "possibly exposed". Then confirm against the exact check-in list (same venue, overlapping time) and report only the venue and date.
- **Registry:** insert the ids of people with `Person.vaccinated()`. Confirm hits against the graph's people.
- **Tuning endpoints:** build filters with the requested m and k over a fixed set of keys, and measure the false positive rate on random non-member keys. Report memory (m / 8 bytes) against a `HashSet<String>` estimate.
- **Rule:** never return a Bloom hit as final. Always include the confirmed result.

## Outbreak simulation (feature 11)

- **Inputs:** the stored graph, the cases (as initially infected), and a set of vaccinated people. Get the plan's people from `new VaccinationPlanner(seed).plan(graph, ring, solver, null).people()`.
- **Random baseline:** pick the same number of people uniformly from those who can be vaccinated, using the seed.
- **Percentiles:** use `new RandomizedQuickselect(seed).percentile(values, 50 or 90)` (copy the array first; it reorders in place).

## Contact definition settings (feature E)

`RingBuilder` already takes a filter: `new RingBuilder(graph, contact -> ...)`.

1. Add optional `minDurationMinutes` and `windowDays` (plus a reference date, or use the latest case confirmation date) to `RingRequest`, `PlanRequest` and `CompareRequest`, with validation annotations.
2. In `PlanService.buildRing(...)`, build the predicate (for example `c.durationMinutes() < 0 || c.durationMinutes() >= min`) and pass it to `RingBuilder`.
3. Decide and document what happens to contacts with an unknown date or duration. The data quality check already counts them as `MISSING_CONTACT_DETAILS`. Keeping them, to be safe, is the suggested choice.
4. Return how many contacts were excluded, and why, so the screen can show it.

## Deadlines and monitoring (features B and C)

- **Last exposure:** for a ring member, the most recent `Contact.date()` on a link to someone in a closer ring (or to a case).
- **B:** deadline = last exposure + window (default 72 hours). Order the doses by deadline with your heap. People past the window go to monitoring instead.
- **C:** monitoring list = ring members who are not cases, not vaccinated and not in the plan, plus anyone past the window. End date = last exposure + incubation period (default 21 days).
- **Where to put it:** either add fields to `PlanResponse` (and fill them in `PlanService.plan`), or create a separate endpoint. A separate `POST /api/networks/{id}/follow-up` keeps `PlanResponse` simple.

## Marking a refusal from the screen (feature A, interactive)

Exclusions currently come from the CSV (`eligible` and `refused` columns). To let someone be marked as refused in the app:

1. Add `ContactGraph.updatePerson(int index, Person person)`. Keep the same id; replace the `Person` in the list.
2. Add an endpoint, for example `POST /api/networks/{id}/people/{personId}` `{ refused: true }`.
3. The frontend refetches the view and rebuilds the rings. Everything else (ring 1 mandatory list, forced people, unprotectable links) already follows the `Person` flags.

## Benchmarks

Add a method to `bench/BenchmarkRunner` in the same style as the existing ones, and call it from `main` under a new name:
- use `new Harness(3, 5)` (or `(3, 7)`) and `harness.measure(() -> freshInput, input -> work(input))`;
- write raw rows with `CsvTable` to `docs/benchmarks/<name>.csv`.

Then add a results section to `docs/benchmarks/README.md`.

## A new screen

1. Create `frontend/src/pages/PlanDetailsPage.jsx` (for example).
2. In `App.jsx`, add `<Route path="plan" element={<PlanDetailsPage />} />` next to the network route.
3. In `lib/tools.js`, set `ready: true` for that tool. It then stops showing the placeholder in the launcher.
4. Read shared state with `const { network, ring, plan, createPlan, ... } = useWorkspace()` (import from `state/context.js`). Call `createPlan({ budget: 10 })` for budget mode.
5. Add API calls to `api/endpoints.js` using `apiGet` and `apiPost`. Errors arrive as `ApiError` with `message` and `details`, so show `message` in a toast (`toast.error`) or a `Notice`.
6. Build the page from `GlassPanel`, `PanelTitle`, `Button`, `Segmented`, `Tabs`, `StatTile` and `Notice`, using the colour tokens. Look at `ConsolePage.jsx` for layout and `RingChart.jsx` for a themed Chart.js chart.
7. If the screen shows the network, reuse `NetworkCanvas` (pass view, ring, plan, cases, selected and hovered).
