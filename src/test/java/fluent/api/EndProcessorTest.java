/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2018, Ondrej Fischer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package fluent.api;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.tools.*;
import java.io.File;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.List;

import static fluent.api.EndProcessorTest.Expectation.FAIL;
import static fluent.api.EndProcessorTest.Expectation.PASS;
import static java.util.Collections.emptyList;

public class EndProcessorTest {

    enum Expectation {PASS, FAIL;
        @Override public String toString() {
            return "Compilation should " + name().toLowerCase() + " when ";
        }
    }

    @DataProvider
    public static Object[][] sourceFiles() {
        return new Object[][]{
                {PASS, "EndMethodNotMissing"},
                {PASS, "PassThroughEndMethodNotMissing"},
                {PASS, "EndMethodMissingInAssignment"},
                {PASS, "EndMethodCheckIgnored"},
                {PASS, "EndMethodNotMissingInNesting"},
                {PASS, "NestedEndMethodNotMissing"},
                {PASS, "ExternalEndMethodNotMissing"},
                {PASS, "ExternalGenericEndMethodNotMissing"},
                {PASS, "ExternalGenericEndMethodWithParameterNotMissing"},
                {PASS, "ExternalGenericEndMethodWithGenericParameterNotMissing"},

                {FAIL, "ImmediateEndMethodMissing"},
                {FAIL, "ImmediateEndMethodMissingAfterConstructor"},
                {FAIL, "EndMethodMissing"},
                {FAIL, "EndMethodMissingInNesting"},
                {FAIL, "UnmarkedEndMethod"},
                {FAIL, "NestedEndMethodMissing"},
                {FAIL, "ExternalEndMethodMissing"},
                {FAIL, "ExternalGenericEndMethodMissing"},
                {FAIL, "ImmediateEndMethodMissingAfterAnonymousClass"}
        };
    }

    @Test(dataProvider = "sourceFiles")
    public void test(Expectation expected, String className) throws URISyntaxException {
        DiagnosticCollector<JavaFileObject> listener = new DiagnosticCollector<>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjects(new File(getClass().getResource(className + ".java").toURI()));
        JavaCompiler.CompilationTask task = compiler.getTask(new StringWriter(), fileManager, listener, emptyList(), null, fileObjects);
        boolean result = task.call();
        List<Diagnostic<? extends JavaFileObject>> diagnostics = listener.getDiagnostics();
        if (!diagnostics.isEmpty()) {
            System.out.println(diagnostics);
        }
        Assert.assertEquals(result, expected == PASS, diagnostics.toString());
    }

}
