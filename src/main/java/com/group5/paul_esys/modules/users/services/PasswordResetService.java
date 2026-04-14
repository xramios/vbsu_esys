package com.group5.paul_esys.modules.users.services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PasswordResetService {

    private static final Logger logger = Logger.getLogger(PasswordResetService.class.getName());
    private static final int TOKEN_EXPIRATION_MINUTES = 15;

    public String generateResetToken(String email) {
        String token = String.format("%06d", new Random().nextInt(999999));
        
        try (Connection conn = ConnectionService.getConnection()) {
            // Find user id by email
            Long userId = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE email = ?")) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getLong("id");
                    }
                }
            }

            if (userId == null) {
                return null;
            }

            // Insert token
            String sql = "INSERT INTO password_reset_tokens (user_id, token, expires_at) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.setString(2, token);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES)));
                ps.executeUpdate();
            }

            logger.info("Generated reset token for user " + userId + ": " + token);
            return token;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error generating reset token", e);
            return null;
        }
    }

    public boolean validateToken(String email, String token) {
        try (Connection conn = ConnectionService.getConnection()) {
            String sql = "SELECT t.id FROM password_reset_tokens t " +
                         "JOIN users u ON t.user_id = u.id " +
                         "WHERE u.email = ? AND t.token = ? AND t.expires_at > ? AND t.used_at IS NULL";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                ps.setString(2, token);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error validating token", e);
            return false;
        }
    }

    public boolean resetPassword(String email, String token, String newPassword) {
        try (Connection conn = ConnectionService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Validate token again
                Long tokenId = null;
                Long userId = null;
                String sqlFind = "SELECT t.id, t.user_id FROM password_reset_tokens t " +
                                 "JOIN users u ON t.user_id = u.id " +
                                 "WHERE u.email = ? AND t.token = ? AND t.expires_at > ? AND t.used_at IS NULL";
                
                try (PreparedStatement ps = conn.prepareStatement(sqlFind)) {
                    ps.setString(1, email);
                    ps.setString(2, token);
                    ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            tokenId = rs.getLong("id");
                            userId = rs.getLong("user_id");
                        }
                    }
                }

                if (tokenId == null) {
                    return false;
                }

                // 2. Update password
                String hashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
                try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET password = ? WHERE id = ?")) {
                    ps.setString(1, hashedPassword);
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                }

                // 3. Mark token as used
                try (PreparedStatement ps = conn.prepareStatement("UPDATE password_reset_tokens SET used_at = ? WHERE id = ?")) {
                    ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setLong(2, tokenId);
                    ps.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error resetting password", e);
            return false;
        }
    }
}
