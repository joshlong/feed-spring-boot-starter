package com.joshlong.feed;

import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FeedTemplateTest {

	private final FeedTemplate feedTemplate = new FeedTemplate();

	@Test
	void render() {

		record Customer(int id, String name) {
		}

		var db = List.of(new Customer(1, "A"), new Customer(2, "B"));

		var feed = this.feedTemplate.buildFeed(FeedTemplate.FeedType.ATOM_0_3, "title", "https://adobe.com",
				"a description", db, customer -> {
					var sei = new SyndEntryImpl();
					sei.setAuthor("Josh");
					sei.setTitle("new customer! " + customer.name());
					var syndContent = new SyndContentImpl();
					syndContent.setType("text/plain");
					syndContent.setMode("escaped");
					syndContent.setValue("this is a description for the new customer added, " + customer.name());
					sei.setContents(List.of(syndContent));
					return sei;
				});
		var render = this.feedTemplate.render(feed);
		System.out.println(render);

		var result = parseFeed(render);
		System.out.println("Feed Title: " + result.title());
		System.out.println("Feed Description: " + result.description());
		for (var entry : result.entries()) {
			System.out.println("\nEntry:");
			for (var field : entry.entrySet()) {
				System.out.println(field.getKey() + ": " + field.getValue());
			}

		}
		Assertions.assertEquals("title", result.title());
		Assertions.assertEquals("a description", result.description());
		Assertions.assertEquals("Josh", result.entries().get(0).get("author"));
		Assertions.assertEquals(result.entries().size() == 2, true, "there should be two entries!");
		Assertions.assertEquals("new customer! A", result.entries().get(0).get("title"));
		Assertions.assertEquals("new customer! B", result.entries().get(1).get("title"));
	}

	record FeedResult(String title, String description, List<Map<String, Object>> entries) {
	}

	private static FeedResult parseFeed(String xmlContent) {
		try {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true); // Enable namespace support
			var builder = factory.newDocumentBuilder();
			var doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));

			// Get feed level elements
			var title = getElementContent(doc, "title");
			var description = getElementContent(doc, "tagline");

			// Parse entries
			var entries = new ArrayList<Map<String, Object>>();
			var entryNodes = doc.getElementsByTagName("entry");

			for (int i = 0; i < entryNodes.getLength(); i++) {
				var entry = (Element) entryNodes.item(i);
				var entryMap = new HashMap<String, Object>();

				// Get entry title
				entryMap.put("title", getElementContent(entry, "title"));

				// Get author name
				var authorElement = (Element) entry.getElementsByTagName("author").item(0);
				if (authorElement != null) {
					entryMap.put("author", getElementContent(authorElement, "name"));
				}

				// Get content
				var contentNodes = entry.getElementsByTagName("content");
				if (contentNodes.getLength() > 0) {
					Element contentElement = (Element) contentNodes.item(0);
					entryMap.put("content", contentElement.getTextContent());
					entryMap.put("contentType", contentElement.getAttribute("type"));
					entryMap.put("contentMode", contentElement.getAttribute("mode"));
				}

				// Get DC creator
				var creatorNodes = entry.getElementsByTagNameNS("http://purl.org/dc/elements/1.1/", "creator");
				if (creatorNodes.getLength() > 0) {
					entryMap.put("dcCreator", creatorNodes.item(0).getTextContent());
				}

				entries.add(entryMap);
			}

			return new FeedResult(title, description, entries);

		}
		catch (Exception e) {
			throw new RuntimeException("Error parsing XML feed", e);
		}
	}

	private static String getElementContent(Document doc, String tagName) {
		var nodes = doc.getElementsByTagName(tagName);
		return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
	}

	private static String getElementContent(Element element, String tagName) {
		var nodes = element.getElementsByTagName(tagName);
		return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
	}

}