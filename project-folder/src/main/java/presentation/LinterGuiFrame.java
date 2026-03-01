package presentation;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;

import datastorage.ConfigLoader;
import datastorage.FileLoader;
import domain.Linter;
import domain.LinterConfig;

public class LinterGuiFrame extends JFrame {

    private static final String DEFAULT_CONFIG_PATH = "linter.properties";
    private static final String SELECT_LINTER_PROMPT = "Select linter to run...";
    private static final String SELECT_EXISTING_PATH_PROMPT = "Select compatible project file...";
    private static final String BROWSE_PATH_OPTION = "Browse for another file/folder...";

    private final Path projectRoot;
    private final List<String> allRegisteredLinterNames;

    private final JTextField inputPathsField;
    private final JComboBox<String> linterDropdown;
    private final JComboBox<String> projectPathsDropdown;

    private final JTextField configPathField;
    private final JCheckBox enableAllLintersCheckBox;
    private final JTextField enabledLintersField;
    private final JSpinner tooManyParametersSpinner;
    private final JSpinner srpThresholdSpinner;

    private final JTextArea outputArea;
    private final JLabel statusLabel;
    private final JButton runButton;

    public LinterGuiFrame() {
        super("Java Linter GUI");

        this.projectRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        this.allRegisteredLinterNames = discoverAllRegisteredLinterNames();

        this.inputPathsField = new JTextField();
        this.linterDropdown = new JComboBox<>();
        this.projectPathsDropdown = new JComboBox<>();

        this.configPathField = new JTextField(DEFAULT_CONFIG_PATH);
        this.enableAllLintersCheckBox = new JCheckBox("enabled_linters=all", true);
        this.enabledLintersField = new JTextField();
        this.tooManyParametersSpinner = new JSpinner(
                new SpinnerNumberModel(LinterConfig.DEFAULT_TOO_MANY_PARAMETERS_LIMIT, 1, 50, 1));
        this.srpThresholdSpinner = new JSpinner(
                new SpinnerNumberModel(LinterConfig.DEFAULT_SRP_LCOM_THRESHOLD, 1, 20, 1));

        this.outputArea = new JTextArea();
        this.statusLabel = new JLabel("Ready");
        this.runButton = new JButton("Run Selected Linter");

        initializeFrame();
        buildLayout();
        wireActions();
        loadLinterDropdownOptions();
        loadConfigIntoControls(configPathField.getText().trim());
    }

    private void initializeFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 760);
        setLocationRelativeTo(null);
    }

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        root.add(buildControlsPanel(), BorderLayout.NORTH);
        root.add(buildOutputPanel(), BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildControlsPanel() {
        JPanel controls = new JPanel(new BorderLayout(8, 8));

        JPanel linterPanel = new JPanel(new BorderLayout(8, 4));
        linterPanel.add(new JLabel("Linter to run (always shows all available linters):"), BorderLayout.NORTH);
        linterPanel.add(linterDropdown, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(8, 4));
        inputPanel.add(new JLabel("Selected input path(s):"), BorderLayout.NORTH);

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.add(inputPathsField, BorderLayout.CENTER);

        JPanel inputButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton browseInputButton = new JButton("Browse...");
        JButton clearInputButton = new JButton("Clear");
        inputButtons.add(browseInputButton);
        inputButtons.add(clearInputButton);
        inputRow.add(inputButtons, BorderLayout.EAST);
        inputPanel.add(inputRow, BorderLayout.CENTER);

        JPanel projectPathsPanel = new JPanel(new BorderLayout(8, 4));
        projectPathsPanel.add(new JLabel("Compatible project files for selected linter:"), BorderLayout.NORTH);

        JPanel projectPathsRow = new JPanel(new BorderLayout(8, 0));
        projectPathsRow.add(projectPathsDropdown, BorderLayout.CENTER);

        JPanel projectPathsButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton addSelectedButton = new JButton("Add Selected");
        JButton refreshFilesButton = new JButton("Refresh Files");
        projectPathsButtons.add(addSelectedButton);
        projectPathsButtons.add(refreshFilesButton);
        projectPathsRow.add(projectPathsButtons, BorderLayout.EAST);
        projectPathsPanel.add(projectPathsRow, BorderLayout.CENTER);

        JPanel configPanel = buildConfigPanel();

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton clearOutputButton = new JButton("Clear Output");
        actionPanel.add(runButton);
        actionPanel.add(clearOutputButton);

        JPanel grouped = new JPanel(new GridLayout(0, 1, 0, 8));
        grouped.add(linterPanel);
        grouped.add(inputPanel);
        grouped.add(projectPathsPanel);
        grouped.add(configPanel);
        grouped.add(actionPanel);

        controls.add(grouped, BorderLayout.CENTER);

        browseInputButton.addActionListener(event -> chooseInputPaths());
        clearInputButton.addActionListener(event -> inputPathsField.setText(""));
        addSelectedButton.addActionListener(event -> addSelectedProjectPath());
        refreshFilesButton.addActionListener(event -> loadProjectPathOptions());
        clearOutputButton.addActionListener(event -> outputArea.setText(""));

        return controls;
    }

    private JPanel buildConfigPanel() {
        JPanel configPanel = new JPanel(new BorderLayout(8, 6));
        configPanel.setBorder(BorderFactory.createTitledBorder("Config controls (maps to linter.properties)"));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1.add(enableAllLintersCheckBox);
        row1.add(new JLabel("enabled_linters (comma-separated if not all):"));
        enabledLintersField.setColumns(35);
        row1.add(enabledLintersField);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.add(new JLabel("too_many_parameters_limit:"));
        row2.add(tooManyParametersSpinner);
        row2.add(new JLabel("srp_lcom_threshold:"));
        row2.add(srpThresholdSpinner);

        JPanel row3 = new JPanel(new BorderLayout(8, 0));
        row3.add(configPathField, BorderLayout.CENTER);

        JPanel configButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton browseConfigButton = new JButton("Browse...");
        JButton loadConfigButton = new JButton("Load");
        JButton saveConfigButton = new JButton("Save");
        configButtons.add(browseConfigButton);
        configButtons.add(loadConfigButton);
        configButtons.add(saveConfigButton);
        row3.add(configButtons, BorderLayout.EAST);

        JPanel container = new JPanel(new GridLayout(0, 1, 0, 6));
        container.add(row1);
        container.add(row2);
        container.add(row3);

        configPanel.add(container, BorderLayout.CENTER);

        browseConfigButton.addActionListener(event -> chooseConfigPath());
        loadConfigButton.addActionListener(event -> loadConfigIntoControls(configPathField.getText().trim()));
        saveConfigButton.addActionListener(event -> saveConfigToFile(configPathField.getText().trim()));

        return configPanel;
    }

    private JScrollPane buildOutputPanel() {
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        outputArea.setLineWrap(false);
        return new JScrollPane(outputArea);
    }

    private void wireActions() {
        runButton.addActionListener(event -> runLintingInBackground());
        linterDropdown.addActionListener(event -> loadProjectPathOptions());
        enableAllLintersCheckBox.addActionListener(event -> updateEnabledLintersInputState());
    }

    private List<String> discoverAllRegisteredLinterNames() {
        List<Linter> allLinters = new LinterFactory().createLinters(LinterConfig.defaultConfig());
        List<String> names = new ArrayList<>();
        for (Linter linter : allLinters) {
            names.add(linter.getClass().getSimpleName());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private void loadLinterDropdownOptions() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement(SELECT_LINTER_PROMPT);
        for (String linterName : allRegisteredLinterNames) {
            model.addElement(linterName);
        }
        linterDropdown.setModel(model);
        linterDropdown.setSelectedIndex(0);
        loadProjectPathOptions();
    }

    private void loadConfigIntoControls(String configPathValue) {
        String resolvedConfigPath = resolveConfigPath(configPathValue);

        ConfigLoader loader = new ConfigLoader();
        LinterConfig config = loader.loadConfig(resolvedConfigPath);

        tooManyParametersSpinner.setValue(Math.max(1, config.getTooManyParametersLimit()));
        srpThresholdSpinner.setValue(Math.max(1, config.getSrpLcomThreshold()));

        if (config.isAllLintersEnabled()) {
            enableAllLintersCheckBox.setSelected(true);
            enabledLintersField.setText("");
        } else {
            enableAllLintersCheckBox.setSelected(false);
            enabledLintersField.setText(toDisplayEnabledLinters(config.getEnabledLinters()));
        }

        updateEnabledLintersInputState();

        if (isRegularFile(resolvedConfigPath)) {
            statusLabel.setText("Loaded config from: " + resolvedConfigPath);
        } else {
            statusLabel.setText("Config file not found. Using defaults in GUI controls.");
        }
    }

    private String toDisplayEnabledLinters(Set<String> normalizedNames) {
        if (normalizedNames == null || normalizedNames.isEmpty()) {
            return "";
        }

        List<String> resolved = new ArrayList<>();
        for (String registered : allRegisteredLinterNames) {
            if (normalizedNames.contains(normalizeName(registered))) {
                resolved.add(registered);
            }
        }

        if (resolved.isEmpty()) {
            resolved.addAll(normalizedNames);
        }

        return String.join(",", resolved);
    }

    private void updateEnabledLintersInputState() {
        boolean enableAll = enableAllLintersCheckBox.isSelected();
        enabledLintersField.setEnabled(!enableAll);
    }

    private void saveConfigToFile(String configPathValue) {
        String resolvedConfigPath = resolveConfigPath(configPathValue);

        Properties properties = new Properties();
        properties.setProperty("enabled_linters", buildEnabledLintersPropertyValue());
        properties.setProperty("too_many_parameters_limit", String.valueOf(tooManyParametersSpinner.getValue()));
        properties.setProperty("srp_lcom_threshold", String.valueOf(srpThresholdSpinner.getValue()));

        try {
            Path path = Path.of(resolvedConfigPath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            try (OutputStream out = Files.newOutputStream(path)) {
                properties.store(out, "Linter configuration");
            }

            statusLabel.setText("Saved config to: " + resolvedConfigPath);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save config: " + exception.getMessage(),
                    "Save Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildEnabledLintersPropertyValue() {
        if (enableAllLintersCheckBox.isSelected()) {
            return "all";
        }

        String raw = enabledLintersField.getText() == null ? "" : enabledLintersField.getText().trim();
        if (raw.isEmpty()) {
            return "all";
        }
        return raw;
    }

    private Linter buildSelectedLinterForRun() {
        String selectedName = getSelectedLinterName();
        if (selectedName == null) {
            return null;
        }

        int tooManyLimit = ((Number) tooManyParametersSpinner.getValue()).intValue();
        int srpThreshold = ((Number) srpThresholdSpinner.getValue()).intValue();

        LinterConfig thresholdConfig = new LinterConfig(null, tooManyLimit, srpThreshold);
        List<Linter> allLinters = new LinterFactory().createLinters(thresholdConfig);
        for (Linter linter : allLinters) {
            if (linter.getClass().getSimpleName().equals(selectedName)) {
                return linter;
            }
        }

        return null;
    }

    private String getSelectedLinterName() {
        Object item = linterDropdown.getSelectedItem();
        if (item == null) {
            return null;
        }
        String value = item.toString();
        if (SELECT_LINTER_PROMPT.equals(value)) {
            return null;
        }
        return value;
    }

    private void loadProjectPathOptions() {
        Linter selectedLinter = buildSelectedLinterForRun();

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement(SELECT_EXISTING_PATH_PROMPT);

        for (String relativePath : discoverCompatibleProjectFiles(selectedLinter)) {
            model.addElement(relativePath);
        }

        model.addElement(BROWSE_PATH_OPTION);
        projectPathsDropdown.setModel(model);
        projectPathsDropdown.setSelectedIndex(0);
    }

    private List<String> discoverCompatibleProjectFiles(Linter selectedLinter) {
        List<String> paths = new ArrayList<>();
        if (selectedLinter == null) {
            return paths;
        }

        try (Stream<Path> discovered = Files.walk(projectRoot)) {
            discovered
                    .filter(Files::isRegularFile)
                    .filter(path -> isProjectFileVisibleInDropdown(path, selectedLinter))
                    .map(projectRoot::relativize)
                    .map(Path::toString)
                    .map(value -> value.replace('\\', '/'))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(paths::add);
        } catch (IOException exception) {
            statusLabel.setText("Could not scan project files: " + exception.getMessage());
        }

        return paths;
    }

    private boolean isProjectFileVisibleInDropdown(Path filePath, Linter selectedLinter) {
        Path relativePath = projectRoot.relativize(filePath);
        if (relativePath.getNameCount() == 0) {
            return false;
        }

        String firstSegment = relativePath.getName(0).toString().toLowerCase(Locale.ROOT);
        boolean classFileLinter = LinterRunner.isClassFileLinter(selectedLinter);

        if (!classFileLinter
                && ("target".equals(firstSegment) || ".git".equals(firstSegment) || ".vscode".equals(firstSegment))) {
            return false;
        }
        if (classFileLinter && (".git".equals(firstSegment) || ".vscode".equals(firstSegment))) {
            return false;
        }

        String fileName = filePath.getFileName().toString().toLowerCase(Locale.ROOT);
        return classFileLinter ? fileName.endsWith(".class") : fileName.endsWith(".java");
    }

    private void addSelectedProjectPath() {
        Object selectedItem = projectPathsDropdown.getSelectedItem();
        if (selectedItem == null) {
            return;
        }

        String selectedPath = selectedItem.toString();
        if (SELECT_EXISTING_PATH_PROMPT.equals(selectedPath)) {
            return;
        }

        if (BROWSE_PATH_OPTION.equals(selectedPath)) {
            chooseInputPaths();
            projectPathsDropdown.setSelectedIndex(0);
            return;
        }

        LinkedHashSet<String> merged = parseCommaSeparatedPaths(inputPathsField.getText());
        merged.add(projectRoot.resolve(selectedPath).normalize().toString());
        inputPathsField.setText(String.join(", ", merged));
    }

    private void chooseInputPaths() {
        Linter selectedLinter = buildSelectedLinterForRun();
        boolean classFileLinter = selectedLinter != null && LinterRunner.isClassFileLinter(selectedLinter);

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle(classFileLinter
                ? "Select .class file(s) or folder(s)"
                : "Select .java file(s) or folder(s)");

        int choice = chooser.showOpenDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        LinkedHashSet<String> merged = parseCommaSeparatedPaths(inputPathsField.getText());
        for (File file : chooser.getSelectedFiles()) {
            merged.add(file.getAbsolutePath());
        }
        inputPathsField.setText(String.join(", ", merged));
    }

    private void chooseConfigPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Select linter.properties file");

        int choice = chooser.showOpenDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            configPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private String resolveConfigPath(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_CONFIG_PATH;
        }
        return value.trim();
    }

    private boolean isRegularFile(String filePath) {
        try {
            return Files.isRegularFile(Path.of(filePath));
        } catch (Exception ignored) {
            return false;
        }
    }

    private LinkedHashSet<String> parseCommaSeparatedPaths(String value) {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        if (value == null || value.trim().isEmpty()) {
            return paths;
        }

        String[] parts = value.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                paths.add(trimmed);
            }
        }
        return paths;
    }

    private void runLintingInBackground() {
        Linter selectedLinter = buildSelectedLinterForRun();
        String selectedLinterName = getSelectedLinterName();

        if (selectedLinter == null || selectedLinterName == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a linter first.",
                    "Missing Linter",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String inputPaths = inputPathsField.getText().trim();
        if (inputPaths.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please add at least one file or folder path.",
                    "Missing Input",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Linter finalSelectedLinter = selectedLinter;
        String configPathForDisplay = resolveConfigPath(configPathField.getText().trim());
        String enabledLintersValue = buildEnabledLintersPropertyValue();
        int tooManyLimit = ((Number) tooManyParametersSpinner.getValue()).intValue();
        int srpThreshold = ((Number) srpThresholdSpinner.getValue()).intValue();

        runButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        statusLabel.setText("Running " + selectedLinterName + "...");

        long startNanos = System.nanoTime();

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return executeLint(inputPaths, finalSelectedLinter, selectedLinterName,
                            configPathForDisplay, enabledLintersValue, tooManyLimit, srpThreshold);
                } catch (Exception exception) {
                    return "Error while running lint: " + exception.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    outputArea.setText(get());
                    outputArea.setCaretPosition(0);
                } catch (Exception exception) {
                    outputArea.setText("Failed to collect lint result: " + exception.getMessage());
                } finally {
                    runButton.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                    double elapsed = (System.nanoTime() - startNanos) / 1_000_000_000.0;
                    statusLabel.setText(String.format("Finished in %.2f s", elapsed));
                }
            }
        }.execute();
    }

    private String executeLint(String fileInput,
            Linter selectedLinter,
            String selectedLinterName,
            String configPathForDisplay,
            String enabledLintersValue,
            int tooManyLimit,
            int srpThreshold) {

        FileLoader fileLoader = new FileLoader();
        List<File> files = fileLoader.loadFiles(fileInput);
        if (files.isEmpty()) {
            return "No valid files found.";
        }

        List<File> compatibleFiles = filterFilesForSelectedLinter(files, selectedLinter);
        if (compatibleFiles.isEmpty()) {
            return "No compatible inputs found for " + selectedLinterName + "."
                    + System.lineSeparator()
                    + expectedInputMessage(selectedLinter);
        }

        StringBuilder output = new StringBuilder();
        output.append("Selected linter: ").append(selectedLinterName).append(System.lineSeparator());
        output.append("Config path (load/save): ").append(configPathForDisplay).append(System.lineSeparator());
        output.append("enabled_linters=").append(enabledLintersValue).append(System.lineSeparator());
        output.append("too_many_parameters_limit=").append(tooManyLimit).append(System.lineSeparator());
        output.append("srp_lcom_threshold=").append(srpThreshold).append(System.lineSeparator());
        output.append("Note: linter dropdown always shows all linters.").append(System.lineSeparator());
        output.append(expectedInputMessage(selectedLinter)).append(System.lineSeparator()).append(System.lineSeparator());
        output.append(selectedLinter.lint(compatibleFiles));
        return output.toString();
    }

    private List<File> filterFilesForSelectedLinter(List<File> files, Linter selectedLinter) {
        List<File> compatible = new ArrayList<>();
        boolean classFileLinter = LinterRunner.isClassFileLinter(selectedLinter);

        for (File file : files) {
            if (file.isDirectory()) {
                compatible.add(file);
                continue;
            }
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (classFileLinter && name.endsWith(".class")) {
                compatible.add(file);
            } else if (!classFileLinter && name.endsWith(".java")) {
                compatible.add(file);
            }
        }

        return compatible;
    }

    private String expectedInputMessage(Linter selectedLinter) {
        if (LinterRunner.isClassFileLinter(selectedLinter)) {
            return "Expected input: .class files or directories with compiled classes (for example, target/classes).";
        }
        return "Expected input: .java files or directories with Java source files.";
    }

    private String normalizeName(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}