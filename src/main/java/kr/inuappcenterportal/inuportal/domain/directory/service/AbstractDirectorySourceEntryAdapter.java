package kr.inuappcenterportal.inuportal.domain.directory.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

abstract class AbstractDirectorySourceEntryAdapter implements DirectorySourceEntryAdapter {

    private static final int REQUEST_TIMEOUT_MILLIS = 20000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    protected Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(REQUEST_TIMEOUT_MILLIS)
                .maxBodySize(0)
                .get();
    }
}
