package com.dairy.apipinal.dashboard;

import com.dairy.apipinal.TestcontainersConfiguration;
import com.dairy.apipinal.identity.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mesure reproductible du chargement du tableau de bord propriétaire :
 * temps de réponse et nombre d'instructions SQL des trois endpoints appelés
 * par DashboardView à l'ouverture. Exclu de "mvn test" (suffixe IT) ;
 * lancer avec : ./mvnw test -Dtest=DashboardLoadBenchmarkIT
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, DashboardLoadBenchmarkIT.StatementCountingConfiguration.class})
@ActiveProfiles({"test", "demo"})
class DashboardLoadBenchmarkIT {

    private static final AtomicLong STATEMENTS = new AtomicLong();

    private static final UUID DEMO_USER = UUID.fromString("b0000000-0000-0000-0000-000000000002");
    private static final UUID DEMO_EXPLOITATION = UUID.fromString("b0000000-0000-0000-0000-000000000003");

    private static final String[] ENDPOINTS = {
            "/api/v1/dashboard/summary",
            "/api/v1/dashboard/anomalies",
            "/api/v1/production/dashboard"
    };

    @Autowired private WebApplicationContext context;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void measureDashboardLoad() throws Exception {
        bench("demo (12 vaches)", DEMO_USER, DEMO_EXPLOITATION);

        UUID[] large = seedLargeFarm(80, 5);
        bench("ferme synthétique (80 vaches)", large[0], large[1]);
    }

    private void bench(String label, UUID userId, UUID exploitationId) throws Exception {
        String token = jwtTokenProvider.generateAccessToken(userId);
        for (String endpoint : ENDPOINTS) {
            for (int i = 0; i < 2; i++) {
                call(endpoint, token, exploitationId);
            }
            List<Long> timings = new ArrayList<>();
            long statements = 0;
            String body = null;
            for (int i = 0; i < 5; i++) {
                STATEMENTS.set(0);
                long start = System.nanoTime();
                body = call(endpoint, token, exploitationId);
                timings.add((System.nanoTime() - start) / 1_000_000);
                statements = STATEMENTS.get();
            }
            Collections.sort(timings);
            System.out.printf("BENCH|%s|%s|median_ms=%d|min_ms=%d|max_ms=%d|sql=%d%n",
                    label, endpoint, timings.get(2), timings.get(0), timings.get(4), statements);
            if (!endpoint.endsWith("/production/dashboard")) {
                System.out.printf("BODY|%s|%s|%s%n", label, endpoint, body);
            } else {
                System.out.printf("BODY|%s|%s|%s%n", label, endpoint,
                        body.replaceAll("\"history\":\\[.*", "\"history\":<omis>"));
            }
        }
    }

    private String call(String endpoint, String token, UUID exploitationId) throws Exception {
        return mockMvc.perform(get(endpoint)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Ferme réaliste : N vaches en lactation, 2 traites/jour sur 45 jours, une ration de L lignes par vache. */
    private UUID[] seedLargeFarm(int animals, int lines) {
        UUID tenant = UUID.randomUUID();
        UUID exploitation = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO tenant (id, nom, statut, created_at, updated_at, version) VALUES (?, 'Bench', 'ACTIF', NOW(), NOW(), 0)", tenant);
        jdbcTemplate.update("INSERT INTO exploitation (id, tenant_id, nom, localite, actif, created_at, updated_at, version) VALUES (?, ?, 'Ferme Bench', 'Thies', true, NOW(), NOW(), 0)", exploitation, tenant);
        jdbcTemplate.update("INSERT INTO utilisateur (id, nom, prenom, statut, telephone, created_at, updated_at, version) VALUES (?, 'Bench', 'Owner', 'ACTIF', ?, NOW(), NOW(), 0)",
                user, "+22178" + (System.nanoTime() % 10_000_000L));
        jdbcTemplate.update("INSERT INTO membership (id, utilisateur_id, exploitation_id, role, created_at, updated_at, version) VALUES (?, ?, ?, 'PROPRIETAIRE', NOW(), NOW(), 0)",
                UUID.randomUUID(), user, exploitation);
        jdbcTemplate.update("INSERT INTO prix_vente_lait (id, tenant_id, exploitation_id, prix_par_litre, date_debut, date_fin, version) VALUES (?, ?, ?, 600, CURRENT_DATE - 90, NULL, 0)",
                UUID.randomUUID(), tenant, exploitation);

        jdbcTemplate.update("""
                INSERT INTO animal (id, tenant_id, exploitation_id, race_id, identifiant, nom, date_naissance, statut, sexe, created_at, updated_at, version)
                SELECT md5(?::text || '|animal|' || g)::uuid, ?, ?, (SELECT id FROM race ORDER BY code LIMIT 1),
                       'B-' || g, 'Vache ' || g, DATE '2020-01-01', 'ACTIF', 'FEMELLE', NOW(), NOW(), 0
                FROM generate_series(1, ?) g
                """, exploitation.toString(), tenant, exploitation, animals);
        jdbcTemplate.update("""
                INSERT INTO lactation (id, tenant_id, animal_id, exploitation_id, date_debut, statut, created_at, updated_at, version)
                SELECT md5(?::text || '|lactation|' || g)::uuid, ?, md5(?::text || '|animal|' || g)::uuid, ?,
                       CURRENT_DATE - 120, 'EN_COURS', NOW(), NOW(), 0
                FROM generate_series(1, ?) g
                """, exploitation.toString(), tenant, exploitation.toString(), exploitation, animals);
        jdbcTemplate.update("""
                INSERT INTO traite (id, tenant_id, lactation_id, exploitation_id, auteur_id, date_heure, type, quantite_litres, date_heure_saisie, version)
                SELECT gen_random_uuid(), ?, md5(?::text || '|lactation|' || g)::uuid, ?, ?,
                       d + s.h, s.t, 8 + (g % 7) + (EXTRACT(DOY FROM d)::int % 3), d + s.h, 0
                FROM generate_series(1, ?) g
                CROSS JOIN generate_series(CURRENT_DATE - 45, CURRENT_DATE, INTERVAL '1 day') d
                CROSS JOIN (VALUES ('MATIN', INTERVAL '6 hours'), ('SOIR', INTERVAL '18 hours')) s(t, h)
                """, tenant, exploitation.toString(), exploitation, user, animals);
        jdbcTemplate.update("""
                INSERT INTO ration (id, animal_id, tenant_id, exploitation_id, date_debut, date_fin, statut, origine, version)
                SELECT md5(?::text || '|ration|' || g)::uuid, md5(?::text || '|animal|' || g)::uuid, ?, ?,
                       CURRENT_DATE - 60, NULL, 'ACTIVE', 'ACTUELLE', 0
                FROM generate_series(1, ?) g
                """, exploitation.toString(), exploitation.toString(), tenant, exploitation, animals);
        jdbcTemplate.update("""
                INSERT INTO ligne_ration (id, ration_id, aliment_id, quantite)
                SELECT gen_random_uuid(), md5(?::text || '|ration|' || g)::uuid, a.id, 1 + (g % 5)
                FROM generate_series(1, ?) g
                CROSS JOIN (SELECT id FROM aliment ORDER BY code LIMIT ?) a
                """, exploitation.toString(), animals, lines);
        return new UUID[]{user, exploitation};
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class StatementCountingConfiguration {

        @Bean
        static BeanPostProcessor statementCountingDataSource() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof DataSource ds && !(bean instanceof DelegatingDataSource)) {
                        return new DelegatingDataSource(ds) {
                            @Override
                            public Connection getConnection() throws SQLException {
                                return counting(super.getConnection());
                            }
                        };
                    }
                    return bean;
                }
            };
        }

        private static Connection counting(Connection target) {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if (name.equals("prepareStatement") || name.equals("createStatement") || name.equals("prepareCall")) {
                            STATEMENTS.incrementAndGet();
                        }
                        try {
                            return method.invoke(target, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
        }
    }
}
