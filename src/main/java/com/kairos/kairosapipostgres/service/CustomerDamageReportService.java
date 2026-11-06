package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.CustomerDamageReportRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.MessageDigest;
import java.util.*;

@Service
public class CustomerDamageReportService {
    private final JdbcTemplate db;

    public CustomerDamageReportService(JdbcTemplate db) {
        this.db = db;
    }

    private record Actor(long customerId, long companyId, String companyName) { }

    private Actor actor(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .noneMatch(role -> "ROLE_CUSTOMER".equals(role.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var rows = db.queryForList("""
                SELECT c.id,c.company_id,co.name FROM customers c JOIN users u ON u.id=c.users_id
                JOIN companies co ON co.id=c.company_id WHERE u.email=?
                """, auth.getName());
        if (rows.size() != 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var row = rows.getFirst();
        return new Actor(number(row, "id"), number(row, "company_id"), row.get("name").toString());
    }

    private static long number(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).longValue();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> capabilities(Authentication auth) {
        var owner = actor(auth);
        return Map.of("version", 1, "photoAvailable", true, "companyId", owner.companyId(),
                "companyName", owner.companyName());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> options(Authentication auth) {
        var owner = actor(auth);
        var products = db.queryForList("""
                SELECT id,name,COALESCE(brand,'') brand FROM products
                WHERE company_id=? ORDER BY name,id LIMIT 1001
                """, owner.companyId());
        boolean truncated = products.size() > 1000;
        if (truncated) {
            products = new ArrayList<>(products.subList(0, 1000));
        }
        var locations = db.queryForList("""
                SELECT ps.product_id,sh.id shelf_id,a.position aisle,s.name sector
                FROM product_shelves ps JOIN products p ON p.id=ps.product_id
                JOIN shelves sh ON sh.id=ps.shelf_id JOIN aisles a ON a.id=sh.aisle_id
                JOIN sectors s ON s.id=a.sector_id
                WHERE p.company_id=? AND s.company_id=?
                ORDER BY ps.product_id,sh.id LIMIT 10001
                """, owner.companyId(), owner.companyId());
        boolean locationsTruncated = locations.size() > 10000;
        if (locationsTruncated) {
            locations = new ArrayList<>(locations.subList(0, 10000));
        }
        return Map.of("companyId", owner.companyId(), "companyName", owner.companyName(),
                "products", products, "locations", locations, "catalogTruncated", truncated,
                "locationsTruncated", locationsTruncated, "photoAvailable", true);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Authentication auth) {
        var owner = actor(auth);
        return db.queryForList("""
                SELECT r.id,r.product_id,p.name,r.problem_type,r.description,a.status,
                CAST(r.created_at AS varchar) created_at,CAST(a.resolution_date AS varchar) resolution_date,
                (r.photo IS NOT NULL) has_photo
                FROM customer_damage_reports r JOIN products p ON p.id=r.product_id
                JOIN alerts a ON a.id=r.alert_id
                WHERE r.customer_id=? AND r.company_id=? ORDER BY r.created_at DESC,r.id DESC LIMIT 100
                """, owner.customerId(), owner.companyId());
    }

    @Transactional
    public Map<String, Object> report(Authentication auth, CustomerDamageReportRequest request) {
        var owner = actor(auth);
        byte[] photo = photo(request.imageBase64());
        String fingerprint = fingerprint(request, photo);
        // Serialize retries for the same customer across all API instances.
        db.queryForList("SELECT id FROM customers WHERE id=? FOR UPDATE", owner.customerId());
        var previous = db.queryForList("""
                SELECT r.id,r.fingerprint,a.status,r.alert_id FROM customer_damage_reports r
                JOIN alerts a ON a.id=r.alert_id WHERE r.customer_id=? AND r.command_id=?
                """, owner.customerId(), request.commandId());
        if (!previous.isEmpty()) {
            var row = previous.getFirst();
            if (!fingerprint.equals(row.get("fingerprint"))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Comando ja utilizado com dados diferentes");
            }
            return confirmation(row);
        }
        if (db.queryForList("SELECT id FROM products WHERE id=? AND company_id=?",
                request.productId(), owner.companyId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        UUID protocol = UUID.randomUUID();
        try {
            db.update("""
                    INSERT INTO customer_damage_reports
                    (id,command_id,customer_id,company_id,product_id,shelf_id,problem_type,description,photo,fingerprint)
                    VALUES (?,?,?,?,?,?,?,?,?,?)
                    """, protocol, request.commandId(), owner.customerId(), owner.companyId(),
                    request.productId(), request.shelfId(), request.problemType(), request.description().trim(), photo, fingerprint);
        } catch (DataIntegrityViolationException error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nao foi possivel encaminhar o relato. Confira a gondola e o repositor responsavel.");
        }
        return confirmation(db.queryForMap("""
                SELECT r.id,r.alert_id,a.status FROM customer_damage_reports r
                JOIN alerts a ON a.id=r.alert_id WHERE r.id=?
                """, protocol));
    }

    private static Map<String, Object> confirmation(Map<String, Object> row) {
        return Map.of("protocol", row.get("id").toString(), "status", row.get("status"), "alertId", row.get("alert_id"));
    }

    private static String fingerprint(CustomerDamageReportRequest request, byte[] photo) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var data = new DataOutputStream(bytes)) {
                data.writeLong(request.productId());
                data.writeLong(request.shelfId() == null ? 0 : request.shelfId());
                data.writeUTF(request.problemType());
                data.writeUTF(request.description().trim());
                if (photo != null) {
                    data.write(photo);
                }
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("Unable to fingerprint report", error);
        }
    }

    private static byte[] photo(String encoded) {
        if (encoded == null) {
            return null;
        }
        if (encoded.isBlank() || encoded.length() > 4194304) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Foto JPEG invalida");
        }
        try {
            byte[] original = Base64.getDecoder().decode(encoded);
            if (original.length < 2 || original.length > 3145728) {
                throw new IllegalArgumentException();
            }
            try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
                var readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) {
                    throw new IllegalArgumentException();
                }
                var reader = readers.next();
                try {
                    reader.setInput(stream);
                    if (!reader.getFormatName().equalsIgnoreCase("JPEG")
                            || (long) reader.getWidth(0) * reader.getHeight(0) > 4000000) {
                        throw new IllegalArgumentException();
                    }
                    var image = reader.read(0);
                    try (var output = new ByteArrayOutputStream()) {
                        if (!ImageIO.write(image, "jpeg", output) || output.size() > 3145728) {
                            throw new IllegalArgumentException();
                        }
                        return output.toByteArray();
                    } finally {
                        image.flush();
                    }
                } finally {
                    reader.dispose();
                }
            }
        } catch (java.io.IOException | IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Foto JPEG invalida");
        }
    }
}
