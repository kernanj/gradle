/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.testing.detection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.testing.fabric.TestFrameworkDetector;
import org.objectweb.asm.ClassReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * @author Tom Eyckmans
 */
public abstract class AbstractTestFrameworkDetector<T extends TestClassVisitor> implements TestFrameworkDetector {
    protected static final String CLASS_FILE_EXT = ".class";

    protected static final String TEST_CASE = "junit/framework/TestCase";
    protected static final String GROOVY_TEST_CASE = "groovy/util/GroovyTestCase";

    private final File testClassesDirectory;
    private final FileCollection testClasspath;
    private List<File> testClassDirectories;
    private ClassFileExtractionManager classFileExtractionManager;
    private final Map<File, Boolean> superClasses;

    protected TestClassProcessor testClassProcessor;

    protected List<String> knownTestCaseClassNames;

    protected AbstractTestFrameworkDetector(File testClassesDirectory, FileCollection testClasspath) {
        this.testClassesDirectory = testClassesDirectory;
        this.testClasspath = testClasspath;
        this.superClasses = new HashMap<File, Boolean>();
        this.knownTestCaseClassNames = new ArrayList<String>();
        addKnownTestCaseClassNames(TEST_CASE, GROOVY_TEST_CASE);
    }

    public File getTestClassesDirectory() {
        return testClassesDirectory;
    }

    protected abstract T createClassVisitor();

    protected File getSuperTestClassFile(String superClassName) {
        prepareClasspath();
        if (StringUtils.isEmpty(superClassName)) {
            throw new IllegalArgumentException("superClassName is empty!");
        }

        final Iterator<File> testClassDirectoriesIt = testClassDirectories.iterator();

        File superTestClassFile = null;
        while (superTestClassFile == null && testClassDirectoriesIt.hasNext()) {
            final File testClassDirectory = testClassDirectoriesIt.next();
            final File superTestClassFileCandidate = new File(testClassDirectory, superClassName + ".class");
            if (superTestClassFileCandidate.exists()) {
                superTestClassFile = superTestClassFileCandidate;
            }
        }

        if (superTestClassFile != null) {
            return superTestClassFile;
        } else { // super test class file not in test class directories
            return classFileExtractionManager.getLibraryClassFile(superClassName);
        }
    }

    private void prepareClasspath() {
        if (classFileExtractionManager != null) {
            return;
        }

        classFileExtractionManager = new ClassFileExtractionManager();
        testClassDirectories = new ArrayList<File>();

        testClassDirectories.add(testClassesDirectory);
        if (testClasspath != null) {
            for (File file : testClasspath) {
                if (file.isDirectory()) {
                    testClassDirectories.add(file);
                } else if (file.isFile() && file.getName().endsWith(".jar")) {
                    classFileExtractionManager.addLibraryJar(file);
                }
            }
        }
    }

    protected TestClassVisitor classVisitor(final File testClassFile) {
        final TestClassVisitor classVisitor = createClassVisitor();

        InputStream classStream = null;
        try {
            classStream = new BufferedInputStream(new FileInputStream(testClassFile));
            final ClassReader classReader = new ClassReader(classStream);
            classReader.accept(classVisitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
        } catch (Throwable e) {
            throw new GradleException("failed to read class file " + testClassFile.getAbsolutePath(), e);
        } finally {
            IOUtils.closeQuietly(classStream);
        }

        return classVisitor;
    }

    protected String classVisitorToClassFilename(final TestClassVisitor classVisitor) {
        final StrBuilder classFilenameBuilder = new StrBuilder();

        classFilenameBuilder.append(classVisitor.getClassName());
        classFilenameBuilder.append(CLASS_FILE_EXT);

        return classFilenameBuilder.toString();
    }

    public boolean processTestClass(File testClassFile) {
        return processTestClass(testClassFile, false);
    }

    protected abstract boolean processTestClass(File testClasFile, boolean superClass);

    protected boolean processSuperClass(File testClassFile) {
        boolean isTest = false;

        Boolean isSuperTest = superClasses.get(testClassFile);

        if (isSuperTest == null) {
            isTest = processTestClass(testClassFile, true);

            superClasses.put(testClassFile, isTest);
        } else {
            isTest = isSuperTest;
        }

        return isTest;
    }

    /**
     * In none super class mode a test class is published when the class is a test and it is not abstract. In super
     * class mode it musn't publish the class otherwise it will get published multiple times (for each extending
     * class).
     */
    protected void publishTestClass(boolean isTest, TestClassVisitor classVisitor, boolean superClass) {
        if (isTest && !classVisitor.isAbstract() && !superClass) {
            testClassProcessor.processTestClass(classVisitorToClassFilename(classVisitor));
        }
    }

    public void manualTestClass(String testClassName) {
        testClassProcessor.processTestClass(testClassName);
    }

    public void setTestClassProcessor(TestClassProcessor testClassProcessor) {
        this.testClassProcessor = testClassProcessor;
    }

    public void addKnownTestCaseClassNames(String... knownTestCaseClassNames) {
        if (knownTestCaseClassNames != null && knownTestCaseClassNames.length != 0) {
            for (String knownTestCaseClassName : knownTestCaseClassNames) {
                if (StringUtils.isNotEmpty(knownTestCaseClassName)) {
                    this.knownTestCaseClassNames.add(knownTestCaseClassName.replaceAll("\\.", "/"));
                }
            }
        }
    }

    protected boolean isKnownTestCaseClassName(String testCaseClassName) {
        boolean isKnownTestCase = false;

        if (StringUtils.isNotEmpty(testCaseClassName)) {
            final Iterator<String> knownTestCaseClassNamesIterator = knownTestCaseClassNames.iterator();
            while (!isKnownTestCase && knownTestCaseClassNamesIterator.hasNext()) {
                final String currentKnownTestCaseClassName = knownTestCaseClassNamesIterator.next();
                if (currentKnownTestCaseClassName.equals(testCaseClassName)) {
                    isKnownTestCase = true;
                }
            }
        }

        return isKnownTestCase;
    }
}
