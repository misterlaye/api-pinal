package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Remplace les PIN stockés en clair par leur empreinte BCrypt.
 * <ul>
 *     <li>utilisateur ouvrier (adhésion EMPLOYE ou GERANT) : le PIN existant est conservé, haché ;</li>
 *     <li>autre utilisateur (propriétaire uniquement) : le PIN est supprimé, la connexion se fait par OTP.</li>
 * </ul>
 */
public class V28__hash_existing_pins extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        record PinRow(UUID id, String pin, boolean worker) {}
        List<PinRow> rows = new ArrayList<>();

        try (Statement select = context.getConnection().createStatement();
             ResultSet rs = select.executeQuery("""
                     SELECT u.id, u.pin_hash,
                            EXISTS (SELECT 1 FROM membership m
                                    WHERE m.utilisateur_id = u.id AND m.role IN ('EMPLOYE', 'GERANT')) AS worker
                     FROM utilisateur u
                     WHERE u.pin_hash IS NOT NULL
                     """)) {
            while (rs.next()) {
                rows.add(new PinRow(rs.getObject("id", UUID.class), rs.getString("pin_hash"), rs.getBoolean("worker")));
            }
        }

        try (PreparedStatement update = context.getConnection()
                .prepareStatement("UPDATE utilisateur SET pin_hash = ? WHERE id = ?")) {
            for (PinRow row : rows) {
                boolean plainPin = row.pin() != null && row.pin().matches("[0-9]{4,6}");
                update.setString(1, row.worker() && plainPin ? encoder.encode(row.pin()) : null);
                update.setObject(2, row.id());
                update.addBatch();
            }
            update.executeBatch();
        }
    }
}
