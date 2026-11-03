package com.ringvax.engine.graph;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Undirected contact network: people are vertices, contact links are edges.
 *
 * <p>Every person gets a dense integer index (0 to V - 1) in the order they are
 * added, so the algorithms can use plain arrays instead of maps. Each person keeps
 * an adjacency list of (neighbour index, contact index) pairs in growable
 * {@code int} arrays, which avoids boxing and keeps a 1,000,000-contact network
 * small in memory.
 *
 * <p>The graph does not reject duplicate links by itself because checking would
 * cost O(degree) per insert; the CSV parser and the generator remove duplicates
 * before adding. Use {@link #hasContact(int, int)} when a single check is needed,
 * for example for a live contact update.
 *
 * <p>Time complexity: adding a person or a contact is amortised O(1); index
 * lookup by id is O(1) expected; {@link #hasContact(int, int)} is
 * O(min(deg(a), deg(b))). Space complexity: O(V + E).
 */
public class ContactGraph {

    private static final int INITIAL_ADJ_CAPACITY = 4;

    private final List<Person> people = new ArrayList<>();
    private final Map<String, Integer> indexById = new HashMap<>();
    private final List<Contact> contacts = new ArrayList<>();

    private int[][] adjNeighbour = new int[16][];
    private int[][] adjContact = new int[16][];
    private int[] degree = new int[16];

    /**
     * Adds a person and returns their index.
     *
     * @throws IllegalArgumentException if a person with the same id already exists
     */
    public int addPerson(Person person) {
        if (indexById.containsKey(person.id())) {
            throw new IllegalArgumentException("Duplicate person id: " + person.id());
        }
        int index = people.size();
        ensurePersonCapacity(index + 1);
        people.add(person);
        indexById.put(person.id(), index);
        adjNeighbour[index] = new int[INITIAL_ADJ_CAPACITY];
        adjContact[index] = new int[INITIAL_ADJ_CAPACITY];
        return index;
    }

    /**
     * Adds an undirected contact between two existing people and returns the
     * contact's index.
     *
     * @throws IllegalArgumentException if either index is out of range or a == b
     */
    public int addContact(int a, int b, LocalDate date, int durationMinutes) {
        checkIndex(a);
        checkIndex(b);
        Contact contact = new Contact(a, b, date, durationMinutes);
        int id = contacts.size();
        contacts.add(contact);
        append(a, b, id);
        append(b, a, id);
        return id;
    }

    /** Number of people (V). */
    public int personCount() {
        return people.size();
    }

    /** Number of contact links (E). */
    public int contactCount() {
        return contacts.size();
    }

    public Person person(int index) {
        checkIndex(index);
        return people.get(index);
    }

    /** Returns the index of the person with this id, or -1 if there is none. */
    public int indexOf(String personId) {
        Integer index = indexById.get(personId);
        return index == null ? -1 : index;
    }

    public boolean containsPerson(String personId) {
        return indexById.containsKey(personId);
    }

    public Contact contact(int contactIndex) {
        return contacts.get(contactIndex);
    }

    /** Read-only view of all people in index order. */
    public List<Person> people() {
        return Collections.unmodifiableList(people);
    }

    /** Read-only view of all contacts in insertion order. */
    public List<Contact> contacts() {
        return Collections.unmodifiableList(contacts);
    }

    public int degree(int person) {
        checkIndex(person);
        return degree[person];
    }

    /** The i-th neighbour of {@code person}, for 0 &lt;= i &lt; degree(person). */
    public int neighbour(int person, int i) {
        checkSlot(person, i);
        return adjNeighbour[person][i];
    }

    /** Index of the contact that links {@code person} to its i-th neighbour. */
    public int contactAt(int person, int i) {
        checkSlot(person, i);
        return adjContact[person][i];
    }

    /** Copy of the neighbour indices of {@code person}. O(degree). */
    public int[] neighbours(int person) {
        checkIndex(person);
        return Arrays.copyOf(adjNeighbour[person], degree[person]);
    }

    /** True if a contact between a and b exists. O(min(deg(a), deg(b))). */
    public boolean hasContact(int a, int b) {
        checkIndex(a);
        checkIndex(b);
        int from = degree[a] <= degree[b] ? a : b;
        int to = from == a ? b : a;
        int[] list = adjNeighbour[from];
        for (int i = 0; i < degree[from]; i++) {
            if (list[i] == to) {
                return true;
            }
        }
        return false;
    }

    private void append(int person, int neighbour, int contactId) {
        int d = degree[person];
        if (d == adjNeighbour[person].length) {
            int newLength = d * 2;
            adjNeighbour[person] = Arrays.copyOf(adjNeighbour[person], newLength);
            adjContact[person] = Arrays.copyOf(adjContact[person], newLength);
        }
        adjNeighbour[person][d] = neighbour;
        adjContact[person][d] = contactId;
        degree[person] = d + 1;
    }

    private void ensurePersonCapacity(int needed) {
        if (needed > degree.length) {
            int newLength = Math.max(needed, degree.length * 2);
            adjNeighbour = Arrays.copyOf(adjNeighbour, newLength);
            adjContact = Arrays.copyOf(adjContact, newLength);
            degree = Arrays.copyOf(degree, newLength);
        }
    }

    private void checkIndex(int person) {
        if (person < 0 || person >= people.size()) {
            throw new IndexOutOfBoundsException(
                    "Person index " + person + " out of range 0.." + (people.size() - 1));
        }
    }

    private void checkSlot(int person, int i) {
        checkIndex(person);
        if (i < 0 || i >= degree[person]) {
            throw new IndexOutOfBoundsException(
                    "Neighbour slot " + i + " out of range for degree " + degree[person]);
        }
    }
}
