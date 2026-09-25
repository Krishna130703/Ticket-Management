package com.ttn.ticket_api.ticket.repository;

import com.ttn.ticket_api.ticket.entity.Ticket;
import com.ttn.ticket_api.ticket.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.comments c WHERE t.id = :id ORDER BY c.createdAt ASC")
    Optional<Ticket> findByIdWithComments(@Param("id") Long id);

    @Query("""
            SELECT t FROM Ticket t
            WHERE (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:status IS NULL OR t.status = :status)
            ORDER BY t.createdAt DESC
            """)
    List<Ticket> findByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") TicketStatus status);
}
