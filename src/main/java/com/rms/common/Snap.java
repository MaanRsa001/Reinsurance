package com.rms.common;

/** id + code + description of a master / lookup row. Transaction tables store all three (snapshot rule 3A). */
public record Snap(Long id, String code, String desc) {
}
