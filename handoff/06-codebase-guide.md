# 6. Codebase guide

## Layout

```
backend/                 Spring Boot 4.1.2, Java 17, Maven wrapper
  src/main/java/com/ringvax/
    api/                 REST controllers, error handler, CORS, request/response records (dto/)
    service/             NetworkService (store, load, view), PlanService (ring, plan, compare)
    data/                CSV parsing and validation, synthetic generator, quality warnings
    engine/graph/        ContactGraph, Person, Contact
    engine/ring/         RingBuilder, RingResult
    engine/cover/        VertexCoverSolver contract, 2-approximation, exact solver
    engine/select/       Randomized Quickselect, median-of-medians
    engine/plan/         VaccinationPlanner (ring 1 + cover + budget mode)
    bench/               Benchmark harness, graph families, brute force, runner
  src/test/java/...      One test class per algorithm, plus ApiTest
frontend/                React 19 + Vite 8, Tailwind CSS 4
  src/api/               fetch wrapper and one function per endpoint
  src/state/             Shared workspace state (network, cases, ring, plan)
  src/components/        ui/, shell/, network/, console/, load/
  src/pages/             LoadPage, ConsolePage, PlaceholderPage
  src/lib/               cn(), number formatting, list of tools/screens
data/sample/             Hand-made sample networks (see its README)
data/sociopatterns/      Real data goes here (to do)
docs/benchmarks/         Raw CSV results and README
docs/report/, docs/screenshots/   Empty for now
handoff/                 This folder
```

## Backend classes

| Class | Responsibility |
|---|---|
| `engine/graph/Person` | Record: id, name, age (nullable), role, vaccinated, eligible, refused; `canBeVaccinated()` |
| `engine/graph/Contact` | Record: endpoints a < b (person indices), date (nullable), durationMinutes (-1 if unknown) |
| `engine/graph/ContactGraph` | People by index plus id map; adjacency as int arrays of (neighbour, contact index); `addPerson`, `addContact`, `indexOf`, `degree`, `neighbour(p, i)`, `contactAt(p, i)`, `hasContact`. Does not reject duplicate links itself; callers check first. |
| `engine/graph/UnknownPersonException` | Unknown id (API: 400 `UNKNOWN_PERSON`) |
| `data/CsvParser` | `parseNetwork(people, contacts)`, `parseCheckIns`, `parseCases` → `ParseResult<T>` (value + `ValidationIssue` warnings) or `CsvFormatException` (API: 400 `INVALID_FILE`) |
| `data/SyntheticNetworkGenerator` | `generate(population, avgContacts, seed)` or `generate(Config)` |
| `data/ContactQualityCheck` | `check(graph, cases, asOf)` → `QualityWarning` list |
| `data/CheckIn`, `data/ConfirmedCase` | Records from checkins.csv and cases.csv |
| `engine/ring/RingBuilder` | `new RingBuilder(graph[, contactFilter]).build(caseIds, depth)` → `RingResult` (levels, rings, mandatory, alreadyVaccinated, cannotVaccinate, targetContacts, casesWithoutContacts) |
| `engine/cover/CoverProblem` | Target links + exclusions; precomputes `forcedPeople()`, `freeContacts()`, `unprotectableContacts()`; `isValidCover`, `uncoveredContacts`. Build with `fromRing`, `of`, `withExclusions`, `allContacts`. |
| `engine/cover/VertexCoverSolver` | `name()`, `solve(CoverProblem)` → `CoverResult` |
| `engine/cover/CoverResult` | method, choices (`CoverChoice`: person, reason enum, explanation), unprotectableContacts, lowerBound, runtimeNanos, searchNodes |
| `engine/cover/ApproxVertexCover` | `NAME = "APPROX"` |
| `engine/cover/ExactVertexCover` | `NAME = "EXACT"`; `new ExactVertexCover(maxPeople ≤ 64, timeout)`; throws `ExactSolverLimitException` (API: 422) |
| `engine/select/RandomizedQuickselect` | `new RandomizedQuickselect(seed)`: `select(int[]/long[], k)`, `percentile(int[], p)`, `median(long[])`, static `percentileRank(n, p)`. Reorders the array. |
| `engine/select/MedianOfMedians` | `MedianOfMedians.select(int[], k)` |
| `engine/plan/VaccinationPlanner` | `new VaccinationPlanner(seed).plan(graph, ring, solver, budgetOrNull)` → `VaccinationPlan` |
| `service/NetworkService` | In-memory store of `StoredNetwork` (id, name, source, graph, cases, checkIns, warnings, qualityWarnings, createdAt); load, generate, samples, view (sampled to 1,500 nodes) |
| `service/PlanService` | Map of solver names → suppliers (add GREEDY here); ring, plan, compare; `guarantee(...)` text |
| `api/GlobalExceptionHandler` | Maps every exception to `ApiError { error, message, details }` |
| `bench/Harness` | `new Harness(warmup, repeats).measure(prepare, task)` → median, min, max |
| `bench/BenchmarkGraphs` | `random(n, avgContacts, seed)`, `regular(n, d, seed)` |
| `bench/BenchmarkRunner` | `main(experiment, outDir)` |

**Test helpers:** `engine/cover/TestGraphs` (star, completeBipartite, path, random, randomExclusions, `bruteForceMinimum`). Use it for the greedy tests.

## API shapes (JSON)

```jsonc
// NetworkSummary (load, upload, generate, summary, list items)
{ "networkId": "net-1a2b3c4d", "name": "Neighbourhood outbreak", "source": "sample", "people": 40, "contacts": 73,
  "caseIds": ["P003","P023"], "checkIns": 13, "warnings": [ { "file": "contacts.csv", "line": 4, "code": "SELF_CONTACT", "message": "..." } ],
  "qualityWarnings": [ { "code": "CASE_WITHOUT_CONTACTS", "personId": "P009", "message": "..." } ], "createdAt": "2026-11-04T10:00:00" }

// NetworkView (GET /api/networks/{id})
{ "networkId": "...", "totalPeople": 40, "totalContacts": 73, "sampled": false,
  "nodes": [ { "id": "P001", "name": "Rahul Sharma", "age": 38, "role": "engineer", "vaccinated": false, "eligible": true, "refused": false, "confirmedCase": false, "contacts": 6 } ],
  "links": [ { "source": "P001", "target": "P002", "date": "2026-11-01", "durationMinutes": 420 } ] }

// RingResponse
{ "depth": 2, "cases": ["P003","P023"], "rings": [["P001", "..."], ["P005", "..."]], "mandatory": ["..."],
  "alreadyVaccinated": ["..."], "cannotVaccinate": ["P004","P011"], "targetLinks": [ { "a": "P013", "b": "P039" } ], "casesWithoutContacts": [] }

// PlanResponse
{ "method": "APPROX", "doses": 21, "dosesForFullPlan": 21, "budget": null, "complete": true, "budgetLimited": false, "targetLinks": 20,
  "people": [ { "personId": "P001", "name": "Rahul Sharma", "reason": "RING_1", "explanation": "Direct contact of confirmed case P003" } ],
  "unprotectedLinks": [], "unprotectableLinks": [], "ring1NotVaccinated": [], "lowerBound": 17, "provenRatioBound": 1.235,
  "guarantee": "At most 2 times the minimum number of doses ...", "runtimeMs": 0.47 }

// CompareResponse
{ "targetLinks": 20, "methods": [ { "method": "APPROX", "doses": 21, "runtimeMs": 0.4, "ratioToExact": 1.24, "guarantee": "...", "error": null },
                                  { "method": "EXACT", "doses": 17, "runtimeMs": 1.1, "ratioToExact": 1.0, "guarantee": "...", "error": null } ] }
```

Reasons in `people[].reason`: `RING_1`, `FORCED`, `MATCHED`, `GREEDY`, `EXACT`, `BUDGET_PRIORITY`.

## Frontend structure

| Path | Role |
|---|---|
| `main.jsx` | Router, `MotionConfig` (respects reduced motion), `WorkspaceProvider`, toasts (Sonner) |
| `App.jsx` | Routes: `/load`, `/network`, and a placeholder for every tool with `ready: false` |
| `lib/tools.js` | Every screen (path, title, description, icon, ready). The launcher and placeholders read this. |
| `api/client.js`, `api/endpoints.js` | `apiGet`, `apiPost` (JSON or FormData), `ApiError` (status, code, details), `BackendDownError`; one function per endpoint |
| `state/workspace.jsx` | `WorkspaceProvider`: network, view, people (Map id → node), cases, depth, ring, plan, method, busy, backendDown; actions `loadSample`, `upload`, `generate`, `switchNetwork`, `toggleCase`, `setDepth`, `setMethod`, `createPlan(options)` (pass `{ budget }` for budget mode) |
| `state/context.js` | `useWorkspace()`. Import the hook from here, not from `workspace.jsx` (keeps hot reload working). |
| `components/shell/` | `AppShell` (backdrop, header, page fade, backend-down banner), `Header`, `Launcher`, `SceneBackdrop` |
| `components/network/` | `NetworkCanvas` (Cytoscape; ref API: `pointOf(id)`, `fit()`, `focusOn(id)`), `networkStyle.js`, `Callouts` (dashed line from an element with `data-callout="key"` to a node), `Legend`, `CurvedGrid`, `personStatus.js` |
| `components/console/` | Guide, cases, people tabs, person popover, plan card, ring chart |
| `components/load/` | Sample cards, upload panel, generator panel, load report |
| `components/ui/` | Building blocks (below) |

## Design system

**Colour tokens** (CSS variables in `index.css`, with light and dark values). Each colour has one meaning everywhere:

| Token / Tailwind name | Meaning |
|---|---|
| `--case` / `case` | Confirmed case (red) |
| `--ring1` / `ring1` | Ring 1 (orange) |
| `--ring2` / `ring2` | Ring 2 (yellow) |
| `--vaccinated` / `vaccinated` | Already vaccinated (green) |
| `--excluded` / `excluded` | Cannot be vaccinated (grey) |
| `--planned` / `planned` | Dose in the plan (cyan ring) |
| `--accent` / `accent` | Interactive and selected (blue) |
| `--warning`, `--danger` | Warnings, errors |
| `text`, `muted`, `faint` | Text levels |
| `.glass`, `.glass-strong`, `.inset` | Frosted panel, more opaque panel, inner row |

**Components** (`components/ui`):
- `GlassPanel` (card with one quiet entrance) and `PanelTitle` (icon, title, subtitle, action).
- `Button`: `variant` is primary, glass, ghost or danger; also `size="sm"`, `loading` and `icon`. `IconButton` for icon-only buttons.
- `Segmented`: pill switch with a sliding highlight.
- `Tabs`: underline tabs with counts.
- `StatTile`: number that counts with NumberFlow.
- `Notice`: tone is info, warning or error. `Empty` for empty states.
- `Slider`: Radix.

**Rules the current screens follow:**
- **Text:** at least 12px (`text-xs`).
- **Corners:** cards `rounded-[20px]`, inner rows `rounded-xl`, pills `rounded-full`.
- **One primary action per card.**
- **Motion:** a page fade, smooth tab and number changes, and nothing decorative that loops.
- **Charts:** Chart.js. Read the colours from CSS variables and listen for the `ringvax-theme` window event to recolour, as `RingChart.jsx` does.
- **Lists:** `useAutoAnimate` from `@formkit/auto-animate/react`.
- **Icons:** `lucide-react`.

**Libraries in use:** react-router 8, motion 14, tailwindcss 4, @radix-ui/react-slider, @formkit/auto-animate, @number-flow/react, sonner, lucide-react, cytoscape with cytoscape-fcose, chart.js with react-chartjs-2, clsx with tailwind-merge, and the Manrope variable font.
