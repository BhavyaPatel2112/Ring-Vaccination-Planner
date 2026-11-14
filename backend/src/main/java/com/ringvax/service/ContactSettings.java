package com.ringvax.service;

import java.time.LocalDate;

import com.ringvax.data.ConfirmedCase;
import com.ringvax.engine.followup.ContactDefinition;
import com.ringvax.engine.graph.Contact;

/** Builds the contact definition (feature E) for a stored network. */
final class ContactSettings {

    private ContactSettings() {
    }

    /**
     * The contact definition for these settings. The window ends on the
     * {@link #referenceDate(StoredNetwork) reference date}. Null settings mean "no limit".
     */
    static ContactDefinition resolve(StoredNetwork network, Integer minDurationMinutes, Integer windowDays) {
        if (minDurationMinutes == null && windowDays == null) {
            return ContactDefinition.ALL;
        }
        return new ContactDefinition(minDurationMinutes, windowDays, referenceDate(network));
    }

    /**
     * The latest confirmation date in cases.csv; without one, the latest contact
     * date in the network; without any dates, today.
     */
    static LocalDate referenceDate(StoredNetwork network) {
        LocalDate latest = null;
        for (ConfirmedCase c : network.cases()) {
            if (c.confirmedDate() != null && (latest == null || c.confirmedDate().isAfter(latest))) {
                latest = c.confirmedDate();
            }
        }
        if (latest != null) {
            return latest;
        }
        for (Contact c : network.graph().contacts()) {
            if (c.date() != null && (latest == null || c.date().isAfter(latest))) {
                latest = c.date();
            }
        }
        return latest != null ? latest : LocalDate.now();
    }
}
