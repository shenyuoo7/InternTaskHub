package com.internhub.controller;

import com.internhub.dto.NewsItemDto;
import com.internhub.service.NewsService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public List<NewsItemDto> list(@RequestParam(required = false) String keyword) {
        return newsService.list(keyword);
    }

    @GetMapping("/related")
    public List<NewsItemDto> related(@RequestParam String keyword) {
        return newsService.related(keyword);
    }

    @PostMapping("/refresh")
    public List<NewsItemDto> refresh(@RequestParam(required = false) String keyword) {
        return newsService.refresh(keyword);
    }
}
