package com.internhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "news_items")
public class NewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(length = 1200)
    private String summary;

    @Column(nullable = false, unique = true, length = 600)
    private String link;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(length = 120)
    private String keyword;

    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    protected NewsItem() {
    }

    public NewsItem(String title, String summary, String link, String source, String keyword,
            LocalDateTime publishedAt, LocalDateTime fetchedAt) {
        this.title = title;
        this.summary = summary;
        this.link = link;
        this.source = source;
        this.keyword = keyword;
        this.publishedAt = publishedAt;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getLink() {
        return link;
    }

    public String getSource() {
        return source;
    }

    public String getKeyword() {
        return keyword;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }
}
