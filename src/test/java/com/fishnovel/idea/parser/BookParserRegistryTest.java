package com.fishnovel.idea.parser;

import com.fishnovel.idea.model.BookDocument;
import com.fishnovel.idea.model.SourceType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Assert;
import org.junit.Test;

public class BookParserRegistryTest {
    private final BookParserRegistry registry = BookParserRegistry.defaultRegistry();

    @Test
    public void shouldParseTxtBook() throws Exception {
        Path file = copySample("sample.txt", ".txt");
        BookDocument document = registry.parse(file);

        Assert.assertEquals("sample", document.getTitle());
        Assert.assertEquals(2, document.getChapters().size());
        Assert.assertEquals("第一章 初见", document.getChapters().get(0).getTitle());
    }

    @Test
    public void shouldParseMarkdownBook() throws Exception {
        Path file = copySample("sample.md", ".md");
        BookDocument document = registry.parse(file);

        Assert.assertEquals(3, document.getChapters().size());
        Assert.assertEquals("序章", document.getChapters().get(0).getTitle());
        Assert.assertTrue(document.getChapters().get(1).getContent().contains("第一节"));
    }

    @Test
    public void shouldParseHtmlBook() throws Exception {
        Path file = copySample("sample.html", ".html");
        BookDocument document = registry.parse(file);

        Assert.assertFalse(document.getTitle().isBlank());
        Assert.assertEquals(2, document.getChapters().size());
        Assert.assertTrue(document.getChapters().get(0).getContent().contains("第一章 起风了"));
    }

    @Test
    public void shouldParseEpubBook() throws Exception {
        Path file = createEpubSample();
        BookDocument document = registry.parse(file);

        Assert.assertEquals("EPUB 样例小说", document.getTitle());
        Assert.assertEquals(2, document.getChapters().size());
        Assert.assertTrue(document.getChapters().get(1).getContent().contains("第二章的内容"));
    }

    @Test
    public void shouldParseRemoteHtmlNovelContent() {
        HtmlBookParser parser = new HtmlBookParser();
        byte[] bytes = """
            <html>
            <head><title>网页章节</title></head>
            <body>
            <div id="chaptercontent">
            <p>第一段内容</p>
            <p>第二段内容</p>
            </div>
            </body>
            </html>
            """.stripIndent().getBytes(StandardCharsets.UTF_8);

        BookDocument document = parser.parseRemote("https://example.com/chapter-1.html", bytes);

        Assert.assertEquals(SourceType.REMOTE_URL, document.getSourceType());
        Assert.assertEquals("网页章节", document.getTitle());
        Assert.assertEquals(1, document.getChapters().size());
        Assert.assertTrue(document.getChapters().get(0).getContent().contains("第一段内容"));
        Assert.assertTrue(document.getChapters().get(0).getContent().contains("第二段内容"));
    }

    private Path copySample(String resourceName, String extension) throws IOException {
        Path directory = Files.createTempDirectory("fishnovel-samples");
        Path file = directory.resolve(resourceName);
        try (InputStream inputStream = getClass().getResourceAsStream("/samples/" + resourceName)) {
            if (inputStream == null) {
                throw new IOException("Missing sample resource: " + resourceName);
            }
            Files.copy(inputStream, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return file;
    }

    private Path createEpubSample() throws IOException {
        Path file = Files.createTempFile("fishnovel-", ".epub");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("mimetype"));
            zip.write("application/epub+zip".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("META-INF/container.xml"));
            zip.write("""
                <?xml version="1.0" encoding="UTF-8"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                    <rootfiles>
                        <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                    </rootfiles>
                </container>
                """.stripIndent().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("OEBPS/content.opf"));
            zip.write("""
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookId" version="2.0">
                    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                        <dc:title>EPUB 样例小说</dc:title>
                        <dc:language>zh-CN</dc:language>
                        <dc:identifier id="BookId">fishnovel-epub-sample</dc:identifier>
                    </metadata>
                    <manifest>
                        <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
                        <item id="chapter1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
                        <item id="chapter2" href="chapter2.xhtml" media-type="application/xhtml+xml"/>
                    </manifest>
                    <spine toc="ncx">
                        <itemref idref="chapter1"/>
                        <itemref idref="chapter2"/>
                    </spine>
                </package>
                """.stripIndent().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("OEBPS/toc.ncx"));
            zip.write("""
                <?xml version="1.0" encoding="UTF-8"?>
                <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
                    <head>
                        <meta name="dtb:uid" content="fishnovel-epub-sample"/>
                    </head>
                    <docTitle><text>EPUB 样例小说</text></docTitle>
                    <navMap>
                        <navPoint id="navPoint-1" playOrder="1">
                            <navLabel><text>第一章</text></navLabel>
                            <content src="chapter1.xhtml"/>
                        </navPoint>
                        <navPoint id="navPoint-2" playOrder="2">
                            <navLabel><text>第二章</text></navLabel>
                            <content src="chapter2.xhtml"/>
                        </navPoint>
                    </navMap>
                </ncx>
                """.stripIndent().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("OEBPS/chapter1.xhtml"));
            zip.write("""
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>第一章</title></head>
                <body><h1>第一章</h1><p>这是第一章的内容。</p></body>
                </html>
                """.stripIndent().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("OEBPS/chapter2.xhtml"));
            zip.write("""
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>第二章</title></head>
                <body><h1>第二章</h1><p>这是第二章的内容。</p></body>
                </html>
                """.stripIndent().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return file;
    }
}
