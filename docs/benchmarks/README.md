# Benchmarks

Raw results from `BenchmarkRunner` (backend package `com.ringvax.bench`). Every input is generated from a fixed seed (2026), so rerunning gives the same inputs; times will differ slightly by machine.

## How to reproduce

```bash
cd backend
./mvnw -q compile
java -Xmx3g -cp target/classes com.ringvax.bench.BenchmarkRunner all ../docs/benchmarks
```

Replace `all` with `quality`, `exact-growth`, `selection`, `approx-vs-greedy`, `dsu` or `bloom` to run one experiment. Running a single experiment records its machine in `environment-<experiment>.txt`, so the environment of the other results is kept.

**Method.** Each timing is the median of 5 or more measured runs, after 3 warm-up runs so the JIT compiler has optimised the code first. The median is taken with our own randomized Quickselect. Input preparation (for example copying the array before an in-place selection) is not timed. Machine details are in [environment.txt](environment.txt): Java 17.0.18, Apple Silicon, 8 cores.

| File | What it holds |
|---|---|
| [approx-vs-exact.csv](approx-vs-exact.csv) | One row per network: approximation size, exact minimum, lower bound, ratio |
| [approx-vs-exact-summary.csv](approx-vs-exact-summary.csv) | Mean and worst ratio per size and density |
| [exact-growth.csv](exact-growth.csv) | One row per network: exact solver time and search nodes, approximation time, brute force time |
| [exact-growth-summary.csv](exact-growth-summary.csv) | Medians per size |
| [selection.csv](selection.csv) | Quickselect, median-of-medians and full sort, per input type and size |
| [approx-vs-greedy.csv](approx-vs-greedy.csv) | Approximation and greedy run time and plan size on synthetic towns, 1K to 1M contacts |
| [dsu-vs-bfs.csv](dsu-vs-bfs.csv) | Union-find cluster updates against recomputing clusters with BFS |
| [bloom-vs-hashset.csv](bloom-vs-hashset.csv) | Bloom filter accuracy, memory and lookup time against a HashSet |

Experiments 4 to 6 (Tanushree's) were first measured on a 2-core Linux machine with Java 21 (the same seeds); rerun them on the demo laptop before submitting so every table comes from one machine. The numbers below are from that first run.

## 1. Approximation quality against the exact minimum

30 seeded random networks per size, at two densities. Both methods solve the full vertex cover of each network.

| People | Mean ratio (avg 3 contacts) | Worst ratio (avg 3) | Mean ratio (avg 6 contacts) | Worst ratio (avg 6) |
|---|---|---|---|---|
| 10 | 1.61 | 2.00 | 1.36 | 1.67 |
| 15 | 1.63 | 2.00 | 1.36 | 1.56 |
| 20 | 1.64 | 2.00 | 1.39 | 1.64 |
| 25 | 1.62 | 1.83 | 1.44 | 1.60 |
| 30 | 1.65 | 1.87 | 1.47 | 1.67 |
| 35 | 1.61 | 1.79 | 1.44 | 1.68 |
| 40 | 1.63 | 1.88 | 1.44 | 1.58 |

**Reading.**
- The guarantee holds on all 420 networks: the ratio never goes above 2.
- It reaches exactly 2 on some small sparse networks. That is the tight case from the proof (a matching where one endpoint per link would have been enough).
- On denser networks the approximation is closer to the minimum, at about 1.4×, because the matched pairs overlap more with the optimal cover.
- This is why the app shows "at most 2 times the minimum", plus the proven ratio for each plan, and never claims the plan is optimal.

## 2. Exact solver growth on hard networks

Random 4-regular networks: everyone has exactly 4 contacts. No one has a single contact, so the solver's "take the neighbour" shortcut never applies, which makes these harder than real contact networks. There are 5 networks per size; the figures are medians.

| People | Exact solver (ms) | Search tree nodes | Approximation (ms) | Brute force, all subsets (ms) |
|---|---|---|---|---|
| 16 | 0.002 | 15 | 0.0016 | 0.24 |
| 20 | 0.003 | 19 | 0.0024 | 2.35 |
| 24 | 0.004 | 33 | 0.0021 | 38.5 |
| 28 | 0.006 | 51 | 0.0026 | – |
| 32 | 0.010 | 85 | 0.0028 | – |
| 36 | 0.016 | 123 | 0.0030 | – |
| 40 | 0.023 | 145 | 0.0034 | – |
| 44 | 0.046 | 257 | 0.0036 | – |
| 48 | 0.059 | 323 | 0.0040 | – |
| 52 | 0.112 | 485 | 0.0044 | – |
| 56 | 0.222 | 827 | 0.0044 | – |
| 60 | 0.242 | 799 | 0.0048 | – |
| 64 | 0.415 | 1,309 | 0.0051 | – |

**Reading.**
- **Brute force** multiplies by about 16 every 4 people (0.24 → 2.35 → 38.5 ms). That is 2^n: 1.93× per extra person. At 40 people it would take hours, so it is only run up to 24.
- **Branch and bound** is still exponential, but much slower-growing. Its search tree grows about 1.10× per extra person, so it doubles roughly every 7 people (15 → 1,309 nodes from 16 to 64).
- **Where the savings come from:** branching on the most connected person, pruning with the matching lower bound, the degree-1 rule, and solving separate groups independently.
- **Why it is still exponential:** vertex cover is NP-complete, so no method is known to avoid exponential growth in the worst case. That is why the API refuses more than 40 undecided people and has a 5 second limit.
- **Approximation:** it grows linearly and stays at a few microseconds throughout.

## 3. Selection: Quickselect against median-of-medians

Finding the median of n integers. Each method gets a fresh copy of the same array.

| Input | Values | Randomized Quickselect (ms) | Median-of-medians (ms) | Full sort (ms) |
|---|---|---|---|---|
| random | 10,000 | 0.27 | 0.57 | 0.68 |
| random | 100,000 | 1.17 | 3.47 | 4.55 |
| random | 1,000,000 | 8.68 | 21.87 | 51.27 |
| random | 10,000,000 | 88.66 | 217.63 | 693.95 |
| sorted | 10,000 | 0.08 | 0.31 | 0.12 |
| sorted | 100,000 | 0.68 | 3.68 | 0.33 |
| sorted | 1,000,000 | 4.53 | 12.27 | 0.23 |
| sorted | 10,000,000 | 29.43 | 113.49 | 2.04 |

**Reading.**
- **Both selection methods grow linearly.** Ten times more values takes about ten times longer: Quickselect goes 8.7 → 88.7 ms and median-of-medians 21.9 → 217.6 ms. The full sort grows faster, at about 13.5× per 10× more values, as n log n predicts.
- **Quickselect is about 2.5× faster than median-of-medians.** Median-of-medians has the stronger guarantee (worst case O(n)), but pays for it with a large constant: sorting groups of 5 and a second recursive selection.
- **Sorted input does not slow Quickselect down.** A fixed first-element pivot would go quadratic here, but random pivots keep it linear.
- **Limitation:** on already-sorted input, Java's built-in sort wins, because it detects sorted runs and finishes in about one pass. Real contact counts are not sorted, so the random-input rows are the relevant ones. At 1,000,000 values (the budget mode and quality warning case), Quickselect takes under 9 ms.

## 4. Approximation against greedy at growing size

Seeded synthetic towns (households, schools, workplaces, community links) with an average of 10 contacts per person. Both methods cover every link of the town.

| Contacts | People | Approximation (ms) | Greedy (ms) | Approximation doses | Greedy doses | Greedy ÷ approximation |
|---|---|---|---|---|---|---|
| 1,000 | 200 | 0.12 | 0.36 | 190 | 157 | 0.83 |
| 10,000 | 2,000 | 0.76 | 2.70 | 1,886 | 1,527 | 0.81 |
| 100,000 | 20,000 | 3.68 | 18.96 | 18,766 | 15,284 | 0.81 |
| 1,000,000 | 200,000 | 18.28 | 417.11 | 187,690 | 152,636 | 0.81 |

**Reading.**
- **Run time.** The approximation grows linearly (about 10× per 10× more contacts, 18 ms at a million contacts, well under the 2-second target). Greedy grows faster, as O((V + E) log V) predicts: every heap operation costs log V, and at a million contacts it is 23 times slower.
- **Plan size.** On these realistic towns greedy needs about 19% fewer doses than the approximation. That is the honest result: greedy is a good heuristic on typical networks.
- **But greedy has no guarantee.** On the greedy trap samples it picks 23 people where 12 are enough and 40 where 18 are enough (2.22×, worse than the approximation's proven bound of 2). The approximation is never above 2× on any input, and it gives a lower bound (the matching size) that proves how far from the minimum each plan can be. That is why the app uses the approximation for the plan and shows greedy as the comparison.

## 5. Clusters: union-find against recomputing with BFS

A synthetic town of the given size; its contacts arrive one by one. Union-find does one `union` per new contact. The baseline recomputes every cluster with a breadth-first search after each new contact; one recomputation at the final size is timed, and the total is that time multiplied by the number of contacts.

| Contacts | People | Union-find, all updates (ms) | Union-find per update (µs) | BFS per update (ms) | BFS for all updates (estimate) | Clusters |
|---|---|---|---|---|---|---|
| 10,000 | 2,500 | 0.28 | 0.028 | 0.45 | 4.5 s | 1 |
| 100,000 | 25,000 | 1.60 | 0.016 | 5.00 | 8.3 min | 3 |
| 1,000,000 | 250,000 | 9.76 | 0.010 | 49.95 | 13.9 hours | 27 |

**Reading.**
- **Union-find is effectively constant per update.** The per-update time does not grow with the network (it even falls, as the JIT warms up), matching the amortised O(α(n)) bound from union by rank plus path compression.
- **BFS grows linearly per update**, so over all updates it is quadratic. At a million contacts, one update takes about 50 ms with BFS against 0.01 µs with union-find, and processing every contact report would take about 14 hours instead of 10 ms.
- Both methods find the same number of clusters (the benchmark checks this).

## 6. Bloom filter against a HashSet

Keys shaped like the exposure keys (venue plus hour). Each filter is sized for a 1% false positive rate: m = −n ln p / (ln 2)², k = (m/n) ln 2 = 7. The measured rate uses 200,000 keys that were never inserted.

| Keys | Bits | k | Theory | Measured | Difference | Bloom memory | HashSet memory (estimate) | Memory saved | Lookup, filter | Lookup, HashSet |
|---|---|---|---|---|---|---|---|---|---|---|
| 10,000 | 95,851 | 7 | 1.00% | 1.01% | 1.1% | 11.7 KB | 986 KB | 84× | 76 ns | 10 ns |
| 100,000 | 958,506 | 7 | 1.00% | 1.02% | 1.5% | 117 KB | 9.6 MB | 84× | 129 ns | 31 ns |
| 1,000,000 | 9,585,059 | 7 | 1.00% | 0.98% | 2.6% | 1.1 MB | 100.5 MB | 88× | 108 ns | 18 ns |

**Reading.**
- **The formula holds.** The measured false positive rate is within 3% (relative) of (1 − e^(−kn/m))^k at every size, well inside the 20% target. There were no false negatives.
- **Memory.** The filter needs about 9.6 bits per key whatever the key is, so it uses 84 to 88 times less memory than a HashSet, which stores every string plus a table entry. This is the reason to use it: the exposure filter could sit on a phone or be shared without the visit records themselves.
- **Lookup speed: the HashSet is faster here.** Java caches each String's hash code, so a HashSet lookup is one cached hash and one comparison, while the filter hashes the key twice and reads 7 bits. The filter's advantage is memory, not speed, and the app never relies on it alone: every "possibly present" answer is confirmed against the exact records.
- **Memory estimate.** HashSet memory is estimated per key for a 64-bit JVM with compressed references (a 32-byte table node, its table slot, a 24-byte String and its byte array). Measuring heap use directly depends on when the garbage collector runs and was too noisy to report.
