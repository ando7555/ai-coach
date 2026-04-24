package com.ai.coach.domain;

import com.ai.coach.domain.dto.PageInfo;

import java.util.Base64;
import java.util.List;
import java.util.function.Function;

/**
 * Generic cursor-based pagination utility.
 * Eliminates duplicated encode/decode/paginate logic across services.
 */
public final class CursorPaginator {

    public static final int DEFAULT_PAGE_SIZE = 20;

    private CursorPaginator() {}

    public static String encodeCursor(Long id) {
        return Base64.getEncoder().encodeToString(("cursor:" + id).getBytes());
    }

    public static Long decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        String decoded = new String(Base64.getDecoder().decode(cursor));
        return Long.valueOf(decoded.substring("cursor:".length()));
    }

    /**
     * Paginate an already-sorted list using cursor-based pagination.
     *
     * @param items    full sorted list
     * @param idGetter function to extract the Long ID from each item
     * @param first    requested page size (null → DEFAULT_PAGE_SIZE)
     * @param after    opaque cursor string (null → start from beginning)
     * @return a {@link Page} containing the slice, pageInfo, and total count
     */
    public static <T> Page<T> paginate(List<T> items, Function<T, Long> idGetter,
                                       Integer first, String after) {
        int pageSize = first != null && first > 0 ? first : DEFAULT_PAGE_SIZE;
        Long afterId = decodeCursor(after);

        List<T> filtered = items;
        if (afterId != null) {
            int idx = -1;
            for (int i = 0; i < items.size(); i++) {
                if (idGetter.apply(items.get(i)).equals(afterId)) {
                    idx = i;
                    break;
                }
            }
            filtered = idx >= 0 && idx + 1 < items.size()
                    ? items.subList(idx + 1, items.size())
                    : List.of();
        }

        boolean hasNextPage = filtered.size() > pageSize;
        List<T> page = hasNextPage ? filtered.subList(0, pageSize) : filtered;

        String endCursor = page.isEmpty()
                ? null
                : encodeCursor(idGetter.apply(page.get(page.size() - 1)));

        return new Page<>(page, new PageInfo(hasNextPage, endCursor), items.size());
    }

    public record Page<T>(List<T> items, PageInfo pageInfo, int totalCount) {}
}
