package com.pdfsign.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Crypto crypto = new Crypto();
    private String publicBaseUrl = "http://localhost:5173";
    private String storagePath = "./storage";
    private int magicTokenTtlDays = 7;
    private Cors cors = new Cors();
    private Mail mail = new Mail();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Crypto getCrypto() {
        return crypto;
    }

    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public int getMagicTokenTtlDays() {
        return magicTokenTtlDays;
    }

    public void setMagicTokenTtlDays(int magicTokenTtlDays) {
        this.magicTokenTtlDays = magicTokenTtlDays;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public static class Jwt {
        private String secret;
        private long accessExpirationMs = 3_600_000;
        private long refreshExpirationMs = 2_592_000_000L;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessExpirationMs() {
            return accessExpirationMs;
        }

        public void setAccessExpirationMs(long accessExpirationMs) {
            this.accessExpirationMs = accessExpirationMs;
        }

        public long getRefreshExpirationMs() {
            return refreshExpirationMs;
        }

        public void setRefreshExpirationMs(long refreshExpirationMs) {
            this.refreshExpirationMs = refreshExpirationMs;
        }
    }

    public static class Crypto {
        private String secret;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    public static class Cors {
        private String allowedOrigins = "*";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Mail {
        private String from = "noreply@pdfsign.local";
        private boolean logOnly;

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public boolean isLogOnly() {
            return logOnly;
        }

        public void setLogOnly(boolean logOnly) {
            this.logOnly = logOnly;
        }
    }
}
