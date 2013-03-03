/*
 * Copyright (c) 2010. All rights reserved.
 */
package ro.isdc.wro.extensions.processor;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.concurrent.Callable;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.config.Context;
import ro.isdc.wro.extensions.processor.js.GoogleClosureCompressorProcessor;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.ResourceType;
import ro.isdc.wro.model.resource.processor.ResourceProcessor;
import ro.isdc.wro.model.resource.processor.decorator.LazyProcessorDecorator;
import ro.isdc.wro.util.LazyInitializer;
import ro.isdc.wro.util.WroTestUtils;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.SourceFile;


/**
 * Test google closure js processor.
 *
 * @author Alex Objelean
 * @created Created on Apr 18, 2010
 */
public class TestGoogleClosureCompressorProcessor {
  private File testFolder;
  private GoogleClosureCompressorProcessor victim;
  @Before
  public void setUp() {
    testFolder = new File(ClassLoader.getSystemResource("test").getFile());
    victim = new GoogleClosureCompressorProcessor() {
      @Override
      protected CompilerOptions newCompilerOptions() {
        final CompilerOptions options = super.newCompilerOptions();
        // explicitly set this to null to make test pass also when running mvn test from command line.
        // the reason are some weird characters used in jquery-core
        options.setOutputCharset(null);
        return options;
      }
    };
    Context.set(Context.standaloneContext());
    WroTestUtils.createInjector().inject(victim);
  }

  @After
  public void tearDown() {
    Context.unset();
  }

  @Test
  public void testWhiteSpaceOnly()
      throws IOException {
    victim.setCompilationLevel(CompilationLevel.WHITESPACE_ONLY);
    final URL url = getClass().getResource("google");

    final File expectedFolder = new File(url.getFile(), "expectedWhitespaceOnly");
    WroTestUtils.compareFromDifferentFoldersByExtension(testFolder, expectedFolder, "js", victim);
  }

  @Test
  public void testSimpleOptimization()
      throws IOException {
    victim.setCompilationLevel(CompilationLevel.SIMPLE_OPTIMIZATIONS);
    final URL url = getClass().getResource("google");

    final File expectedFolder = new File(url.getFile(), "expectedSimple");
    WroTestUtils.compareFromDifferentFoldersByExtension(testFolder, expectedFolder, "js", victim);
  }

  @Test
  public void testAdvancedOptimization()
      throws IOException {
    victim.setCompilationLevel(CompilationLevel.ADVANCED_OPTIMIZATIONS);
    final URL url = getClass().getResource("google");

    final File expectedFolder = new File(url.getFile(), "expectedAdvanced");
    WroTestUtils.compareFromDifferentFoldersByExtension(testFolder, expectedFolder, "js", victim);
  }

  @Test
  public void shouldAcceptNullExterns()
      throws IOException {
    victim = new GoogleClosureCompressorProcessor(CompilationLevel.ADVANCED_OPTIMIZATIONS) {
      @Override
      protected JSSourceFile[] getExterns(final Resource resource) {
        return null;
      }
    };
    final StringWriter sw = new StringWriter();
    WroTestUtils.createInjector().inject(victim);

    victim.process(null, new StringReader("function test( ) {}"), sw);
    Assert.assertEquals("", sw.toString());
  }

  @Test(expected=WroRuntimeException.class)
  public void shouldFailWhenInvalidExternProvided()
      throws IOException {
    victim = new GoogleClosureCompressorProcessor(CompilationLevel.ADVANCED_OPTIMIZATIONS) {
      @Override
      protected SourceFile[] getExterns(final Resource resource) {
        return new SourceFile[] {
          SourceFile.fromFile(new File("INVALID"))
        };
      }
    };
    WroTestUtils.createInjector().inject(victim);

    final StringWriter sw = new StringWriter();
    victim.process(null, new StringReader("alert(1);"), sw);
    //will leave result unchanged, because the processing is not successful.
    Assert.assertEquals("alert(1);", sw.toString());
  }

  @Test
  public void shouldBeThreadSafe()
      throws Exception {
    WroTestUtils.runConcurrently(new Callable<Void>() {
      @Override
      public Void call()
          throws Exception {
        victim.process(null, new StringReader("alert(1);"), new StringWriter());
        return null;
      }
    });
  }

  @Test
  public void shouldSupportCorrectResourceTypes() {
    WroTestUtils.assertProcessorSupportResourceTypes(new GoogleClosureCompressorProcessor(), ResourceType.JS);
  }

  @Test
  public void shouldMinimizeWhenUsedAsLazyProcessor() throws Exception {
    final ResourceProcessor victim = new LazyProcessorDecorator(new LazyInitializer<ResourceProcessor>() {
      @Override
      protected ResourceProcessor initialize() {
        return new GoogleClosureCompressorProcessor();
      }
    });
    WroTestUtils.createInjector().inject(victim);
    final StringWriter sw = new StringWriter();
    victim.process(null, new StringReader("alert(1);"), sw);
    Assert.assertEquals("alert(1);", sw.toString());
  }
}
