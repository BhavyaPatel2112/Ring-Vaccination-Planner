# 9. Status on 14 November (Tanushree's half)

Everything in [04-what-tanushree-does-next.md](04-what-tanushree-does-next.md) is built, tested and committed, except the items in "Still to do" below.

## What was added

| Area | Pieces |
|---|---|
| Algorithms (`engine/`) | `dsu/DisjointSetUnion` and `OutbreakClusters`; `heap/IndexedMaxHeap`; `cover/GreedyVertexCover`; `bloom/HashFunctions` (FNV-1a 64-bit, MurmurHash3 32-bit, checked against the published test vectors), `BloomFilter` and `BloomExperiment`; `sim/OutbreakSimulator`; `followup/ContactDefinition` (feature E) and `FollowUpPlanner` (features B and C) |
| Endpoints | clusters, live contacts, greedy in plan and compare, compare on every link, follow-up, exposure check, registry check, Bloom tuning and stats, simulate, benchmarks (saved and live) |
| Screens | plan details, compare, clusters, deadlines and monitoring, exposure self-check, Bloom tuning, simulation, benchmarks |
| Benchmarks | approximation against greedy (1K to 1M contacts), union-find against BFS, Bloom filter against HashSet, written up in `docs/benchmarks/README.md` |
| Docs | full README with every endpoint, results and screenshots; `docs/screenshots`; SocioPatterns converter |
| Tests | about 100 new tests; the whole backend suite passes (over 240 tests) |

## Small fixes to existing code

- `PlanService`: `cond ? 1.0 : null` unboxed `null` and crashed for any method without a lower bound (greedy). Written out with if statements.
- `RingChart` (and the new charts): with a saved light theme the charts kept dark colours until the theme was toggled, because the header applied the theme before the chart started listening. The colours are now read again after mounting.
- `data/sample/README.md`: P015's market visit (07:00 to 12:00) does not overlap the case's visit (18:00 to 19:00); the app correctly says "not exposed".
- `ring`, `plan` and `compare` accept two optional contact settings (`minDurationMinutes`, `windowDays`) and report what they left out; without them nothing changes.

## Findings worth saying in the demo

- Greedy uses about 19% fewer doses than the 2-approximation on realistic towns, but it has no guarantee: on the trap samples it needs 23 against 12 and 40 against 18. The approximation is never above 2 times the minimum and is 18 times faster at a million contacts.
- The Bloom filter saves 84 to 88 times the memory of a HashSet, with the measured false positive rate within 3% of the formula. A HashSet lookup is faster (Java caches String hashes), so the filter's advantage is memory, not speed.

## Still to do (needs your laptop)

1. ~~**Run the tests and benchmarks on your laptop.**~~ Done on 14 November: tests pass and experiments 4 to 6 were rerun on the demo laptop. `cd backend && ./mvnw test`, then rerun experiments 4 to 6 so every table comes from one machine: `java -Xmx3g -cp target/classes com.ringvax.bench.BenchmarkRunner approx-vs-greedy ../docs/benchmarks` (and `dsu`, `bloom`). Update the numbers in `docs/benchmarks/README.md` if they change.
2. **SocioPatterns:** download the high school files and run the converter (see `data/sociopatterns/README.md`), then copy the exact licence from the download page into that README.
3. **Demo rehearsal** (below), the viva for your algorithms, and your sections of the report when the faculty asks.
4. Optional: marking a refusal from the screen (feature A interactive), described in [07](07-where-your-code-plugs-in.md). Refusals from the CSV already work everywhere.

## An 8-minute demo order

1. **Load** "Neighbourhood outbreak". Mention the validation with "Messy data" (rows rejected with line numbers, nothing silently dropped).
2. **Network:** the two cases, ring 1 orange, ring 2 yellow. Generate the plan: 21 doses, at most 1.24 times the minimum. Click a person to show why they are in the plan.
3. **Plan details:** reasons per dose; budget 10 (ring 1 first, then the top 10% by Quickselect); the double-dose guard skips P005 and P013 (Bloom hit, then confirmed).
4. **Compare:** the three methods on the rings. Then load "Greedy trap (35 people)", choose "Every link": approximation 24, exact 12, greedy 23. On the 58-person trap the exact solver refuses (NP-hard).
5. **Clusters:** generate a sparse town (400 people, 1.4 contacts); report a contact between two clusters and show the merge in microseconds.
6. **Deadlines and monitoring:** vaccinate-by order from the heap; switch on the 15-minute and 14-day settings and show how many contacts are left out.
7. **Exposure self-check:** V002, 1 Nov, 18:30 to 19:15: "possibly exposed", then confirmed. For a false alarm, first mark P031 as a case on the network screen, then check V002 from 19:30 to 20:00: the 19:00 hour is in the filter, but P031 left at 19:15, so the exact check clears it.
8. **Simulation:** about 94% fewer infections than no vaccination, 80% fewer than random doses.
9. **Bloom tuning and Benchmarks** if there is time: move k away from 7 and watch the rate rise on both sides.
