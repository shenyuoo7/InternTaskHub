package com.internhub.repository;

import com.internhub.entity.NewsItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

    Optional<NewsItem> findByLink(String link);

    List<NewsItem> findTop30ByOrderByPublishedAtDescFetchedAtDesc();
}
