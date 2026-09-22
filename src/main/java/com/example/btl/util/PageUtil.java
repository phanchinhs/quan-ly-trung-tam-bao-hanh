package com.example.btl.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PageUtil {

    public int clampPage(int page, int totalPages) {
        if (totalPages <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(page, totalPages - 1));
    }

    /** Tạo danh sách trang rút gọn: min{đầu, cuối, trang hiện tại +-1} + dấu "...". */
    public List<Map<String, Object>> buildPagination(int current, int total) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (total <= 0) {
            return items;
        }
        if (total <= 7) {
            for (int i = 0; i < total; i++) {
                items.add(pageItem(i, false));
            }
            return items;
        }
        items.add(pageItem(0, false));
        int start = Math.max(1, current - 1);
        int end = Math.min(total - 2, current + 1);
        if (start > 1) {
            items.add(pageItem(-1, true));
        }
        for (int i = start; i <= end; i++) {
            items.add(pageItem(i, false));
        }
        if (end < total - 2) {
            items.add(pageItem(-1, true));
        }
        items.add(pageItem(total - 1, false));
        return items;
    }

    private Map<String, Object> pageItem(int page, boolean ellipsis) {
        Map<String, Object> item = new HashMap<>();
        item.put("page", page);
        item.put("ellipsis", ellipsis);
        return item;
    }
}