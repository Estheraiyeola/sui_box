// src/main/java/org/example/processor/BlockchainEntityProcessor.java
package org.example.templates;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.example.annotation.BlockchainEntity;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

@SupportedAnnotationTypes("org.example.annotation.BlockchainEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class BlockchainEntityProcessor extends AbstractProcessor {
    private Configuration cfg;
    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassLoaderForTemplateLoading(
                getClass().getClassLoader(), "/templates"
        );
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Track whether the registry has been generated to avoid duplicates
        boolean registryGenerated = false;

        // Collect all struct names for potential use in templates or logging
        List<String> allStructs = new ArrayList<>();

        // Process each @BlockchainEntity annotated class
        for (Element e : roundEnv.getElementsAnnotatedWith(BlockchainEntity.class)) {
            TypeElement cls = (TypeElement) e;
            String pkg = processingEnv.getElementUtils()
                    .getPackageOf(cls).getQualifiedName().toString();
            String name = cls.getSimpleName().toString();
            BlockchainEntity ann = cls.getAnnotation(BlockchainEntity.class);
            String moveModule = ann.module();
            String moveStruct = ann.struct();

            allStructs.add(moveStruct);

            // Collect fields for the model
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

            try {
                // Generate Move code
                Template moveTpl = cfg.getTemplate("sui_box_module.move.ftl");
                Map<String, Object> ctx = new HashMap<>();
                ctx.put("module", moveModule);
                ctx.put("struct", moveStruct);
                ctx.put("fields", fields);
                FileObject moveOut = processingEnv.getFiler()
                        .createResource(StandardLocation.SOURCE_OUTPUT, "", moveModule + ".move");
                try (Writer w = moveOut.openWriter()) {
                    moveTpl.process(ctx, w);
                }

                // Generate Java model class
                Template modelTpl = cfg.getTemplate("Model.java.ftl");
                Map<String, Object> modelCtx = new HashMap<>();
                modelCtx.put("package", pkg + ".templates");
                modelCtx.put("className", name);
                modelCtx.put("structName", moveStruct);  // Use moveStruct for annotation
                modelCtx.put("fields", fields);
                JavaFileObject modelOut = processingEnv.getFiler()
                        .createSourceFile(pkg + ".templates." + name);
                try (Writer w = modelOut.openWriter()) {
                    modelTpl.process(modelCtx, w);
                }
            } catch (IOException | TemplateException ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.getMessage());
            }
        }

        // Generate ModelRegistry once, if any @BlockchainEntity classes are present
        if (!registryGenerated && !roundEnv.getElementsAnnotatedWith(BlockchainEntity.class).isEmpty()) {
            try {
                Template regTpl = cfg.getTemplate("ModelRegistry.java.ftl");
                Map<String, Object> ctx = new HashMap<>();
                ctx.put("registryPkg", "org.example.templates");  // Adjust if necessary
                JavaFileObject regOut = processingEnv.getFiler()
                        .createSourceFile("org.example.templates.ModelRegistry");
                try (Writer w = regOut.openWriter()) {
                    regTpl.process(ctx, w);
                }
                registryGenerated = true;
            } catch (IOException | TemplateException ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.getMessage());
            }
        }

        return true;
    }

}
