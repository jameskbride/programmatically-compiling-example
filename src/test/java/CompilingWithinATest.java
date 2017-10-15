import org.junit.Test;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.*;

public class CompilingWithinATest {

    @Test
    public void itCanCompileFromTheResourceDirectory() throws IOException, ClassNotFoundException, NoSuchMethodException, URISyntaxException {
        File libraryFile = new File(this.getClass().getClassLoader().getResource("Library.java").toURI());

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        Iterable<? extends JavaFileObject> compilationUnits1 =
                fileManager.getJavaFileObjectsFromFiles(Arrays.asList(libraryFile));
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask compilerTask = compiler.getTask(null, fileManager, diagnosticCollector, null, null, compilationUnits1);
        compilerTask.setProcessors(Arrays.asList(new EmptyAnnotationProcessor()));
        boolean success = compilerTask.call();

        fileManager.close();

        assertTrue(success);
        assertSame(boolean.class, Class.forName("Library").getDeclaredMethod("someLibraryMethod").getReturnType());
        Optional<Diagnostic<? extends JavaFileObject>> foundErrors = diagnosticCollector.getDiagnostics()
                .stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                .findAny();
        assertFalse(foundErrors.isPresent());
    }

    @Test
    public void itCanReportOnErrorsFromTheResourceDirectory() throws URISyntaxException, IOException, ClassNotFoundException, NoSuchMethodException {
        File libraryFile = new File(this.getClass().getClassLoader().getResource("NonCompilingLibrary.java").toURI());

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        Iterable<? extends JavaFileObject> compilationUnits1 =
                fileManager.getJavaFileObjectsFromFiles(Arrays.asList(libraryFile));
        JavaCompiler.CompilationTask compilerTask = compiler.getTask(null, fileManager, diagnosticCollector, null, null, compilationUnits1);
        compilerTask.setProcessors(Arrays.asList(new EmptyAnnotationProcessor()));
        boolean success = compilerTask.call();

        fileManager.close();

        assertFalse(success);

        Optional<Diagnostic<? extends JavaFileObject>> foundErrors = diagnosticCollector.getDiagnostics()
                .stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                .findAny();

        assertTrue(foundErrors.isPresent());
        assertEquals(2, foundErrors.get().getLineNumber());
    }
}
