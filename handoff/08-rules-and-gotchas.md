# 8. Rules, conventions and gotchas

This is the full working context for anyone continuing the code: a person, or any tool you use to help write it. Read it before changing anything.

## Project context in five lines

- **Project:** Ring Vaccination Planner, an Advanced Algorithm CIA 3 project at ATLAS SkillTech University, due **20 November 2026** (live demo and individual viva).
- **Team:** Bhavya Patel (done; away after 8 November) and Tanushree (continuing). Each person commits only their own work, from their own GitHub account.
- **Stack:** Java 17 with Spring Boot 4.1.2 (backend, port 8090), and React 19 with Vite 8 and Tailwind 4 (frontend, port 5173, `/api` proxied to the backend).
- **The problem:** choose the fewest people to vaccinate around confirmed cases so that every risky contact link has a vaccinated end. That is minimum vertex cover, which is NP-hard.
- **The method:** a 2-approximation for the plan, an exact solver for proof on small rings, a greedy baseline for comparison, and a Bloom filter plus randomized Quickselect for the CO5 requirement.

## Non-negotiable rules (from the assessment)

1. **Every assessed algorithm is our own Java code.** No library implementations: no Guava `BloomFilter`, no JGraphT, no Apache Commons selection. Java collections (`ArrayList`, `HashMap`, `ArrayDeque`) are allowed as supporting structures. `Arrays.sort` appears only as a benchmark baseline.
2. **Java runs the algorithms; the frontend only displays.** No algorithm logic in JavaScript and no precomputed results in the browser. Cytoscape only draws the network; the fCoSE layout is display, not an assessed algorithm.
3. **Never claim the approximation is optimal.** Always "at most 2 times the minimum", plus the measured or proven ratio.
4. **A Bloom filter hit means "possibly present".** Confirm it exactly before showing a final answer.
5. **Every random process takes a seed** (Quickselect pivots, generators, simulation). The default seed in the API is 2026.
6. **Each algorithm class has Javadoc** stating its purpose, time complexity and space complexity. Look at `ApproxVertexCover` or `RandomizedQuickselect` for the style.
7. **At least 5 JUnit tests per core algorithm**, including at least one boundary or edge case. Existing classes have 9 to 19.
8. **Invalid input is never silently dropped.** Rejected rows are reported with line numbers; invalid requests return 400 with a clear message. 404 is for an unknown network; 422 is for a valid request that cannot be solved as asked.

## Repository conventions

- **Commits:** small, one logical change each, plain imperative messages ("Add Bloom filter with double hashing"). No generated-by notes or co-author trailers. Push at least once a day. `main` must always build, with all tests passing.
- **Java style:**
  - 4-space indents.
  - Records for data.
  - Constructor injection for Spring beans.
  - Engine classes have no Spring dependency.
  - Domain errors are exceptions, mapped in `GlobalExceptionHandler`.
  - Comments explain *why*, not *what*.
- **Package boundaries:** `engine/` is pure algorithms; `data/` handles input; `service/` orchestrates; `api/` handles HTTP only.
- **JavaScript style:** 2 spaces, no semicolons, single quotes (as the existing files). Components are PascalCase `.jsx`; helpers are `.js`. Run `npx oxlint` (it should print nothing) and `npm run build` before committing.
- **Language in the interface:** plain words for outbreak teams ("Cannot be vaccinated", not "excluded vertex"). Sentence case. Short labels.
- **Keep it human:** names, messages, comments and docs are written plainly and simply.

## Gotchas already found (save yourself the time)

| Area | Gotcha | What to do |
|---|---|---|
| Spring Boot 4 | The MockMvc test annotation moved: `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` | Copy the imports from `ApiTest`. Parse JSON with `com.jayway.jsonpath.JsonPath` (Jackson is version 3, under `tools.jackson`). |
| Spring Boot 4 | The 422 status is `HttpStatus.UNPROCESSABLE_CONTENT` (not `UNPROCESSABLE_ENTITY`); 413 is `CONTENT_TOO_LARGE` | Already used in the handler |
| Spring | A service with two constructors fails to start ("No default constructor found") | Mark the Spring one `@Autowired` (see `NetworkService`) |
| Spring | The sample folder path is relative (`../data/sample`) | Start the backend from `backend/`, or set `ringvax.sample-dir` |
| Ports | 8080 was taken on Bhavya's laptop | The backend uses 8090. Change `application.properties` and `frontend/vite.config.js` together. |
| Data | Networks live in memory | Restarting the backend loses them. The frontend handles the 404 by going back to Load data. |
| `ContactGraph` | It does not reject duplicate links by itself | Check `hasContact(a, b)` before `addContact` |
| Quickselect | `select` and `percentile` reorder the array you pass | Pass a copy if you still need the original order |
| Exact solver | It is very fast on ordinary random networks (pruning works well) | Use random 4-regular graphs (`BenchmarkGraphs.regular`) to show exponential growth. Search nodes are in `CoverResult.searchNodes()`. |
| Cytoscape | It injects CSS forcing `position: relative` on its container | Keep the container `h-full w-full` inside a sized wrapper (as `NetworkCanvas` does) |
| Cytoscape | Colours ignore alpha (`rgba`) | Use solid colours, plus `line-opacity`, `background-opacity` or `opacity` |
| Cytoscape | Style transitions and animated layouts depend on animation frames, which pause in background tabs | The layout runs with `animate: false`, and colours change instantly. Keep it that way. |
| Theme | Canvas and chart colours do not follow CSS automatically | Read the CSS variables, and listen for `window` event `ringvax-theme` to restyle (see `RingChart.jsx`, `NetworkCanvas.jsx`) |
| React | Hot reload breaks if a context hook lives in the same file as the provider | Import `useWorkspace` from `state/context.js` |
| Callouts | Lines are drawn from any element with `data-callout="key"` to a node | Pass `{ key, nodeId, color }` to `<Callouts>`. Keep at most one or two lines visible at a time. |
| Build | Vite warns that the bundle is over 500 kB (Cytoscape and Chart.js) | Harmless. If you want, lazy-load pages with `React.lazy`. |

## Definition of done for any task

- [ ] The algorithm is in `engine/`, with Javadoc stating purpose, time and space complexity.
- [ ] At least 5 tests, including an edge case. `./mvnw test` passes.
- [ ] The endpoint has an API test for success and for its error paths.
- [ ] The screen uses the design system, works in light and dark, and has no text under 12px.
- [ ] `npx oxlint` is clean and `npm run build` passes.
- [ ] It is committed with a clear message from your own account, and pushed.
