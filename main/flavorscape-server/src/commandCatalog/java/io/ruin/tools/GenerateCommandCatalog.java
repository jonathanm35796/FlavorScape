package io.ruin.tools;

import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Reads Java syntax, never executes command handlers or includes commented-out cases. */
public final class GenerateCommandCatalog {
    private static final Map<String, String> SECTIONS = Map.of(
        "handleRegular", "REGULAR", "handleSupport", "SUPPORT", "handleMod", "MODERATOR",
        "handleSeniorMod", "SENIOR_MODERATOR", "handleAdmin", "ADMINISTRATOR");

    public static void main(String[] args) throws Exception { generate(Path.of(args[0]), Path.of(args[1])); }

    public static void generate(Path source, Path output) throws Exception {
        List<String> rows = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Set<String> methods = new HashSet<>();
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Command catalogue generation requires a JDK");
        try (var files = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostic -> {
                if (diagnostic.getKind() == javax.tools.Diagnostic.Kind.ERROR)
                    throw new IllegalStateException(diagnostic.toString());
            }, List.of("-proc:none"), null, files.getJavaFileObjects(source));
            for (CompilationUnitTree unit : task.parse()) {
                new TreeScanner<Void, Void>() {
                    @Override public Void visitMethod(MethodTree method, Void unused) {
                        String section = SECTIONS.get(method.getName().toString());
                        if (section == null) return null;
                        methods.add(method.getName().toString());
                        SwitchTree commands = null;
                        for (StatementTree statement : method.getBody().getStatements()) {
                            if (statement instanceof SwitchTree candidate &&
                                    candidate.getExpression().toString().replace("(", "").replace(")", "").equals("command"))
                                commands = candidate;
                        }
                        if (commands == null) throw new IllegalStateException("No command switch in " + method.getName());
                        List<String> aliases = new ArrayList<>();
                        for (CaseTree commandCase : commands.getCases()) {
                            for (ExpressionTree expression : commandCase.getExpressions()) {
                                if (!(expression instanceof LiteralTree literal) || !(literal.getValue() instanceof String name))
                                    throw new IllegalStateException("Command names must be string literals");
                                if (!seen.add(section + ":" + name)) throw new IllegalStateException("Duplicate command: " + name);
                                aliases.add(name);
                            }
                            if (commandCase.getStatements() == null || commandCase.getStatements().isEmpty()) continue;
                            if (aliases.isEmpty()) continue;
                            List<String> examples = new ArrayList<>();
                            new TreeScanner<Void, Void>() {
                                @Override public Void visitLiteral(LiteralTree literal, Void v) {
                                    if (literal.getValue() instanceof String text && text.startsWith("::")) {
                                        String name = text.substring(2).split("\\s+", 2)[0];
                                        if (aliases.contains(name)) examples.add(text);
                                    }
                                    return null;
                                }
                            }.scan(commandCase.getStatements(), null);
                            String body = commandCase.getStatements().toString();
                            boolean restrictedManager = body.contains("if (isCommunityManager)") || body.contains("if(isCommunityManager)");
                            String example = examples.isEmpty() ? "" : examples.get(0).replace('\t', ' ').replace('\n', ' ');
                            rows.add(section + "\t" + String.join(",", aliases) + "\t" + restrictedManager + "\t" + example);
                            aliases.clear();
                        }
                        return null;
                    }
                }.scan(unit, null);
            }
        }
        if (!methods.equals(SECTIONS.keySet())) throw new IllegalStateException("Missing command handler sections: " + methods);
        Files.createDirectories(output.getParent());
        Files.write(output, rows, StandardCharsets.UTF_8);
        System.out.println("Indexed " + seen.size() + " command names across " + rows.size() + " alias groups");
    }
}
