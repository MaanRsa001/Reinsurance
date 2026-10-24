package com.rms.common;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> content, long total, int page, int size) {
    public static <T> PageResponse<T> of(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getTotalElements(), p.getNumber(), p.getSize());
    }
}
