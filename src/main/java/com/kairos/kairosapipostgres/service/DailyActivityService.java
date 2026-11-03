package com.kairos.kairosapipostgres.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Identity comes from a verified JWT; companies come exclusively from database links. */
@Service
public class DailyActivityService {
    private final JdbcTemplate jdbc;

    public DailyActivityService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 3)
    public void record(long authenticatedUserId) {
        int userId = Math.toIntExact(authenticatedUserId);
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid authenticated identity");
        }
        jdbc.query("""
                SELECT public.register_user_access(?, authorized.company_id)
                FROM (
                    SELECT company_id FROM public.employees WHERE users_id = ?
                    UNION
                    SELECT company_id FROM public.customers WHERE users_id = ?
                ) authorized
                """, rs -> { }, userId, userId, userId);
    }
}
