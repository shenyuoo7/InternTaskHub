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
import java.util.Set;
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

    private static final String DEFAULT_KEYWORD = "Java Spring Boot Vue AI Agent 开源";

    private static final List<FeedSource> FEED_SOURCES = List.of(
            new FeedSource("InfoQ 中文", "https://www.infoq.cn/feed/", false),
            new FeedSource("OSChina 开源社区", "https://www.oschina.net/news/rss", false),
            new FeedSource("Hacker News 技术快讯", "https://hnrss.org/newest?q=", true));

    private static final Set<String> TECH_KEYWORDS = Set.copyOf(List.of(
            "java", "spring", "spring boot", "vue", "vue3", "javascript", "typescript",
            "python", "golang", "rust", "ai", "agent", "llm", "openai", "github",
            "kubernetes", "docker", "database", "mysql", "redis", "cloud", "api",
            "开源", "技术", "编程", "程序员", "后端", "前端", "数据库", "云原生",
            "框架", "架构", "大模型", "模型", "智能体", "软件", "代码", "算法",
            "longcat", "mcp"));

    private final NewsItemRepository newsRepository;
    private final HttpClient httpClient;

    public NewsService(NewsItemRepository newsRepository) {
        this.newsRepository = newsRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Transactional
    public List<NewsItemDto> list(String keyword) {
        String effectiveKeyword = effectiveKeyword(keyword);
        if (newsRepository.count() == 0) {
            fetchAndSave(effectiveKeyword);
        }
        return filterAndMap(effectiveKeyword);
    }

    @Transactional
    public List<NewsItemDto> refresh(String keyword) {
        String effectiveKeyword = effectiveKeyword(keyword);
        fetchAndSave(effectiveKeyword);
        return filterAndMap(effectiveKeyword);
    }

    @Transactional
    public List<NewsItemDto> related(String keyword) {
        String effectiveKeyword = effectiveKeyword(keyword);
        List<NewsItemDto> current = filterAndMap(effectiveKeyword);
        if (current.size() < 3) {
            fetchAndSave(effectiveKeyword);
            current = filterAndMap(effectiveKeyword);
        }
        if (current.isEmpty() && !DEFAULT_KEYWORD.equals(effectiveKeyword)) {
            fetchAndSave(DEFAULT_KEYWORD);
            current = filterAndMap(DEFAULT_KEYWORD);
        }
        return current.stream().limit(8).toList();
    }

    private void fetchAndSave(String keyword) {
        LocalDateTime fetchedAt = LocalDateTime.now();
        for (FeedSource source : FEED_SOURCES) {
            for (ParsedNews item : fetchFeed(source, keyword)) {
                ParsedNews normalized = localize(item, keyword, fetchedAt);
                if (!StringUtils.hasText(normalized.link()) || !isTechnical(normalized, keyword)) {
                    continue;
                }
                newsRepository.findByLink(normalized.link()).orElseGet(() -> newsRepository.save(new NewsItem(
                        truncate(normalized.title(), 240),
                        truncate(normalized.summary(), 1200),
                        truncate(normalized.link(), 600),
                        normalized.source(),
                        truncate(keyword, 120),
                        normalized.publishedAt(),
                        fetchedAt)));
            }
        }
    }

    private List<NewsItemDto> filterAndMap(String keyword) {
        String normalized = normalize(keyword);
        return sortedItems().stream()
                .filter(item -> !StringUtils.hasText(normalized) || matches(item, normalized))
                .limit(40)
                .map(NewsItemDto::from)
                .toList();
    }

    private List<ParsedNews> fetchFeed(FeedSource source, String keyword) {
        try {
            String url = source.queryable()
                    ? source.url() + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                    : source.url();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "InternTaskHub/1.0")
                    .GET()
                    .build();
            String body = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
            return parseFeed(body, source.name(), keyword);
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
        return java.util.stream.IntStream.range(0, Math.min(items.getLength(), 18))
                .mapToObj(items::item)
                .filter(Element.class::isInstance)
                .map(Element.class::cast)
                .map(item -> new ParsedNews(
                        fallback(text(item, "title"), "未命名技术资讯"),
                        fallback(stripHtml(text(item, "description")), stripHtml(text(item, "content:encoded"))),
                        text(item, "link"),
                        source,
                        keyword,
                        parseDate(fallback(text(item, "pubDate"), text(item, "updated")))))
                .toList();
    }

    private List<ParsedNews> parseAtomEntries(NodeList entries, String source, String keyword) {
        return java.util.stream.IntStream.range(0, Math.min(entries.getLength(), 18))
                .mapToObj(entries::item)
                .filter(Element.class::isInstance)
                .map(Element.class::cast)
                .map(entry -> new ParsedNews(
                        fallback(text(entry, "title"), "未命名技术资讯"),
                        fallback(stripHtml(text(entry, "summary")), stripHtml(text(entry, "content"))),
                        atomLink(entry),
                        source,
                        keyword,
                        parseDate(fallback(text(entry, "updated"), text(entry, "published")))))
                .toList();
    }

    private ParsedNews localize(ParsedNews item, String keyword, LocalDateTime fetchedAt) {
        String title = stripHtml(item.title());
        String summary = stripHtml(item.summary());
        boolean chineseTitle = containsChinese(title);
        boolean chineseSummary = containsChinese(summary);

        String localizedTitle = chineseTitle ? title : "技术快讯：" + title;
        String localizedSummary;
        if (StringUtils.hasText(summary) && chineseSummary) {
            localizedSummary = summary;
        } else if (StringUtils.hasText(summary)) {
            localizedSummary = "来自" + item.source() + "的技术资讯，关键词「" + readableKeyword(keyword)
                    + "」。原文摘要：" + summary;
        } else {
            localizedSummary = "来自" + item.source() + "的最新技术动态，关注「" + readableKeyword(keyword)
                    + "」相关方向，可点击原文查看完整内容。";
        }

        return new ParsedNews(
                localizedTitle,
                localizedSummary,
                cleanLink(item.link()),
                item.source(),
                keyword,
                item.publishedAt() == null ? fetchedAt : item.publishedAt());
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

    private boolean isTechnical(ParsedNews item, String keyword) {
        String haystack = normalize(item.title() + " " + item.summary() + " " + item.source() + " " + keyword);
        if (matchesKeyword(haystack, keyword)) {
            return true;
        }
        return TECH_KEYWORDS.stream().anyMatch(haystack::contains);
    }

    private boolean matchesKeyword(String haystack, String keyword) {
        return java.util.Arrays.stream(normalize(keyword).split("\\s+"))
                .filter(StringUtils::hasText)
                .anyMatch(haystack::contains);
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
                text -> OffsetDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime(),
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

    private String cleanLink(String value) {
        return value == null ? "" : value.replace("&amp;", "&").trim();
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

    private String effectiveKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : DEFAULT_KEYWORD;
    }

    private String readableKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : "技术动态";
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private boolean containsChinese(String value) {
        return value != null && value.codePoints().anyMatch(codePoint ->
                Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private record FeedSource(String name, String url, boolean queryable) {
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
