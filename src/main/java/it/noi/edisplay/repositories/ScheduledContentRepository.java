// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.noi.edisplay.repositories;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.noi.edisplay.model.ScheduledContent;

public interface ScheduledContentRepository extends JpaRepository<ScheduledContent, Long> {

    ScheduledContent findByUuid(String uuid);

    List<ScheduledContent> findByDisplayId(Long displayId);

    ScheduledContent findByDisplayIdAndEventId(Long displayId, String eventId);

    // Find content by event ID
    ScheduledContent findByEventId(String eventId);

    /**
     * Find expired events for cleanup
     * This includes events that have ended before cutoff date
     * AND events that are no longer in the current OpenDataHub data (not in the currectEventIds list)
     * 
     * @param cutoffDate Date before which events are considered expired
     * @param currentEventIds List of event IDs currently in OpenDataHub
     * @return List of scheduled content to be removed
     */
    @Query("SELECT sc FROM ScheduledContent sc WHERE " + "(sc.endDate < :cutoffDate) OR " + "(sc.eventId NOT IN :currentEventIds)")
    List<ScheduledContent> findExpiredEvents(
        @Param("cutoffDate") Date cutoffDate,
        @Param("currentEventIds") List<String> currentEventIds
    );


    List<ScheduledContent> findAll();
}
