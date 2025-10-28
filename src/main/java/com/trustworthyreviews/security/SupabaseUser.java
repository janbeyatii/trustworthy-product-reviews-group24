package com.trustworthyreviews.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public class SupabaseUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String email;
    private final Map<String, Object> metadata;

    public SupabaseUser(String id, String email, Map<String, Object> metadata) {
        this.id = id;
        this.email = email;
        this.metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
