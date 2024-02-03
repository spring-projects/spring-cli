package org.springframework.cli.merger.ai;

import io.netty.handler.codec.http2.HttpConversionUtil;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.maven.MavenParser;
import org.springframework.cli.SpringCliException;
import org.springframework.cli.runtime.engine.actions.InjectMavenDependency;
import org.springframework.cli.runtime.engine.actions.handlers.AbstractInjectMavenActionHandler;
import org.springframework.cli.runtime.engine.actions.handlers.InjectMavenDependencyActionHandler;
import org.springframework.cli.runtime.engine.actions.handlers.json.ConversionUtils;
import org.springframework.cli.runtime.engine.actions.handlers.json.Lsp;
import org.springframework.cli.util.ClassNameExtractor;
import org.springframework.cli.util.MavenDependencyReader;
import org.springframework.cli.util.PomReader;
import org.springframework.cli.util.TerminalMessage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.cli.util.PropertyFileUtils.mergeProperties;

public class ProjectArtifactEditGenerator {
    private final List<ProjectArtifact> projectArtifacts;

    private final Path projectPath;

    private final String readmeFileName;

    private final TerminalMessage terminalMessage;

    private final Pattern compiledGroupIdPattern;

    private final Pattern compiledArtifactIdPattern;

    public ProjectArtifactEditGenerator(List<ProjectArtifact> projectArtifacts, Path projectPath, String readmeFileName, TerminalMessage terminalMessage) {
        this.projectArtifacts = projectArtifacts;
        this.projectPath = projectPath;
        this.readmeFileName = readmeFileName;
        this.terminalMessage = terminalMessage;
        compiledGroupIdPattern = Pattern.compile("<groupId>(.*?)</groupId>");
        compiledArtifactIdPattern = Pattern.compile("<artifactId>(.*?)</artifactId>");
    }

    public ProcessArtifactResult<Lsp.WorkspaceEdit> process() throws IOException {
        return processArtifacts(projectArtifacts, projectPath, terminalMessage);
    }

    private ProcessArtifactResult<Lsp.WorkspaceEdit> processArtifacts(List<ProjectArtifact> projectArtifacts, Path projectPath, TerminalMessage terminalMessage) throws IOException {
        ProcessArtifactResult<Lsp.WorkspaceEdit> processArtifactResult = new ProcessArtifactResult<>();
        String changeAnnotationId = UUID.randomUUID().toString();
        Lsp.WorkspaceEdit we = new Lsp.WorkspaceEdit(new ArrayList<>(), Map.of(changeAnnotationId, new Lsp.ChangeAnnotation("Apply %s".formatted(readmeFileName), true, "Spring CLI applied guide from markdown file")));
        for (ProjectArtifact projectArtifact : projectArtifacts) {
//            try {
                ProjectArtifactType artifactType = projectArtifact.getArtifactType();
                switch (artifactType) {
                    case SOURCE_CODE:
                        we.documentChanges().addAll(writeSourceCode(projectArtifact, projectPath, changeAnnotationId));
                        break;
                    case TEST_CODE:
                        we.documentChanges().addAll(writeTestCode(projectArtifact, projectPath, changeAnnotationId));
                        break;
                    case MAVEN_DEPENDENCIES:
                        we.documentChanges().addAll(writeMavenDependencies(projectArtifact, projectPath, terminalMessage, changeAnnotationId));
                        break;
                    case APPLICATION_PROPERTIES:
                        we.documentChanges().addAll(writeApplicationProperties(projectArtifact, projectPath, changeAnnotationId));
                        break;
                    case MAIN_CLASS:
                        we.documentChanges().addAll(updateMainApplicationClassAnnotations(projectArtifact, projectPath, terminalMessage, changeAnnotationId));
                        break;
                    case HTML:
                        we.documentChanges().addAll(writeHtml(projectArtifact, projectPath, changeAnnotationId));
                        break;
                    default:
                        processArtifactResult.addToNotProcessed(projectArtifact);
                        break;
                }
//            }
//            catch (IOException ex) {
//                ex.printStackTrace();
//                throw new SpringCliException("Could not write project artifact.", ex);
//            }
        }
        processArtifactResult.setResult(we);
        return processArtifactResult;
    }

    private List<Lsp.ChangeOperation> writeSourceCode(ProjectArtifact projectArtifact, Path projectPath, String changeAnnotationId) throws IOException {
        String packageName = this.calculatePackageForArtifact(projectArtifact);
        ClassNameExtractor classNameExtractor = new ClassNameExtractor();
        Optional<String> className = classNameExtractor.extractClassName(projectArtifact.getText());
        if (className.isPresent()) {
            Path output = resolveSourceFile(projectPath, packageName, className.get() + ".java");
            return createEdit(output, getFileContent(output), projectArtifact.getText(), changeAnnotationId);
        }
        return Collections.emptyList();
    }

    private List<Lsp.ChangeOperation> writeTestCode(ProjectArtifact projectArtifact, Path projectPath, String changeAnnotationId) throws IOException {
        // TODO parameterize better to reduce code duplication
        String packageName = this.calculatePackageForArtifact(projectArtifact);
        ClassNameExtractor classNameExtractor = new ClassNameExtractor();
        Optional<String> className = classNameExtractor.extractClassName(projectArtifact.getText());
        if (className.isPresent()) {
            Path output = resolveTestFile(projectPath, packageName, className.get() + ".java");
            return createEdit(output, getFileContent(output), projectArtifact.getText(), changeAnnotationId);
        }
        return Collections.emptyList();
    }

    private List<Lsp.ChangeOperation> writeMavenDependencies(ProjectArtifact projectArtifact, Path projectPath, TerminalMessage terminalMessage, String changeAnnotationId) {
        PomReader pomReader = new PomReader();
        Path currentProjectPomPath = this.projectPath.resolve("pom.xml");
        if (Files.notExists(currentProjectPomPath)) {
            throw new SpringCliException("Could not find pom.xml in " + this.projectPath + ".  Make sure you are running the command in the project's root directory.");
        }
        Model currentModel = pomReader.readPom(currentProjectPomPath.toFile());
        List<Dependency> currentDependencies = currentModel.getDependencies();


        MavenDependencyReader mavenDependencyReader = new MavenDependencyReader();
        //projectArtifact.getText() contains a list of <dependency> elements
        String[] mavenDependencies = mavenDependencyReader.parseMavenSection(projectArtifact.getText());

        List<Recipe> recipes = Arrays.stream(mavenDependencies)
                .filter(candidateDependencyText -> !candidateDependencyAlreadyPresent(getProjectDependency(candidateDependencyText), currentDependencies))
                .flatMap(candidateDependencyText -> {
                    InjectMavenDependencyActionHandler injectMavenDependencyActionHandler =
                            new InjectMavenDependencyActionHandler(null, new HashMap<>(), projectPath, terminalMessage);
                    InjectMavenDependency injectMavenDependency = new InjectMavenDependency(candidateDependencyText);
                    return injectMavenDependencyActionHandler.getRecipe(injectMavenDependency).stream();
                }).collect(Collectors.toList());

        if (!recipes.isEmpty()) {
            List<Result> res = AbstractInjectMavenActionHandler.runRecipe(currentProjectPomPath, recipes, projectPath).getChangeset().getAllResults();
            return convertToEdits(res, changeAnnotationId);
        }

        return Collections.emptyList();
    }

    private List<Lsp.ChangeOperation> convertToEdits(List<Result> allResults, String changeAnnotationId) {
        List<Lsp.ChangeOperation> edits = new ArrayList<>();
        for (Result res : allResults) {
            Path p = res.getBefore() == null ? res.getAfter().getSourcePath() : res.getBefore().getSourcePath();
            String uri = p.toUri().toASCIIString();
            ConversionUtils.computeTextDocEdit(uri, res.getBefore().printAll(), res.getAfter().printAll(), changeAnnotationId).ifPresent(edits::add);
        }
        return edits;
    }


    private boolean candidateDependencyAlreadyPresent(ProjectDependency toMergeDependency, List<Dependency> currentDependencies) {
        String candidateGroupId = toMergeDependency.getGroupId();
        String candidateArtifactId = toMergeDependency.getArtifactId();
        boolean candidateDependencyAlreadyPresent = false;
        for (Dependency currentDependency : currentDependencies) {
            String currentGroupId = currentDependency.getGroupId();
            String currentArtifactId = currentDependency.getArtifactId();
            if (candidateGroupId.equals(currentGroupId) && candidateArtifactId.equals(currentArtifactId)) {
                candidateDependencyAlreadyPresent = true;
                break;
            }
        }
        return candidateDependencyAlreadyPresent;

    }

    private String massageText(String text) {
        if (text.contains("<dependencies>")) {
            return text;
        } else {
            return "<dependencies>" + text + "</dependencies>";
        }
    }

    private ProjectDependency getProjectDependency(String xml) {
        String groupId = null;
        String artifactId = null;
        try {
            groupId = extractValue(xml, this.compiledGroupIdPattern);
            artifactId = extractValue(xml, this.compiledArtifactIdPattern);

        } catch (Exception ex) {
            throw new SpringCliException("Exception processing dependency: " + xml, ex);
        }
        if (groupId == null || artifactId == null) {
            throw new SpringCliException("Could not process dependency: " + xml);
        }
        return new ProjectDependency(groupId, artifactId);
    }

    private static String extractValue(String xml, Pattern compiledPattern) {
        Matcher matcher = compiledPattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private List<Lsp.ChangeOperation> writeApplicationProperties(ProjectArtifact projectArtifact, Path projectPath, String changeAnnotationId) throws IOException {
        Path applicationPropertiesPath =  projectPath.resolve("src").resolve("main").resolve("resources").resolve("application.properties");

        Properties srcProperties = new Properties();
        Properties destProperties = new Properties();
        srcProperties.load(IOUtils.toInputStream(projectArtifact.getText(), StandardCharsets.UTF_8));
        if (Files.exists(applicationPropertiesPath)) {
            destProperties.load(new FileInputStream(applicationPropertiesPath.toFile()));
        }
        Properties mergedProperties = mergeProperties(srcProperties, destProperties);

        StringWriter sw = new StringWriter();
        mergedProperties.store(sw, "updated by spring ai add");
        sw.flush();
        String newContent = sw.getBuffer().toString();

        return createEdit(applicationPropertiesPath, getFileContent(applicationPropertiesPath), newContent, changeAnnotationId);
    }

    private List<Lsp.ChangeOperation> updateMainApplicationClassAnnotations(ProjectArtifact projectArtifact, Path projectPath, TerminalMessage terminalMessage, String changeAnnotationId) {
        // TODO mer
        return Collections.emptyList();
    }

    private List<Lsp.ChangeOperation> writeHtml(ProjectArtifact projectArtifact, Path projectPath, String changeAnnotationId) throws IOException {
        String html = projectArtifact.getText();
        String fileName = extractFilenameFromComment(html);
        if (fileName != null) {
            Path htmlFile = projectPath.resolve(fileName);
            return createEdit(htmlFile, getFileContent(htmlFile), projectArtifact.getText(), changeAnnotationId);
        }
        return Collections.emptyList();
    }

    private String getFileContent(Path file) throws IOException {
        if (Files.exists(file)) {
            return Files.readString(file);
        }
        return "";
    }

    private List<Lsp.ChangeOperation> createEdit(Path file, String oldContent, String newContent, String changeAnnotationId) {
        List<Lsp.ChangeOperation> edits = new ArrayList<>();
        String uri = file.toUri().toASCIIString();
        if (!Files.exists(file)) {
            edits.add(new Lsp.CreateFile(uri, new Lsp.CreateFileOptions(true, false), changeAnnotationId));
            edits.add(new Lsp.TextDocumentEdit(new Lsp.TextDocumentIdentifier(uri), List.of(new Lsp.TextEdit(new Lsp.Range(new Lsp.Position(0,0), new Lsp.Position(0, 0)), newContent, changeAnnotationId))));
        } else {
            ConversionUtils.computeTextDocEdit(uri, oldContent, newContent, changeAnnotationId).ifPresent(edits::add);
        }
        return edits;
    }


    private String calculatePackageForArtifact(ProjectArtifact projectArtifact) {
        String packageToUse = "com.example.ai";
        try (BufferedReader reader = new BufferedReader(new StringReader(projectArtifact.getText()))) {
            String firstLine = reader.readLine();
            if (firstLine.contains("package")) {
                String regex = "^package\\s+([a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*);";
                Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                Matcher matcher = pattern.matcher(firstLine);
                // Find the package statement and extract the package name
                String packageName = "";
                if (matcher.find()) {
                    packageToUse = matcher.group(1);
                }
            }
        } catch (IOException e) {
            throw new SpringCliException("Could not parse package name from Project Artifact: " +
                    projectArtifact.getText());
        }
        return packageToUse;
    }

    private static String extractFilenameFromComment(String content) {
        String commentPattern = "<!--\\s*filename:\\s*(\\S+\\.html)\\s*-->";
        Pattern pattern = Pattern.compile(commentPattern);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public Path resolveSourceFile(Path projectPath, String packageName, String fileName) {
        Path sourceDirectory = projectPath.resolve("src").resolve("main").resolve("java");
        return resolvePackage(sourceDirectory, packageName).resolve(fileName);
    }

    public Path resolveTestFile(Path projectPath, String packageName, String fileName) {
        Path sourceDirectory = projectPath.resolve("src").resolve("test").resolve("java");
        return resolvePackage(sourceDirectory, packageName).resolve(fileName);
    }

    private static Path resolvePackage(Path directory, String packageName) {
        return directory.resolve(packageName.replace('.', '/'));
    }


//    private Path createSourceFile(Path projectPath, String packageName, String fileName) throws IOException {
//        Path sourceFile = resolveSourceFile(projectPath, packageName, fileName);
//        createFile(sourceFile);
//        return sourceFile;
//    }
//
//    private Path createTestFile(Path projectPath, String packageName, String fileName) throws IOException {
//        Path sourceFile = resolveTestFile(projectPath, packageName, fileName);
//        createFile(sourceFile);
//        return sourceFile;
//    }
//
//    public Path resolveSourceFile(Path projectPath, String packageName, String fileName) {
//        Path sourceDirectory = projectPath.resolve("src").resolve("main").resolve("java");
//        return resolvePackage(sourceDirectory, packageName).resolve(fileName);
//    }
//
//    public Path resolveTestFile(Path projectPath, String packageName, String fileName) {
//        Path sourceDirectory = projectPath.resolve("src").resolve("test").resolve("java");
//        return resolvePackage(sourceDirectory, packageName).resolve(fileName);
//    }
//
//    private static Path resolvePackage(Path directory, String packageName) {
//        return directory.resolve(packageName.replace('.', '/'));
//    }
//
//    private void createFile(Path file) throws IOException {
//        if (Files.exists(file)) {
//            //System.out.println("deleting file " + file.toAbsolutePath());
//            Files.delete(file);
//        }
//        Files.createDirectories(file.getParent());
//        if (Files.notExists(file)) {
//            Files.createFile(file);
//        }
//    }
}
