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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * registers GraalVM / Spring AOT hints for ROME RSS/ATOM feeds.
 *
 * @author Josh Long
 */
class FeedRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static Set<TypeReference> findClassesInPackage(String packageName, TypeFilter typeFilter) {
		var classPathScanningCandidateComponentProvider = new ClassPathScanningCandidateComponentProvider(false);
		classPathScanningCandidateComponentProvider.addIncludeFilter(typeFilter);
		return classPathScanningCandidateComponentProvider//
				.findCandidateComponents(packageName)//
				.stream()//
				.map(bd -> TypeReference.of(Objects.requireNonNull(bd.getBeanClassName())))//
				.collect(Collectors.toUnmodifiableSet());
	}

	private static Set<TypeReference> findAnnotatedClassesInPackage(String packageName) {
		return findClassesInPackage(packageName, (metadataReader, metadataReaderFactory) -> true);
	}

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

		if (!ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", FeedRuntimeHintsRegistrar.class.getClassLoader()))
			return;

		var mcs = MemberCategory.values();
		var classes = findAnnotatedClassesInPackage("com.rometools.rome");
		for (var c : classes) {
			try {
				var cls = Class.forName(c.getName());
				if (Serializable.class.isAssignableFrom(cls)) {
					this.log.info("register {} for reflection/serialization.", c.getName());
					hints.serialization().registerType(c);
					hints.reflection().registerType(c, mcs);
				}
			} //
			catch (ClassNotFoundException e) {
				this.log.warn("could not find the class {} and got the following exception: {}", (Object) c,
						e.toString()); // don't care
			}
		}

		// rome
		for (var c : new Class<?>[] { Date.class, SyndEntry.class, com.rometools.rome.feed.module.DCModuleImpl.class })
			hints.reflection().registerType(c, mcs);

		var resource = new ClassPathResource("/com/rometools/rome/rome.properties");
		hints.resources().registerResource(resource);
		try (var in = resource.getInputStream()) {
			var props = new Properties();
			props.load(in);
			props.propertyNames().asIterator().forEachRemaining(pn -> {
				var clz = loadClasses((String) pn, props.getProperty((String) pn));
				clz.forEach(cn -> hints.reflection().registerType(TypeReference.of(cn), mcs));
			});
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static List<String> loadClasses(String propertyName, String propertyValue) {
		Assert.hasText(propertyName, "the propertyName must not be null");
		Assert.hasText(propertyValue, "the propertyValue must not be null");
		return Arrays //
				.stream((propertyValue.contains(" ")) ? propertyValue.split(" ") : new String[] { propertyValue }) //
				.map(String::trim).filter(xValue -> !xValue.isBlank()).toList();

	}

}
