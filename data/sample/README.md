# Sample data

Small hand-made networks for the demo and the tests. All names are made up. `SampleDataTest` in the backend checks that each folder still parses as described here.

## outbreak-small

A neighbourhood of 40 people in 10 households, with 73 contacts and 2 confirmed cases. Use it for the main demo.

- **Cases:** Aarav Sharma (P003, age 8, class 3B at Sunrise School) and Manoj Gupta (P023, bus driver on route 14), both confirmed on 2026-11-02.
- **Settings:** households, a school class and staff room, a bus route, an office, a pharmacy counter, a hospital ward, a market, a restaurant kitchen, college friends and doorstep deliveries.
- **Special people:**
  - P004 Anaya Sharma is an infant and P014 Divya Nair is pregnant. Both are marked `eligible=false` (no live vaccine).
  - P011 Neha Patel has `refused=true`.
  - P005, P013 and P022 are already vaccinated.
- **Contacts for the contact definition settings:**
  - Doorstep deliveries (4 to 5 minutes) and one bus ride (10 minutes) are under 15 minutes.
  - P022 to P038 on 2026-10-10 is outside a two-week window.
- **Venues in checkins.csv:**

  | Venue | Place |
  |---|---|
  | V001 | Sunrise School |
  | V002 | Central Market |
  | V003 | Green Cross Pharmacy |
  | V005 | Lakeview Mall food court |
  | V006 | Bus depot canteen |

  Some visits overlap a case's visit (P006, P015, P031, P029, P032, P038) and some do not (P020 the next day, P033 at a venue with no case, P036 after the case left).

## validation-demo

A network with one example of each input problem. It shows that bad rows are reported rather than silently dropped.

| File | Problem | What happens |
|---|---|---|
| people.csv | P003 listed twice | Second row skipped (`DUPLICATE_PERSON`) |
| people.csv | P004 age 140 | Row skipped (`INVALID_VALUE`) |
| people.csv | P005 vaccinated left blank | Assumed not vaccinated (`ASSUMED_VALUE`) |
| people.csv | P006 vaccinated "maybe" | Row skipped (`INVALID_VALUE`) |
| people.csv | P007 name contains a comma | Read correctly because it is quoted |
| contacts.csv | P001-P002 three times, once reversed | Merged into one link (`DUPLICATE_CONTACT` x2) |
| contacts.csv | P003-P003 | Row skipped (`SELF_CONTACT`) |
| contacts.csv | P099 does not exist | Row skipped (`UNKNOWN_PERSON`) |
| contacts.csv | Date written 01/11/2026 | Row skipped (`INVALID_VALUE`) |
| contacts.csv | Duration -10 | Row skipped (`INVALID_VALUE`) |
| contacts.csv | Row with only 3 values | Row skipped (`MALFORMED_ROW`) |
| cases-unknown-id.csv | Case P404 does not exist | Whole file rejected (400), naming P404 |

## Networks for testing the algorithms

| Folder | People | Links | Why it is here |
|---|---|---|---|
| star | 8 | 7 | One hub with 7 leaves. Optimum is 1 (the hub). The 2-approximation returns 2, its worst case. |
| complete-bipartite-3-3 | 6 | 9 | Every person on one side meets every person on the other side. Optimum is 3, the 2-approximation returns 6. |
| greedy-worst-case-12 | 35 | 115 | Layered network built so greedy "most connected first" keeps choosing badly. Greedy picks 23 people against an optimum of 12 (ratio 1.92). Small enough for the exact solver. |
| greedy-worst-case-18 | 58 | 259 | Same construction, larger. Greedy picks 40 against an optimum of 18 (ratio 2.22), worse than the approximation's guaranteed bound of 2. |

The greedy worst-case networks have 12 or 18 "left" people (P001 onwards). For every block size i from 2 up to that number, the left people are split into groups of i, and each group gets one "right" person who meets every member of the group. The left people form a cover of the right size. Greedy keeps picking the right people, which have the most links at each step.
