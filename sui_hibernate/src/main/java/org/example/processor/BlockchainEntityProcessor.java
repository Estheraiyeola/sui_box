package org.example.processor;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.example.annotation.BlockchainEntity;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@SupportedAnnotationTypes("org.example.annotation.BlockchainEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class BlockchainEntityProcessor extends AbstractProcessor {
    private Configuration cfg;
    private static final String MOVE_DIR = "move";
    private static final String BUILD_JAVA_DIR = "target/build/java";

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/templates");
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Initialized BlockchainEntityProcessor");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing @BlockchainEntity annotations");
        boolean registryGenerated = false;
        List<String> allStructs = new ArrayList<>();

        for (Element e : roundEnv.getElementsAnnotatedWith(BlockchainEntity.class)) {
            TypeElement cls = (TypeElement) e;
            String pkg = processingEnv.getElementUtils().getPackageOf(cls).getQualifiedName().toString();
            String name = cls.getSimpleName().toString();
            BlockchainEntity ann = cls.getAnnotation(BlockchainEntity.class);
            String moveModule = ann.module();
            String moveStruct = ann.struct();
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing class: " + pkg + "." + name + ", module: " + moveModule);
            allStructs.add(moveStruct);

            var fields = new ArrayList<Map<String, String>>();
            for (VariableElement ve : ElementFilter.fieldsIn(cls.getEnclosedElements())) {
                String fname = ve.getSimpleName().toString();
                String ftype = ve.asType().toString();
                String moveType = switch (ftype) {
                    case "java.lang.String" -> "String";
                    case "long", "java.lang.Long" -> "u64";
                    case "boolean", "java.lang.Boolean" -> "bool";
                    default -> "String";
                };
                var map = new HashMap<String, String>();
                map.put("name", fname);
                map.put("javaType", ftype);
                map.put("moveType", moveType);
                fields.add(map);
            }

            // Generate Move file
            try {
                Template moveTpl = cfg.getTemplate("sui_box_module.move.ftl");
                Map<String, Object> ctx = new HashMap<>();
                ctx.put("module", moveModule);
                ctx.put("struct", moveStruct);
                ctx.put("fields", fields);

                String testMoveDir = System.getProperty("test.move.dir");
                if (testMoveDir != null) {
                    Path movePath = Paths.get(testMoveDir, "sources", moveModule + ".move");
                    Files.createDirectories(movePath.getParent());
                    try (Writer w = Files.newBufferedWriter(movePath)) {
                        moveTpl.process(ctx, w);
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                "Wrote Move file to test dir: " + movePath.toAbsolutePath());
                    }
                } else {
                    FileObject moveOut = processingEnv.getFiler()
                            .createResource(StandardLocation.SOURCE_OUTPUT, "", moveModule + ".move");
                    try (Writer w = moveOut.openWriter()) {
                        moveTpl.process(ctx, w);
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                "Wrote Move file: " + moveModule + ".move");
                    }
                }
            } catch (IOException | TemplateException ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Failed to generate Move file for " + moveModule + ": " + ex.getMessage());
            }

            // Generate Java model
            try {
                Template modelTpl = cfg.getTemplate("Model.java.ftl");
                String modelPackage = pkg.isEmpty() ? "templates" : pkg + ".templates";
                Map<String, Object> modelCtx = new HashMap<>();
                modelCtx.put("package", modelPackage);
                modelCtx.put("className", name);
                modelCtx.put("structName", moveStruct);
                modelCtx.put("fields", fields);

                String testJavaDir = System.getProperty("test.java.dir");
                if (testJavaDir != null) {
                    Path javaPath = Paths.get(testJavaDir, modelPackage.replace(".", "/"), name + ".java");
                    Files.createDirectories(javaPath.getParent());
                    try (Writer w = Files.newBufferedWriter(javaPath)) {
                        modelTpl.process(modelCtx, w);
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                "Wrote Java model to test dir: " + javaPath.toAbsolutePath());
                    }
                } else {
                    JavaFileObject modelOut = processingEnv.getFiler()
                            .createSourceFile(modelPackage + "." + name);
                    try (Writer w = modelOut.openWriter()) {
                        modelTpl.process(modelCtx, w);
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                "Wrote Java model: " + modelPackage + "." + name);
                    }
                }
            } catch (IOException | TemplateException ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Failed to generate Java model for " + name + ": " + ex.getMessage());
            }
        }

        if (!registryGenerated && !roundEnv.getElementsAnnotatedWith(BlockchainEntity.class).isEmpty()) {
            try {
                Template regTpl = cfg.getTemplate("ModelRegistry.java.ftl");
                Map<String, Object> ctx = new HashMap<>();
                ctx.put("registryPkg", "org.example.templates");

                String testJavaDir = System.getProperty("test.java.dir");
                if (testJavaDir != null) {
                    Path regPath = Paths.get(testJavaDir, "org/example/templates/ModelRegistry.java");
                    Files.createDirectories(regPath.getParent());
                    try (Writer w = Files.newBufferedWriter(regPath)) {
                        regTpl.process(ctx, w);
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                "Wrote ModelRegistry to test dir: " + regPath.toAbsolutePath());
                    }
                } else {
                    JavaFileObject regOut = processingEnv.getFiler()
                            .createSourceFile("org.example.templates.ModelRegistry");
                    try (Writer w = regOut.openWriter()) {
                        regTpl.process(ctx, w);
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                "Wrote ModelRegistry: org.example.templates.ModelRegistry");
                    }
                }
                registryGenerated = true;
            } catch (IOException | TemplateException ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Failed to generate ModelRegistry: " + ex.getMessage());
            }
        }

        return true;
    }
}