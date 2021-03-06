/*
 * Copyright (C) 2018
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package hammermail.server;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class UI extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException, InterruptedException {
        primaryStage.setTitle("Hammermail backend");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("UI.fxml"));
        primaryStage.setScene(new Scene(loader.load()));
        primaryStage.setMaximized(true);
        primaryStage.getIcons().add(new Image( "hammermail/resources/hammermail.png" )); 


        UIController uiController = loader.getController();
        UIModel model = new UIModel();
        uiController.initModel(model);

        primaryStage.show();
        
        Backend backend = startServer();
        backend.logProperty().addListener((value, oldValue, newValue) -> {
            Platform.runLater(() -> {
                model.setLog(newValue);
            });
        });
        
        primaryStage.setOnCloseRequest(e -> {
            backend.stopServer();
        });
         
        //Uncomment to test with DummyClient
        /*
        for (int i = 0; i < 100; i++)
        {
            testServer();
        }
        */
    }

    Backend startServer() {
        Backend backend = new Backend();
        Thread t = new Thread(() -> {
            backend.startServer();
        });
        t.start();
        return backend;
    }

    void testServer() {
        System.out.println("Testing Server!");
        Thread t = new Thread(() -> {
            try {
                System.out.println("Testing in 3 seconds!");
                Thread.sleep(1000);
                System.out.println("Testing in 2 seconds!");
                Thread.sleep(1000);
                System.out.println("Testing in 1 second!");
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                DummyClient c = new DummyClient();

            } catch (InterruptedException ex) {
                Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        t.start();
    }

    public static void main(String[] args) throws ParseException, InterruptedException, SQLException {
        Database d = new Database(false);
        d.addUser("tano", "a");
        d.addUser("marco", "a");
        d.addUser("omar", "a");
        d.addUser("andrea", "a");
        d.release();

        launch(args);    
    }

}
