package com.rms.master;

import com.rms.common.ApiException;
import com.rms.common.Snap;
import com.rms.domain.RefLookupType;
import com.rms.domain.RefLookupValue;
import com.rms.repository.RefLookupTypeRepository;
import com.rms.repository.RefLookupValueRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LookupService {
    private final RefLookupValueRepository values;
    private final RefLookupTypeRepository types;
    private final Map<String, Snap> byKey = new ConcurrentHashMap<>();
    private final Map<Long, Snap> byIdCache = new ConcurrentHashMap<>();
    private final Map<Long, Long> typeOfValue = new ConcurrentHashMap<>();
    private final Map<String, Long> typeIds = new ConcurrentHashMap<>();

    public LookupService(RefLookupValueRepository values, RefLookupTypeRepository types) {
        this.values = values;
        this.types = types;
    }

    /** Resolve a lookup value by type code + value code, e.g. ("TREATY_STATUS", "DRAFT"). */
    public Snap snap(String typeCode, String code) {
        String key = typeCode + ":" + code;
        Snap cached = byKey.get(key);
        if (cached != null) return cached;
        RefLookupValue v = values.findByTypeCodeAndCode(typeCode, code)
            .orElseThrow(() -> ApiException.badRequest("LOOKUP_NOT_FOUND", "Lookup value " + key + " is not configured"));
        Snap s = new Snap(v.getId(), v.getCode(), v.getDescription());
        byKey.put(key, s);
        return s;
    }

    public Snap byId(Long id) {
        if (id == null) throw ApiException.badRequest("LOOKUP_REQUIRED", "A lookup value is required");
        Snap cached = byIdCache.get(id);
        if (cached != null) return cached;
        RefLookupValue v = values.findById(id)
            .orElseThrow(() -> ApiException.badRequest("LOOKUP_NOT_FOUND", "Lookup value id " + id + " does not exist"));
        typeOfValue.put(id, v.getTypeId());
        Snap s = new Snap(v.getId(), v.getCode(), v.getDescription());
        byIdCache.put(id, s);
        return s;
    }

    /** Same as byId but verifies the value belongs to the expected lookup type. */
    public Snap byIdOfType(String typeCode, Long id) {
        Snap s = byId(id);
        Long expected = typeIds.computeIfAbsent(typeCode, c -> types.findByCode(c).map(RefLookupType::getId)
            .orElseThrow(() -> ApiException.badRequest("LOOKUP_TYPE_NOT_FOUND", "Lookup type " + c + " is not configured")));
        if (!expected.equals(typeOfValue.get(id))) {
            throw ApiException.badRequest("LOOKUP_WRONG_TYPE", "Value " + s.code() + " is not a " + typeCode + " value");
        }
        return s;
    }

    public List<RefLookupValue> list(String typeCode) {
        return values.findActiveByType(typeCode);
    }

    public void evictCaches() {
        byKey.clear();
        byIdCache.clear();
        typeOfValue.clear();
        typeIds.clear();
    }
}
