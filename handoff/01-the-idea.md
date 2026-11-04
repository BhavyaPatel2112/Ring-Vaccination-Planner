# 1. The idea

## The problem in plain words

When an outbreak starts (measles, mpox, Ebola), the health team knows two things:
- who is sick: the **confirmed cases**;
- who those people met: their **contacts**, found by contact tracing.

Vaccine doses and vaccinator time are limited.

**Ring vaccination** protects the people around each case, so the disease has nowhere to go:
- **Ring 1:** people who met a case directly.
- **Ring 2:** people who met someone in ring 1.

It was central to eradicating smallpox and was used in the 2015 Ebola ring vaccination trial in Guinea.

The question our app answers:

> Given the contact network and the confirmed cases, who exactly should be vaccinated so that every risky contact is protected, using as few doses as possible?

## Who uses it

- **District outbreak response teams and contact tracers**, who decide who to vaccinate first. This is the main user.
- **Members of the public**, who want to check whether they were possibly exposed at a place they visited.

## The algorithm insight

A risky contact link is protected if at least one of the two people on it is vaccinated. Choosing the fewest people so that every link has a vaccinated end is the **minimum vertex cover** problem. That problem is **NP-hard**: no fast method is known to always find the true minimum.

So the app does three things:
1. **Uses a fast method with a guarantee**, the 2-approximation by maximal matching. It always needs at most 2 times the minimum number of doses, and it runs in linear time.
2. **Compares it with a greedy baseline** ("most connected first"). Greedy often looks good, but it has no guarantee and can be badly fooled.
3. **Proves quality with an exact solver** (branch and bound) on small rings, and shows that the exact method's run time grows exponentially.

Two randomized tools cover the course's CO5 requirement:
- **Bloom filter:** fast "were you possibly at a venue with a case?" checks, and a "possibly already vaccinated" registry. Every hit is then confirmed with an exact check.
- **Randomized Quickselect:** finds percentiles fast, for example the top 10% most connected people when doses are short, or the median infections in a simulation.

## Modelling assumptions

1. A contact link is a possible route of transmission. Vaccinating at least one person on it blocks that route.
2. Confirmed cases are isolated, so they are not vaccinated. Their direct contacts (ring 1) are.
3. People already vaccinated are immune, so links touching them are already safe.
4. Ring depth is 2 by default (contacts, and contacts of contacts) and can be changed to 1 or 3.
5. Vaccination is treated as fully protective. The simulation can relax this with an efficacy setting.
6. Some people cannot be vaccinated: infants and pregnant or immunocompromised people (for live vaccines), and people who refuse. Their links must be protected through the other person. If both people on a link cannot be vaccinated, the link is flagged for quarantine or monitoring instead.

## Why this scores well on the rubric

- **Real problem:** a real public health strategy, with clear users and measurable outcomes.
- **At least two syllabus techniques, one from Units 4 to 6:** vertex cover approximation (Unit 5), NP-hardness and the exact solver (Unit 6), the Bloom filter and randomized Quickselect (Unit 6, CO5), median-of-medians (Unit 1), and disjoint-set union and the heap (Unit 2).
- **Everything written by us in Java:** the frontend only displays results.
- **Honest claims:** the approximation is never called optimal, and Bloom filter hits are always confirmed.
