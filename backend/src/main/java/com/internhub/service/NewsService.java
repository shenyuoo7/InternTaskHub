package com.internhub.service;

import com.internhub.dto.NewsItemDto;
import com.internhub.entity.NewsItem;
import com.internhub.repository.NewsItemRepository;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Service
public class NewsService {

    private final NewsItemRepository newsRepository;
    private final HttpClient httpClient;

    public NewsService(NewsItemRepository newsRepository) {
        this.newsRepository = newsRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Transactional(readOnly = true)
    public List<NewsItemDto> list(String keyword) {
        String normalized = normalize(keyword);
        return sortedItems().stream()
                .filter(item -> !StringUtils.hasText(normalized) || matches(item, normalized))
                .limit(30)
                .map(NewsItemDto::from)
                .toList();
    }

    @Transactional
    public List<NewsItemDto> refresh(String keyword) {
        String effectiveKeyword = StringUtils.hasText(keyword) ? keyword.trim() : "Java Spring Boot Vue";
        List<ParsedNews> fetched = fetchHackerNewsRss(effectiveKeyword);
        LocalDateTime now = LocalDateTime.now();
        for (ParsedNews item : fetched) {
            if (!StringUtils.hasText(item.link())) {
                continue;
            }
            newsRepository.findByLink(item.link()).orElseGet(() -> newsRepository.save(new NewsItem(
                    item.title(),
                    item.summary(),
                    item.link(),
                    item.source(),
                    effectiveKeyword,
                    item.publishedAt(),
                    now)));
        }
        return list(effectiveKeyword);
    }

    @Transactional
    public List<NewsItemDto> related(String keyword) {
        List<NewsItemDto> current = list(keyword);
        if (current.size() < 3) {
            current = refresh(keyword);
        }
        return current.stream().limit(8).toList();
    }

    private List<ParsedNews> fetchHackerNewsRss(String keyword) {
        try {
            String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            URI uri = URI.create("https://hnrss.org/newest?q=" + encoded);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "InternTaskHub/1.0")
                    .GET()
                    .build();
            String body = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
            return parseFeed(body, "Hacker News RSS", keyword);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<ParsedNews> parseFeed(String xml, String source, String keyword) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);

        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        NodeList rssItems = document.getElementsByTagName("item");
        if (rssItems.getLength() > 0) {
            return parseRssItems(rssItems, source, keyword);
        }
        NodeList atomEntries = document.getElementsByTagName("entry");
        return parseAtomEntries(atomEntries, source, keyword);
    }

    private List<ParsedNews> parseRssItems(NodeList items, String source, String keyword) {
        return java.util.stream.IntStream.range(0, Math.min(items.getLength(), 12))
                .mapToObj(items::item)
                .filter(Element.class::isInstance)
                .map(Element.class::cast)
                .map(item -> new ParsedNews(
                        fallback(text(item, "title"), "Untitled news"),
                        truncate(stripHtml(text(item, "description")), 360),
                        text(item, "link"),
                        source,
                        keyword,
                        parseDate(text(item, "pubDate"))))
                .toList();
    }

    private List<ParsedNews> parseAtomEntries(NodeList entries, String source, String keyword) {
        return java.util.stream.IntStream.range(0, Math.min(entries.getLength(), 12))
                .mapToObj(entries::item)
                .filter(Element.class::isInstance)
                .map(Element.class::cast)
                .map(entry -> new ParsedNews(
                        fallback(text(entry, "title"), "Untitled news"),
                        truncate(stripHtml(fallback(text(entry, "summary"), text(entry, "content"))), 360),
                        atomLink(entry),
                        source,
                        keyword,
                        parseDate(fallback(text(entry, "updated"), text(entry, "published")))))
                .toList();
    }

    private List<NewsItem> sortedItems() {
        return newsRepository.findAll().stream()
                .sorted(Comparator
                        .comparing((NewsItem item) -> newsTime(item), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing((NewsItem item) -> item.getId(), Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private LocalDateTime newsTime(NewsItem item) {
        return item.getPublishedAt() != null ? item.getPublishedAt() : item.getFetchedAt();
    }

    private boolean matches(NewsItem item, String keyword) {
        return normalize(item.getTitle()).contains(keyword)
                || normalize(item.getSummary()).contains(keyword)
                || normalize(item.getKeyword()).contains(keyword)
                || normalize(item.getSource()).contains(keyword);
    }

    private String text(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    private String atomLink(Element entry) {
        NodeList nodes = entry.getElementsByTagName("link");
        if (nodes.getLength() == 0 || !(nodes.item(0) instanceof Element link)) {
            return "";
        }
        String href = link.getAttribute("href");
        return StringUtils.hasText(href) ? href : link.getTextContent().trim();
    }

    private LocalDateTime parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }
        List<java.util.function.Function<String, LocalDateTime>> parsers = List.of(
                text -> ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime(),
                text -> OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime(),
                text -> LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        for (java.util.function.Function<String, LocalDateTime> parser : parsers) {
            try {
                return parser.apply(value);
            } catch (DateTimeParseException ignored) {
                // Try the next common feed date format.
            }
        }
        return LocalDateTime.now();
    }

    private String stripHtml(String value) {
        return value == null ? "" : value.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    private String truncate(String value, int length) {
        if (value == null || value.length() <= length) {
            return value;
        }
        return value.substring(0, length - 3) + "...";
    }

    private String fallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private record ParsedNews(
            String title,
            String summary,
            String link,
            String source,
            String keyword,
            LocalDateTime publishedAt) {
    }
}
