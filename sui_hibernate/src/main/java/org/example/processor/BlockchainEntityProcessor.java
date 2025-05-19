package org.example.processor;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.example.annotation.BlockchainEntity;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes("org.example.annotation.BlockchainEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class BlockchainEntityProcessor extends AbstractProcessor {
    private Configuration cfg;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/templates");
        env.getMessager().printMessage(Diagnostic.Kind.NOTE, "⦿ Initialized BlockchainEntityProcessor");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annos, RoundEnvironment roundEnv) {
        // first look for test prop, then fallback to production
        String moveDirProp = System.getProperty("test.move.dir", System.getProperty("sui.move.dir"));
        Path projectRoot   = moveDirProp != null ? Paths.get(moveDirProp) : null;

        // if we have a projectRoot, ensure Move.toml + sources dir exist
        if (projectRoot != null) {
            try {
                Files.createDirectories(projectRoot.resolve("sources"));
                Path toml = projectRoot.resolve("Move.toml");
                if (!Files.exists(toml)) {
                    Files.writeString(toml, """
                  [package]
                  name = "auto_pkg"
                  version = "0.0.1"
                  edition = "2024"

                  [addresses]
                  # fill these in your manager before publish

                  [dependencies]
                  """);
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                            "⦿ Scaffolded Move.toml at " + toml.toAbsolutePath());
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Failed to scaffold Move project: " + e.getMessage());
            }
        }

        for (Element e : roundEnv.getElementsAnnotatedWith(BlockchainEntity.class)) {
            TypeElement cls                 = (TypeElement) e;
            String       pkg                 = processingEnv.getElementUtils()
                    .getPackageOf(cls).getQualifiedName().toString();
            String       javaName            = cls.getSimpleName().toString();
            BlockchainEntity anno            = cls.getAnnotation(BlockchainEntity.class);
            String       module              = anno.module();
            String       struct              = anno.struct();

            // gather fields
            List<Map<String,String>> fields = new ArrayList<>();
            for (var ve : ElementFilter.fieldsIn(cls.getEnclosedElements())) {
                String name  = ve.getSimpleName().toString();
                String ftype = ve.asType().toString();
                String mtype = switch(ftype) {
                    case "java.lang.String"        -> "String";
                    case "long","java.lang.Long"   -> "u64";
                    case "boolean","java.lang.Boolean" -> "bool";
                    default                         -> "String";
                };
                fields.add(Map.of(
                        "name",     name,
                        "javaType", ftype,
                        "moveType", mtype
                ));
            }

            // generate .move file
            try {
                Template t = cfg.getTemplate("sui_box_module.move.ftl");
                Map<String,Object> ctx = Map.of(
                        "module", module,
                        "struct", struct,
                        "fields", fields
                );
                if (projectRoot != null) {
                    Path out = projectRoot.resolve("sources").resolve(module + ".move");
                    Files.createDirectories(out.getParent());
                    try (var w = Files.newBufferedWriter(out)) {
                        t.process(ctx, w);
                    }
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                            "⦿ Wrote Move file to " + out.toAbsolutePath());
                } else {
                    FileObject fo = processingEnv.getFiler()
                            .createResource(StandardLocation.SOURCE_OUTPUT, "", module + ".move");
                    try (var w = fo.openWriter()) {
                        t.process(ctx, w);
                    }
                }
            } catch (Throwable ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Move generation failed for " + module + ": " + ex.getMessage());
            }

            // generate Java model
            try {
                Template t = cfg.getTemplate("Model.java.ftl");
                String modelPkg = pkg.isBlank() ? "templates" : pkg + ".templates";
                Map<String,Object> ctx = Map.of(
                        "package",    modelPkg,
                        "className",  javaName,
                        "structName", struct,
                        "module",     module,
                        "fields",     fields
                );
                // if using test.java.dir, write there
                String javaDir = System.getProperty("test.java.dir");
                if (javaDir != null) {
                    Path out = Paths.get(javaDir)
                            .resolve(modelPkg.replace(".", "/"))
                            .resolve(javaName + ".java");
                    Files.createDirectories(out.getParent());
                    try (var w = Files.newBufferedWriter(out)) {
                        t.process(ctx, w);
                    }
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                            "⦿ Wrote Java model to " + out.toAbsolutePath());
                }
                else if (projectRoot != null) {
                    Path out = projectRoot
                            .resolve("src")
                            .resolve(modelPkg.replace(".", "/"))
                            .resolve(javaName + ".java");
                    Files.createDirectories(out.getParent());
                    try (var w = Files.newBufferedWriter(out)) {
                        t.process(ctx, w);
                    }
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                            "⦿ Wrote Java model to " + out.toAbsolutePath());
                } else {
                    JavaFileObject jfo = processingEnv.getFiler()
                            .createSourceFile(modelPkg + "." + javaName);
                    try (var w = jfo.openWriter()) {
                        t.process(ctx, w);
                    }
                }
            } catch (Throwable ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Java model failed for " + javaName + ": " + ex.getMessage());
            }
        }

        return true;
    }
}
