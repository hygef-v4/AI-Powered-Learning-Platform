package edu.aiplatform;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Map;

/** Quy tắc khung: tên interface, ranh giới unit. */
@AnalyzeClasses(packages = "edu.aiplatform",
        importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludePackageInfos.class})
class ArchitectureTest {

    /** Package → số unit; package cùng số là cùng một unit. */
    static final Map<String, Integer> UNIT_OF_PACKAGE = Map.ofEntries(
            Map.entry("identity", 1), Map.entry("audit", 2),
            Map.entry("files", 3), Map.entry("jobs", 3), Map.entry("settings", 3), Map.entry("backup", 3),
            Map.entry("subjects", 4), Map.entry("academics", 5), Map.entry("ai", 6),
            Map.entry("assignments", 7), Map.entry("assignmentwork", 8), Map.entry("grading", 9),
            Map.entry("quizzes", 10), Map.entry("quizpractice", 11),
            Map.entry("groups", 12), Map.entry("groupdocs", 12), Map.entry("materials", 13),
            Map.entry("announcements", 14), Map.entry("notifications", 14), Map.entry("admindashboard", 15));

    @ArchTest
    static final ArchRule interfaces_are_named_with_I_prefix = classes()
            .that().areInterfaces().and().areNotAnnotations()
            .should().haveNameMatching(".*[.$]I[A-Z][A-Za-z0-9]*")
            .because("interface đặt tên dạng Ixxx, không dùng đuôi Port");

    @ArchTest
    static final ArchRule unit_internals_are_private = classes()
            .that().resideInAnyPackage("..service..", "..model..", "..repository..", "..dto..", "..config..", "..integration..", "..controller..")
            .should(onlyBeAccessedFromTheSameUnit())
            .allowEmptyShould(true)
            .because("unit khác chỉ dùng package contract");

    static Integer unitOf(JavaClass javaClass) {
        String name = javaClass.getPackageName();
        String prefix = "edu.aiplatform.";
        if (!name.startsWith(prefix)) {
            return null;
        }
        String rest = name.substring(prefix.length());
        int dot = rest.indexOf('.');
        return UNIT_OF_PACKAGE.get(dot < 0 ? rest : rest.substring(0, dot));
    }

    static ArchCondition<JavaClass> onlyBeAccessedFromTheSameUnit() {
        return new ArchCondition<>("only be accessed from the same unit") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                Integer unit = unitOf(item);
                for (Dependency dependency : item.getDirectDependenciesToSelf()) {
                    Integer origin = unitOf(dependency.getOriginClass());
                    if (origin != null && !origin.equals(unit)) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }
}
