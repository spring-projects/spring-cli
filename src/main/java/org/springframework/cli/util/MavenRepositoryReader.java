/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.springframework.cli.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.springframework.cli.SpringCliException;

public class MavenRepositoryReader {

	private static final Logger logger = LoggerFactory.getLogger(MavenRepositoryReader.class);

	public String[] parseMavenRepositories(String text) {
		String textToUse = massageText(text);
		ErrorHandler errorHandler = new SimpleErrorHandler(logger);
		List<String> dependencies = new ArrayList<>();
		try {
			InputStream inputStream = IOUtils.toInputStream(textToUse, StandardCharsets.UTF_8);
			Document document = buildDocument(errorHandler, inputStream);
			parseDocument(document, dependencies);
		} catch (IOException ex) {
			throw new SpringCliException("Cannot parse maven repositories from string: " + text, ex);
		}
		catch (SAXException ex) {
			throw new SpringCliException("Invalid XML in maven repositories from string: " + text, ex);
		}
		catch (ParserConfigurationException ex) {
			throw new SpringCliException("Internal error parsing maven repositories from string:" + text, ex);
		}
		catch (TransformerException ex) {
			throw new SpringCliException("Internal error transforming Node to text from string:" + text, ex);
		}
		return dependencies.toArray(new String[0]);
	}

	private String massageText(String text) {
		if (text.contains("<repositories>")) {
			return text;
		} else {
			return "<repositories>" + text + "</repositories>";
		}
	}

	/**
	 * Reads the Document and populates the provided array with individual dependency
	 * text values for each dependency element.
	 * @param document The Document to parse
	 * @param dependencies a list to populate with each dependency text
	 * @throws TransformerException if the element can't be converted to text
	 */
	private void parseDocument(Document document, List<String> dependencies) throws TransformerException {
		Element root = document.getDocumentElement();
		NodeList nodeList = root.getElementsByTagName("repository");

		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node dependencyNode = nodeList.item(i);
			if (dependencyNode.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) dependencyNode;
				StringWriter writer = new StringWriter();
				transformer.transform(new DOMSource(element), new StreamResult(writer));
				String xml = writer.toString();
				dependencies.add(xml);
			}
		}
	}

	protected Document buildDocument(ErrorHandler handler, InputStream stream)
			throws ParserConfigurationException, SAXException, IOException {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder parser = dbf.newDocumentBuilder();
		parser.setErrorHandler(handler);
		return parser.parse(stream);
	}

	private class SimpleErrorHandler implements ErrorHandler {

		private final Logger logger;
		public SimpleErrorHandler(Logger logger) {
			this.logger = logger;
		}

		@Override
		public void warning(SAXParseException ex) throws SAXException {
			logger.warn("Ignored XML validation warning", ex);
		}

		@Override
		public void error(SAXParseException ex) throws SAXException {
			throw ex;
		}

		@Override
		public void fatalError(SAXParseException ex) throws SAXException {
			throw ex;
		}
	}
}
