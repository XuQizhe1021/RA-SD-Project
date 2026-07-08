package com.hqtraining.backend.common;

import java.util.List;

public record PageResult<T>(
        List<T> list,
        int pageNum,
        int pageSize,
        long total
) {
}
