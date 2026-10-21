CREATE TABLE restocking_history_delivery (
    id bigserial PRIMARY KEY,
    restocking_id bigint NOT NULL UNIQUE,
    company_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    week_start date NOT NULL,
    payload text NOT NULL,
    delivered boolean NOT NULL DEFAULT false,
    next_attempt_at timestamp with time zone NOT NULL
);
CREATE INDEX idx_history_delivery_pending ON restocking_history_delivery(delivered, next_attempt_at);
CREATE TABLE restocking_history_bypass (
    id bigserial PRIMARY KEY,
    company_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    week_start date NOT NULL
);
CREATE INDEX idx_history_bypass_scope ON restocking_history_bypass(company_id, employee_id, week_start);
CREATE INDEX idx_restockings_employee_week ON restockings(employee_id, restocking_date DESC, id DESC);
