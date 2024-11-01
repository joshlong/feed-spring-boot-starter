/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.joshlong.feed;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

/**
 * support for creating RSS and ATOM feeds with ROME
 * 
 * @author Josh Long
 */
public class FeedTemplate {

	public enum FeedType {

		RSS_0_9("rss_0.9"), RSS_0_93("rss_0.93"), ATOM_0_3("atom_0.3");

		private final String value;

		FeedType(String value) {
			this.value = value;
		}

		public String value() {
			return this.value;
		}

	}

	public <T> SyndFeed buildFeed(FeedType type, String title, String link, String description, List<T> posts,
			SyndEntryMapper<T> mapper) {
		return this.buildFeed(type.value(), title, link, description, posts, mapper);
	}

	public <T> SyndFeed buildFeed(String feedType, String title, String link, String description, List<T> posts,
			SyndEntryMapper<T> mapper) {
		Assert.notNull(mapper, "the mapper must not be null");
		Assert.hasText(feedType, "the feedType must not be null");
		Assert.hasText(link, "the link must not be null");
		Assert.hasText(title, "the link must not be null");
		Assert.notNull(posts, "the posts must not be null");
		var entries = posts//
			.stream()//
			// .parallel()//
			.map(i -> {
				try {
					return mapper.map(i);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			})//
			.collect(Collectors.toList());
		return this.buildFeed(feedType, title, link, description, entries);
	}

	public String render(SyndFeed feed) {
		try {
			var output = new SyndFeedOutput();
			return output.outputString(feed);
		} //
		catch (FeedException e) {
			throw new RuntimeException(e);
		}
	}

	private SyndFeed buildFeed(String feedType, String title, String link, String description, List<SyndEntry> posts) {
		var feed = new SyndFeedImpl();
		feed.setFeedType(feedType);
		feed.setTitle(title);
		feed.setLink(link);
		feed.setDescription(description);
		feed.setEntries(posts);

		return feed;
	}

}
