package com.contractdetector.impact;

import com.contractdetector.change.SchemaChange;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PojoDiscoveryService {

    private final JavaParser javaParser;


    public List<POJOClass> findAffectedPojos(List<SchemaChange> changes,
                                             Path mainSourceDir) {
        List<POJOClass> affectedPojos = new ArrayList<>();

        List<String> changedFields = changes.stream()
                                            .map(SchemaChange::getFieldName)
                                            .distinct()
                                            .collect(Collectors.toList());

        try {
            Files.walk(mainSourceDir)
                 .filter(path -> path.toString().endsWith(".java"))
                 .forEach(file -> {
                     try {
                         analyzePojoFile(file, changedFields, affectedPojos);
                     } catch (IOException e) {
                         log.warn("Failed to parse: {}", file, e);
                     }
                 });

        } catch (IOException e) {
            log.error("Failed to walk source directory", e);
        }

        log.info("Found {} affected POJOs", affectedPojos.size());
        return affectedPojos;
    }

    private void analyzePojoFile(Path file,
                                 List<String> changedFields,
                                 List<POJOClass> affectedPojos) throws IOException {

        CompilationUnit cu = javaParser.parse(file).getResult()
                                       .orElseThrow(() -> new IOException("Failed to parse: " + file));

        String packageName = cu.getPackageDeclaration()
                               .map(pd -> pd.getNameAsString())
                               .orElse("");

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {

            List<POJOClass.POJOField> matchingFields = new ArrayList<>();
            List<String> jsonAnnotations = new ArrayList<>();

            for (FieldDeclaration field : clazz.getFields()) {

                String fieldName = field.getVariable(0).getNameAsString();

                String jsonPropertyName = getJsonPropertyName(field);
                String effectiveName = jsonPropertyName != null ?
                        jsonPropertyName : fieldName;

                if (changedFields.contains(effectiveName) ||
                        changedFields.contains(fieldName)) {

                    matchingFields.add(POJOClass.POJOField.builder()
                                                          .name(fieldName)
                                                          .type(field.getVariable(0).getType().asString())
                                                          .jsonPropertyName(jsonPropertyName)
                                                          .annotations(field.getAnnotations().stream()
                                                                            .map(AnnotationExpr::toString)
                                                                            .collect(Collectors.toList()))
                                                          .build());

                    jsonAnnotations.addAll(field.getAnnotations().stream()
                                                .map(AnnotationExpr::toString)
                                                .collect(Collectors.toList()));
                }
            }

            if (!matchingFields.isEmpty()) {
                affectedPojos.add(POJOClass.builder()
                                           .id(java.util.UUID.randomUUID().toString())
                                           .filePath(file)
                                           .className(clazz.getNameAsString())
                                           .packageName(packageName)
                                           .fields(matchingFields)
                                           .matchingChanges(changedFields.stream()
                                                                        .filter(cf -> matchingFields.stream()
                                                                                                    .anyMatch(mf -> mf.getName().equals(cf) ||
                                                                                                            mf.getJsonPropertyName().equals(cf)))
                                                                        .collect(Collectors.toList()))
                                           .hasJsonAnnotations(!jsonAnnotations.isEmpty())
                                           .jsonAnnotations(jsonAnnotations)
                                           .build());
            }
        });
    }


    private String getJsonPropertyName(FieldDeclaration field) {
        Optional<AnnotationExpr> jsonProperty = field.getAnnotations().stream()
                                                     .filter(a -> a.getNameAsString().equals("JsonProperty"))
                                                     .findFirst();

        if (jsonProperty.isPresent()) {
            String annotation = jsonProperty.get().toString();

            if (annotation.contains("(")) {
                String value = annotation.replaceAll(
                        ".*@JsonProperty\\(\"([^\"]+)\"\\).*", "$1");
                if (!value.equals(annotation)) return value;

                value = annotation.replaceAll(
                        ".*@JsonProperty\\(value = \"([^\"]+)\"\\).*", "$1");
                if (!value.equals(annotation)) return value;
            }
        }

        return null;
    }
}