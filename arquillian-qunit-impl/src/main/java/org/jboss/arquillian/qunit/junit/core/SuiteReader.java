/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.qunit.junit.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.arquillian.phantom.resolver.ResolvingPhantomJSDriverService;
import org.jboss.arquillian.qunit.api.model.QUnitTest;
import org.jboss.arquillian.qunit.api.model.TestMethod;
import org.jboss.arquillian.qunit.api.model.TestSuite;
import org.jboss.arquillian.qunit.junit.model.QUnitTestImpl;
import org.jboss.arquillian.qunit.junit.utils.QUnitConstants;
import org.jboss.arquillian.qunit.utils.FileUtilities;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 *
 * @author Lukas Fryc
 * @author Tolis Emmanouilidis
 *
 */
public final class SuiteReader {

    private static final Logger LOGGER = Logger.getLogger(SuiteReader.class.getName());

    private SuiteReader() {
    }

    private static class SingletonHolder {
        public static final SuiteReader INSTANCE = new SuiteReader();
    }

    public static SuiteReader getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static final String QUNIT_READER = (new StringBuilder())
        .append(
            "(function(a){if(a.QUnit!==undefined){a.arquillianQUnitTests=a.arquillianQUnitTests||[];if(!String.prototype.trim){String.prototype.trim=function(){return this.replace(/^\\s+|\\s+$/g,\"\")}}a.test=function(b,d,e,c){a.arquillianQUnitTests.push(((QUnit.config.currentModule&&String(QUnit.config.currentModule).trim()!==\"\")?QUnit.config.currentModule:\"\")+\"")
        .append(QUnitConstants.DELIMITER)
        .append(
            "\"+((b&&b.trim()!==\"\")?b:\"\"))};a.asyncTest=function(b,c,d){a.arquillianQUnitTests.push(((QUnit.config.currentModule&&String(QUnit.config.currentModule).trim()!==\"\")?QUnit.config.currentModule:\"\")+\"")
        .append(QUnitConstants.DELIMITER)
        .append(
            "\"+((b&&b.trim()!==\"\")?b:\"\"))};a.QUnit.test=function(b,d,e,c){a.arquillianQUnitTests.push(((QUnit.config.currentModule&&String(QUnit.config.currentModule).trim()!==\"\")?QUnit.config.currentModule:\"\")+\"")
        .append(QUnitConstants.DELIMITER)
        .append(
            "\"+((b&&b.trim()!==\"\")?b:\"\"))};a.QUnit.asyncTest=function(b,c,d){a.arquillianQUnitTests.push(((QUnit.config.currentModule&&String(QUnit.config.currentModule).trim()!==\"\")?QUnit.config.currentModule:\"\")+\"")
        .append(QUnitConstants.DELIMITER).append("\"+((b&&b.trim()!==\"\")?b:\"\"))}}})(this); \n \n").toString();

    private static final String JS_PATTERN = "([^\\s]+(\\.(js))$)";

    public Map<String, List<String>> readQUnitTests(Archive<?> archive, TestSuite suite) throws IOException {
        injectQUnitSuiteReader(archive);
        return readQUnitSuite(archive, suite);
    }

    private Map<String, List<String>> readQUnitSuite(Archive<?> archive, TestSuite suite) throws IOException {

        Map<String, List<String>> qunitSuiteNameTestsHM = new LinkedHashMap<String, List<String>>();

        final TestMethod[] qunitTestMethods = suite.getTestMethods();
        if (!ArrayUtils.isEmpty(qunitTestMethods)) {

//            PhantomJSDriverService driverService = (PhantomJSDriverService) ResolvingPhantomJSDriverService
//                .createDefaultService();
//            PhantomJSDriver driver = new PhantomJSDriver(driverService, DesiredCapabilities.phantomjs());

        	ChromeOptions options = new ChromeOptions();
        	options.addArguments("--headless");
        	ChromeDriver driver = new ChromeDriver(options);
        	
            for (TestMethod method : qunitTestMethods) {
                if (!StringUtils.isEmpty(method.getQUnitTestSuiteFilePath())) {

                    qunitSuiteNameTestsHM.put(method.getQUnitTestSuiteFilePath(), new ArrayList<String>());

                    final String qunitTestFilePath = (new StringBuilder()).append(QUnitConstants.TMP_FOLDER).append("/")
                        .append(archive.getName()).append("/").append(method.getQUnitTestSuiteFilePath()).toString();

                    URL url = new File(qunitTestFilePath).toURI().toURL();

                    driver.get(url.toExternalForm());

                    @SuppressWarnings("unchecked")
                    List<String> qunitTestList =
                        (List<String>) driver.executeScript("return window.arquillianQUnitTests");

                    if (!CollectionUtils.isEmpty(qunitTestList)) {

                        for (String moduleTestNameStr : qunitTestList) {

                            // final String moduleTestNameStr = moduleConsString.toString();

                            final int delimiterIndex = moduleTestNameStr.indexOf(QUnitConstants.DELIMITER);

                            QUnitTest test =
                                new QUnitTestImpl().setModuleName(moduleTestNameStr.substring(0, delimiterIndex))
                                    .setName(
                                        moduleTestNameStr.substring(delimiterIndex + QUnitConstants.DELIMITER.length()));

                            qunitSuiteNameTestsHM.get(method.getQUnitTestSuiteFilePath()).add(test.getDescriptionName());
                        }
                    }
                }
            }
            driver.close();
        }

        try {
            FileUtilities.deleteDirectory(QUnitConstants.TMP_FOLDER);
        } catch (IOException ignore) {
            LOGGER.log(Level.WARNING, "deleteDirectory Error", ignore);
        }

        return qunitSuiteNameTestsHM;
    }

    private void injectQUnitSuiteReader(Archive<?> archive) throws IOException {
        final File tempFolder = FileUtilities.createDirectory(QUnitConstants.TMP_FOLDER);
        archive.as(ExplodedExporter.class).exportExploded(tempFolder);

        for (Entry<ArchivePath, Node> entry : archive.getContent(Filters.include(JS_PATTERN)).entrySet()) {
            final String jsFilePath = (new StringBuilder()).append(QUnitConstants.TMP_FOLDER).append("/")
                .append(archive.getName()).append(entry.getValue()).toString();
            final File jsFile = new File(jsFilePath);
            final String initialJsContent = FileUtilities.readFile(jsFilePath);
            jsFile.delete();
            final String modifiedJsContent = new StringBuilder().append(QUNIT_READER).append(initialJsContent).toString();
            jsFile.createNewFile();
            FileUtilities.writeToFile(jsFile, modifiedJsContent);
        }
    }
}
