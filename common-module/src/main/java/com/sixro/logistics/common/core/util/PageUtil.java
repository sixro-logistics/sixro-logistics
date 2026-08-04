package com.sixro.logistics.common.core.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageUtil {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    public static Pageable toPageable(int page, int size, String direction) {
        return toPageable(page, size, direction, DEFAULT_SORT_FIELD);
    }

    public static Pageable toPageable(
            int page,
            int size,
            String direction,
            String sortField
    ) {
        int validatedPage = Math.max(page, DEFAULT_PAGE);
        int validatedSize = validateSize(size);
        Sort.Direction sortDirection = resolveDirection(direction);

        return PageRequest.of(
                validatedPage,
                validatedSize,
                Sort.by(sortDirection, sortField)
        );
    }

    private static int validateSize(int size) {
        return switch (size) {
            case 10, 30, 50 -> size;
            default -> DEFAULT_SIZE;
        };
    }

    private static Sort.Direction resolveDirection(String direction) {
        return "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
    }
}
