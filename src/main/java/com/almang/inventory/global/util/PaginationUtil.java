package com.almang.inventory.global.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

public class PaginationUtil {

    private PaginationUtil() {} // 인스턴스 생성 방지

    public static PageRequest createPageRequest(Integer page, Integer size, String sortBy) {
        int pageIndex = (page == null || page < 1) ? 0 : page - 1;
        int pageSize = (size == null || size < 1) ? 20 : size;

        return PageRequest.of(pageIndex, pageSize, Sort.by(Direction.ASC, sortBy));
    }

    public static PageRequest createPageRequest(Integer page, Integer size, Direction direction, String sortBy) {
        int pageIndex = (page == null || page < 1) ? 0 : page - 1;
        int pageSize = (size == null || size < 1) ? 20 : size;

        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, sortBy));
    }
}
