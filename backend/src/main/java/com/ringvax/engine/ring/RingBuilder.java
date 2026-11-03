package com.ringvax.engine.ring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;
import com.ringvax.engine.graph.UnknownPersonException;

/**
 * Builds the vaccination rings around confirmed cases with a multi-source
 * breadth-first search.
 *
 * <ul>
 *   <li><b>Ring 1</b>: direct contacts of a case. Everyone here who can be
 *       vaccinated is vaccinated (mandatory, as in real ring vaccination).</li>
 *   <li><b>Ring 2</b> (and deeper rings up to the depth): contacts of the
 *       previous ring who are not already in a closer ring.</li>
 *   <li><b>Target links</b>: links with at least one endpoint in ring 2 or
 *       beyond, where neither endpoint is a case (cases are isolated), already
 *       vaccinated, or a ring 1 member who will be vaccinated. A ring 1 member who
 *       cannot be vaccinated does not protect their links, so those links stay in
 *       the target set and the cover has to use the other endpoint.</li>
 * </ul>
 *
 * <p>An optional contact filter decides which contacts count at all (used by
 * the contact definition settings, for example "at least 15 minutes within the
 * last 14 days"). Filtered-out contacts are ignored for both rings and targets.
 *
 * <p>Time complexity: O(V + E) for the search plus O(sum of degrees of ring
 * members) for the target links. Space complexity: O(V).
 */
public class RingBuilder {

    public static final int DEFAULT_DEPTH = 2;
    public static final int MAX_DEPTH = 5;

    private final ContactGraph graph;
    private final Predicate<Contact> contactFilter;

    public RingBuilder(ContactGraph graph) {
        this(graph, contact -> true);
    }

    public RingBuilder(ContactGraph graph, Predicate<Contact> contactFilter) {
        this.graph = graph;
        this.contactFilter = contactFilter;
    }

    /**
     * Builds the rings for the given case ids.
     *
     * @throws UnknownPersonException   if an id is not in the network
     * @throws IllegalArgumentException if there are no cases or the depth is out of range
     */
    public RingResult build(Collection<String> caseIds, int depth) {
        int[] cases = new int[caseIds.size()];
        int i = 0;
        for (String id : caseIds) {
            int index = graph.indexOf(id);
            if (index < 0) {
                throw new UnknownPersonException(id);
            }
            cases[i++] = index;
        }
        return buildFromIndices(cases, depth);
    }

    /** Same as {@link #build(Collection, int)} but with person indices. */
    public RingResult buildFromIndices(int[] caseIndices, int depth) {
        if (depth < 1 || depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Ring depth must be between 1 and " + MAX_DEPTH + ", got " + depth);
        }
        int[] cases = Arrays.stream(caseIndices).distinct().sorted().toArray();
        if (cases.length == 0) {
            throw new IllegalArgumentException("At least one confirmed case is required");
        }
        int n = graph.personCount();
        for (int c : cases) {
            if (c < 0 || c >= n) {
                throw new IllegalArgumentException("Case index " + c + " is out of range");
            }
        }

        int[] level = new int[n];
        Arrays.fill(level, RingResult.OUTSIDE);
        int[] queue = new int[n];
        int head = 0;
        int tail = 0;
        for (int c : cases) {
            level[c] = 0;
            queue[tail++] = c;
        }
        while (head < tail) {
            int u = queue[head++];
            if (level[u] == depth) {
                continue;
            }
            for (int i = 0; i < graph.degree(u); i++) {
                if (!counts(graph.contactAt(u, i))) {
                    continue;
                }
                int v = graph.neighbour(u, i);
                if (level[v] == RingResult.OUTSIDE) {
                    level[v] = level[u] + 1;
                    queue[tail++] = v;
                }
            }
        }

        List<List<Integer>> ringLists = new ArrayList<>();
        for (int k = 0; k < depth; k++) {
            ringLists.add(new ArrayList<>());
        }
        List<Integer> mandatory = new ArrayList<>();
        List<Integer> vaccinated = new ArrayList<>();
        List<Integer> cannot = new ArrayList<>();
        for (int p = 0; p < n; p++) {
            if (level[p] < 1) {
                continue;
            }
            ringLists.get(level[p] - 1).add(p);
            Person person = graph.person(p);
            if (person.vaccinated()) {
                vaccinated.add(p);
            } else if (!person.canBeVaccinated()) {
                cannot.add(p);
            } else if (level[p] == 1) {
                mandatory.add(p);
            }
        }

        List<Integer> targets = new ArrayList<>();
        for (int k = 2; k <= depth; k++) {
            for (int u : ringLists.get(k - 1)) {
                for (int i = 0; i < graph.degree(u); i++) {
                    int contact = graph.contactAt(u, i);
                    int v = graph.neighbour(u, i);
                    // A link between two deep-ring people is found from both ends; keep it once.
                    if (level[v] >= 2 && v < u) {
                        continue;
                    }
                    if (counts(contact) && !isProtected(u, level) && !isProtected(v, level)) {
                        targets.add(contact);
                    }
                }
            }
        }
        targets.sort(null);

        List<Integer> isolatedCases = new ArrayList<>();
        for (int c : cases) {
            boolean hasContact = false;
            for (int i = 0; i < graph.degree(c) && !hasContact; i++) {
                hasContact = counts(graph.contactAt(c, i));
            }
            if (!hasContact) {
                isolatedCases.add(c);
            }
        }

        List<int[]> rings = new ArrayList<>();
        for (List<Integer> ring : ringLists) {
            rings.add(toArray(ring));
        }
        return new RingResult(depth, cases, level, rings, toArray(mandatory), toArray(vaccinated),
                toArray(cannot), toArray(targets), toArray(isolatedCases));
    }

    private boolean counts(int contactIndex) {
        return contactFilter.test(graph.contact(contactIndex));
    }

    /** A person whose links need no protection: a case, immune, or vaccinated as ring 1. */
    private boolean isProtected(int person, int[] level) {
        Person p = graph.person(person);
        return level[person] == 0 || p.vaccinated() || (level[person] == 1 && p.canBeVaccinated());
    }

    private static int[] toArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
