package presentation;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class LinterGuiMain {

    private LinterGuiMain() {
    }

    public static void main(String[] args) {
        launch();
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            useSystemLookAndFeel();
            new LinterGuiFrame().setVisible(true);
        });
    }

    private static void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to default look and feel
        }
    }
}
