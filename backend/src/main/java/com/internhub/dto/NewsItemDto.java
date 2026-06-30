package com.internhub.dto;

import com.internhub.entity.NewsItem;
import java.time.LocalDateTime;

public record NewsItemDto(
        Long id,
        String title,
        String summary,
        String link,
        String source,
        String keyword,
        LocalDateTime publishedAt,
        LocalDateTime fetchedAt) {

    public static NewsItemDto from(NewsItem item) {
        return new NewsItemDto(
                item.getId(),
                item.getTitle(),
                item.getSummary(),
                item.getLink(),
                item.getSource(),
                item.getKeyword(),
                item.getPublishedAt(),
                item.getFetchedAt());
    }
}
