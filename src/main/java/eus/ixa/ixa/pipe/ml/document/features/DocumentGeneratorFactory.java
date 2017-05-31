/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package eus.ixa.ixa.pipe.ml.document.features;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ext.ExtensionLoader;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;

/**
 * Creates a set of feature generators based on a provided XML descriptor.
 *
 * Example of an XML descriptor:
 *<p>
 * &lt;generators&gt;
 *   &lt;charngram min = "2" max = "5"/&gt;
 *   &lt;definition/&gt;
 *   &lt;cache&gt;
 *     &lt;window prevLength = "3" nextLength = "3"&gt;
 *       &lt;generators&gt;
 *         &lt;prevmap/&gt;
 *         &lt;sentence/&gt;
 *         &lt;tokenclass/&gt;
 *         &lt;tokenpattern/&gt;
 *       &lt;/generators&gt;
 *     &lt;/window&gt;
 *   &lt;/cache&gt;
 * &lt;/generators&gt;
 * </p>
 *
 * Each XML element is mapped to a {@link DocumentGeneratorFactory.XmlFeatureGeneratorFactory} which
 * is responsible to process the element and create the specified
 * {@link DocumentFeatureGenerator}. Elements can contain other
 * elements in this case it is the responsibility of the mapped factory to process
 * the child elements correctly. In some factories this leads to recursive
 * calls the
 * {@link DocumentGeneratorFactory.XmlFeatureGeneratorFactory#create(Element, FeatureGeneratorResourceProvider)}
 * method.
 *
 * In the example above the generators element is mapped to the
 * {@link DocumentGeneratorFactory.DocumentFeatureAggregatorFactory} which then
 * creates all the aggregated {@link DocumentFeatureGenerator}s to
 * accomplish this it evaluates the mapping with the same mechanism
 * and gives the child element to the corresponding factories. All
 * created generators are added to a new instance of the
 * {@link DocumentFeatureAggregator} which is then returned.
 */
public class DocumentGeneratorFactory {

  /**
   * The {@link XmlFeatureGeneratorFactory} is responsible to construct
   * an {@link DocumentFeatureGenerator} from an given XML {@link Element}
   * which contains all necessary configuration if any.
   */
  interface XmlFeatureGeneratorFactory {

    /**
     * Creates an {@link DocumentFeatureGenerator} from a the describing
     * XML element.
     *
     * @param generatorElement the element which contains the configuration
     * @param resourceManager the resource manager which could be used
     *     to access referenced resources
     *
     * @return the configured {@link DocumentFeatureGenerator}
     */
    DocumentFeatureGenerator create(Element generatorElement,
        FeatureGeneratorResourceProvider resourceManager) throws InvalidFormatException;
  }

  /**
   * @see DocumentFeatureAggregator
   */
  static class DocumentFeatureAggregatorFactory implements XmlFeatureGeneratorFactory {

    public DocumentFeatureGenerator create(Element generatorElement,
        FeatureGeneratorResourceProvider resourceManager)  throws InvalidFormatException {

      Collection<DocumentFeatureGenerator> aggregatedGenerators = new LinkedList<>();

      NodeList childNodes = generatorElement.getChildNodes();

      for (int i = 0; i < childNodes.getLength(); i++) {
        Node childNode = childNodes.item(i);
        if (childNode instanceof Element) {
          Element aggregatedGeneratorElement = (Element) childNode;
          aggregatedGenerators.add(
              DocumentGeneratorFactory.createGenerator(aggregatedGeneratorElement, resourceManager));
        }
      }

      return new DocumentFeatureAggregator(aggregatedGenerators.toArray(
          new DocumentFeatureGenerator[aggregatedGenerators.size()]));
    }

    static void register(Map<String, XmlFeatureGeneratorFactory> factoryMap) {
      factoryMap.put("generators", new DocumentFeatureAggregatorFactory());
    }
  }

  static class DocumentCustomFeatureGeneratorFactory implements XmlFeatureGeneratorFactory {

    public DocumentFeatureGenerator create(Element generatorElement,
        FeatureGeneratorResourceProvider resourceManager) throws InvalidFormatException {

      String featureGeneratorClassName = generatorElement.getAttribute("class");

      DocumentFeatureGenerator generator =
          ExtensionLoader.instantiateExtension(DocumentFeatureGenerator.class, featureGeneratorClassName);

      if (generator instanceof DocumentCustomFeatureGenerator) {

        DocumentCustomFeatureGenerator customGenerator = (DocumentCustomFeatureGenerator) generator;

        Map<String, String> properties = new HashMap<>();

        NamedNodeMap attributes = generatorElement.getAttributes();

        for (int i = 0; i < attributes.getLength(); i++) {
          Node attribute = attributes.item(i);
          if (!"class".equals(attribute.getNodeName())) {
            properties.put(attribute.getNodeName(), attribute.getNodeValue());
          }
        }

        if (resourceManager != null) {
          customGenerator.init(properties, resourceManager);
        }
      }

      return generator;
    }

    static void register(Map<String, XmlFeatureGeneratorFactory> factoryMap) {
      factoryMap.put("custom", new DocumentCustomFeatureGeneratorFactory());
    }
  }

  private static Map<String, XmlFeatureGeneratorFactory> factories = new HashMap<>();

  static {
    DocumentFeatureAggregatorFactory.register(factories);
    DocumentCustomFeatureGeneratorFactory.register(factories);
  }

  /**
   * Creates a {@link DocumentFeatureGenerator} for the provided element.
   * To accomplish this it looks up the corresponding factory by the
   * element tag name. The factory is then responsible for the creation
   * of the generator from the element.
   *
   * @param generatorElement
   * @param resourceManager
   *
   * @return
   */
  static DocumentFeatureGenerator createGenerator(Element generatorElement,
      FeatureGeneratorResourceProvider resourceManager) throws InvalidFormatException {

    String elementName = generatorElement.getTagName();

    XmlFeatureGeneratorFactory generatorFactory = factories.get(elementName);

    if (generatorFactory == null) {
      throw new InvalidFormatException("Unexpected element: " + elementName);
    }

    return generatorFactory.create(generatorElement, resourceManager);
  }

  private static org.w3c.dom.Document createDOM(InputStream xmlDescriptorIn)
      throws IOException {
    DocumentBuilderFactory documentBuilderFacoty = DocumentBuilderFactory.newInstance();

    DocumentBuilder documentBuilder;

    try {
      documentBuilder = documentBuilderFacoty.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException(e);
    }

    org.w3c.dom.Document xmlDescriptorDOM;

    try {
      xmlDescriptorDOM = documentBuilder.parse(xmlDescriptorIn);
    } catch (SAXException e) {
      throw new InvalidFormatException("Descriptor is not valid XML!", e);
    }
    return xmlDescriptorDOM;
  }

  /**
   * Creates an {@link DocumentFeatureGenerator} from an provided XML descriptor.
   *
   * Usually this XML descriptor contains a set of nested feature generators
   * which are then used to generate the features by one of the opennlp
   * components.
   *
   * @param xmlDescriptorIn the {@link InputStream} from which the descriptor
   *     is read, the stream remains open and must be closed by the caller.
   *
   * @param resourceManager the resource manager which is used to resolve resources
   *     referenced by a key in the descriptor
   *
   * @return created feature generators
   *
   * @throws IOException if an error occurs during reading from the descriptor
   *     {@link InputStream}
   */
  public static DocumentFeatureGenerator create(InputStream xmlDescriptorIn,
      FeatureGeneratorResourceProvider resourceManager) throws IOException {

    org.w3c.dom.Document xmlDescriptorDOM = createDOM(xmlDescriptorIn);

    Element generatorElement = xmlDescriptorDOM.getDocumentElement();

    return createGenerator(generatorElement, resourceManager);
  }

  public static Map<String, ArtifactSerializer<?>> extractCustomArtifactSerializerMappings(
      InputStream xmlDescriptorIn)
      throws IOException, InvalidFormatException {

    Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();

    org.w3c.dom.Document xmlDescriptorDOM = createDOM(xmlDescriptorIn);

    XPath xPath = XPathFactory.newInstance().newXPath();


    NodeList customElements;
    try {
      XPathExpression exp = xPath.compile("//custom");
      customElements = (NodeList) exp.evaluate(xmlDescriptorDOM.getDocumentElement(), XPathConstants.NODESET);
    } catch (XPathExpressionException e) {
      throw new IllegalStateException("The hard coded XPath expression should always be valid!");
    }

    for (int i = 0; i < customElements.getLength(); i++) {

      if (customElements.item(i) instanceof Element) {
        Element customElement = (Element) customElements.item(i);

        // Note: The resource provider is not available at that point, to provide
        // resources they need to be loaded first!
        DocumentFeatureGenerator generator = createGenerator(customElement, null);

        if (generator instanceof ArtifactToSerializerMapper) {
          ArtifactToSerializerMapper mapper = (ArtifactToSerializerMapper) generator;
          mapping.putAll(mapper.getArtifactSerializerMapping());
        }
      }
    }
    return mapping;
  }

  /**
   * Provides a list with all the elements in the xml feature descriptor.
   * @param xmlDescriptorIn the xml feature descriptor
   * @return a list containing all elements
   * @throws IOException if inputstream cannot be open
   * @throws InvalidFormatException if xml is not well-formed
   */
  public static List<Element> getDescriptorElements(InputStream xmlDescriptorIn)
      throws IOException {

    List<Element> elements = new ArrayList<>();
    org.w3c.dom.Document xmlDescriptorDOM = createDOM(xmlDescriptorIn);
    XPath xPath = XPathFactory.newInstance().newXPath();
    NodeList allElements;
    try {
      XPathExpression exp = xPath.compile("//*");
      allElements = (NodeList) exp.evaluate(xmlDescriptorDOM.getDocumentElement(), XPathConstants.NODESET);
    } catch (XPathExpressionException e) {
      throw new IllegalStateException("The hard coded XPath expression should always be valid!");
    }

    for (int i = 0; i < allElements.getLength(); i++) {
      if (allElements.item(i) instanceof Element) {
        Element customElement = (Element) allElements.item(i);
        elements.add(customElement);
      }
    }
    return elements;
  }
}

