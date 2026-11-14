package com.lectures.business.design.sales.persistence;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceUnitSettingsTest {

    private static Element unit;

    @BeforeAll
    static void readPersistenceXml() throws Exception {
        try (InputStream in = PersistenceUnitSettingsTest.class.getResourceAsStream("/META-INF/persistence.xml")) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(in);
            unit = (Element) document.getElementsByTagNameNS("*", "persistence-unit").item(0);
        }
    }

    @Test
    @DisplayName("the unit is a JTA unit on the application's data source")
    void jtaOnTheApplicationDataSource() {
        assertThat(unit.getAttribute("transaction-type")).isEqualTo("JTA");
        assertThat(text("jta-data-source")).isEqualTo("java:app/jdbc/sales");
    }

    @Test
    @DisplayName("only the listed entities are managed")
    void onlyListedClasses() {
        assertThat(text("exclude-unlisted-classes")).isEqualTo("true");
    }

    @Test
    @DisplayName("the second-level cache is off")
    void sharedCacheIsOff() {
        assertThat(text("shared-cache-mode")).isEqualTo("NONE");
    }

    @Test
    @DisplayName("Flyway owns the schema, JPA generates nothing")
    void schemaGenerationIsOff() {
        assertThat(property("jakarta.persistence.schema-generation.database.action")).isEqualTo("none");
    }

    private static String text(String tag) {
        NodeList nodes = unit.getElementsByTagNameNS("*", tag);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent().trim();
    }

    private static String property(String name) {
        NodeList properties = unit.getElementsByTagNameNS("*", "property");
        for (int i = 0; i < properties.getLength(); i++) {
            Element property = (Element) properties.item(i);
            if (property.getAttribute("name").equals(name)) {
                return property.getAttribute("value");
            }
        }
        return null;
    }
}
