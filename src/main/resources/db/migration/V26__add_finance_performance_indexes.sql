-- ================================================================
-- V26: Add performance indexes for Finance Dashboard
-- ================================================================
-- These indexes dramatically improve dashboard query performance
-- by optimizing date-range and tenant-filtered queries
-- ================================================================

-- Index for charge_exploitation queries with date range
-- Supports: SELECT * FROM charge_exploitation WHERE tenant_id = ? AND date >= ? AND date < ? ORDER BY date DESC
CREATE INDEX IF NOT EXISTS idx_charge_tenant_date 
    ON charge_exploitation(tenant_id, date DESC)
    WHERE date IS NOT NULL;

-- Index for calcul_rentabilite snapshot retrieval
-- Supports: SELECT * FROM calcul_rentabilite WHERE tenant_id = ? ORDER BY date_calcul DESC LIMIT 4
CREATE INDEX IF NOT EXISTS idx_rentabilite_tenant_date_calcul 
    ON calcul_rentabilite(tenant_id, date_calcul DESC)
    WHERE date_calcul IS NOT NULL;

-- Index for calcul_rentabilite month filtering
-- Supports: filtering by periode_debut for historical chart data
CREATE INDEX IF NOT EXISTS idx_rentabilite_tenant_periode 
    ON calcul_rentabilite(tenant_id, periode_debut)
    WHERE periode_debut IS NOT NULL;

-- Index for traite queries with date range (used by rentabiliteService.calculateLive)
-- Supports: Production queries filtered by tenant and date
CREATE INDEX IF NOT EXISTS idx_traite_tenant_date 
    ON traite(tenant_id, date_heure DESC)
    WHERE date_heure IS NOT NULL;

-- Note: vente_lait table doesn't exist in current schema
-- Revenue calculation uses prix_vente_lait configuration instead

