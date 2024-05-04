import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;

public class RecipeRecommendationSystem extends JFrame {
    private Connection connection;
    private JTextArea recipeTextArea;
    private JComboBox<String> foodItemsComboBox;
    private JButton recommendButton;

    public RecipeRecommendationSystem() {
        super("Recipe Recommendation System");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        recipeTextArea = new JTextArea();
        recipeTextArea.setEditable(false);

        foodItemsComboBox = new JComboBox<>();
        recommendButton = new JButton("Recommend Recipes");

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(foodItemsComboBox, BorderLayout.NORTH);
        panel.add(recommendButton, BorderLayout.CENTER);

        add(recipeTextArea, BorderLayout.CENTER);
        add(panel, BorderLayout.NORTH);

        recommendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                recommendRecipes();
            }
        });

        try {
            // Connect to MySQL database
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/recipe_manager_db", "root", "12345");
            loadFoodItems();
            String input = JOptionPane.showInputDialog(this, "Enter ingredients separated by commas:");
            if (input != null && !input.isEmpty()) {
                String[] ingredients = input.split(",");
                recommendRecipes(ingredients);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to connect to database");
        }
    }

    private void loadFoodItems() {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT DISTINCT ingredient_name FROM ingredients");
            while (resultSet.next()) {
                foodItemsComboBox.addItem(resultSet.getString("ingredient_name"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void recommendRecipes() {
        String selectedIngredient = (String) foodItemsComboBox.getSelectedItem();
        if (selectedIngredient == null) {
            JOptionPane.showMessageDialog(this, "Please select an ingredient");
            return;
        }
        recommendRecipes(new String[]{selectedIngredient});
    }

    private void recommendRecipes(String[] ingredients) {
        if (ingredients == null || ingredients.length == 0) {
            JOptionPane.showMessageDialog(this, "Please enter at least one ingredient.");
            return;
        }

        try {
            StringBuilder queryBuilder = new StringBuilder("SELECT DISTINCT recipe_name FROM recipes r INNER JOIN recipe_ingredients ri ON r.recipe_id = ri.recipe_id INNER JOIN ingredients i ON ri.ingredient_id = i.ingredient_id WHERE i.ingredient_name IN (");
            for (int i = 0; i < ingredients.length; i++) {
                queryBuilder.append("?");
                if (i < ingredients.length - 1) {
                    queryBuilder.append(",");
                }
            }
            queryBuilder.append(")");

            PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.toString());
            for (int i = 0; i < ingredients.length; i++) {
                preparedStatement.setString(i + 1, ingredients[i].trim());
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            ArrayList<String> recommendedRecipes = new ArrayList<>();
            while (resultSet.next()) {
                recommendedRecipes.add(resultSet.getString("recipe_name"));
            }

            if (recommendedRecipes.isEmpty()) {
                recipeTextArea.setText("No recipes found with the given ingredients.");
            } else {
                StringBuilder recipesText = new StringBuilder();
                recipesText.append("Recommended Recipes with the given ingredients:\n");
                for (String recipe : recommendedRecipes) {
                    recipesText.append("- ").append(recipe).append("\n");
                }
                recipeTextArea.setText(recipesText.toString());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to recommend recipes");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new RecipeRecommendationSystem().setVisible(true);
            }
        });
    }
}
