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
package hammermail.client;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import hammermail.core.EmptyMail;
import hammermail.core.Mail;
import static hammermail.core.Utils.sendRequest;
import hammermail.net.requests.*;
import hammermail.net.responses.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class UILoginController implements Initializable {

    private Stage s;
        
    @FXML
    private TextField username;

    @FXML
    private PasswordField password;

    @FXML 
    private Text loginfailure;

    @FXML
    private void handleLogin(ActionEvent event) {
        //TODO: LOAD FILE IF EXISTS, CREATE IT AND FILL IT IF IT DOESN'T EXISTS
        try {
            if (username.getText().isEmpty() || password.getText().isEmpty())
                spawnLogin();
            else {
                ResponseBase response = composeAndSendGetMail();
                if (response instanceof ResponseError){
                    loginfailure.setFill(Color.rgb(255,0,0));
                    loginfailure.setText("Incorrect Authentication");

                } else if (response instanceof ResponseMails){
                    updateModelReqMail(response);
                    spawnHome();
                    //Move in the UI controller
                    
                } 
            }
        } catch (ClassNotFoundException | IOException ex){
            System.out.println(ex.getMessage());
            // set the response to error internal_error
        } 
    }  
    
    

    @FXML
    private void handleSignup(ActionEvent event){
        
        try {
            if (username.getText().isEmpty() || password.getText().isEmpty())
                spawnLogin();
        
            else {
                //Compose request and send
                String user = username.getText();
                RequestSignUp requestSignUp = new RequestSignUp();
                requestSignUp.SetAuthentication(user, password.getText());
                ResponseBase response = sendRequest(requestSignUp);

                
                //Username taken
                if (response instanceof ResponseError){
                    loginfailure.setFill(Color.rgb(255,0,0));
                    loginfailure.setText("Username taken");

                } else if (response instanceof ResponseSuccess){
                    response = composeAndSendGetMail();
//                    Model.getModel().createJson(user);
                    if (response instanceof ResponseError)
                        spawnLogin();

                    else if (response instanceof ResponseMails){
                        updateModelReqMail(response);
                        spawnHome();
                    }
                    
                }
            }
        } catch (ClassNotFoundException | IOException classEx){
            // set the response to error internal_error
        }       
 
    }
     
    //CONVERT INTO DIFF
    private void updateModelReqMail(ResponseBase response){
        Model.getModel().setCurrentUser(username.getText(), password.getText());
        Model.getModel().createJson(username.getText());
        //to fill with mails on the database
        
        //I don't know where to put this...
        Timestamp ts = Model.getModel().calculateLastMailStored();
        
        List<Mail> received = ((ResponseMails) response).getReceivedMails();
        List<Mail> sent = ((ResponseMails) response).getSentMails();
        //this way drafts will be overridden
        received.forEach(Model.getModel()::storeMail);
        sent.forEach(Model.getModel()::storeMail);
        
        Model.getModel().dispatchMail();
    }
    
    
    
    //maybe event argument will be eliminated..
    /**
    * Method to spawn a new Login view.
    * @author Gaetano
    * @param event:
    */
    private void spawnLogin(){
         //spawn a new login view
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("UIlogin.fxml"));
            Parent root = fxmlLoader.load();       
            UILoginController loginController = fxmlLoader.getController();
            loginController.init(s);
            s.close();

            Scene scene = new Scene(root);
            s.setScene(scene);
            s.setTitle("HammerMail - Login");
            s.show();
         } catch (IOException e){
             //TODO
         }
    }
    
    private void spawnHome(){
        try {
            FXMLLoader uiLoader = new FXMLLoader();
            uiLoader.setLocation(getClass().getResource("UI.fxml"));
            Parent root;  
            root = uiLoader.load();
            UIController uiController = uiLoader.getController();
            s.close(); //close login view
            Stage newstage = new Stage();
            newstage.setTitle("HammerMail - Home");
            newstage.setScene(new Scene(root));
            uiController.init(newstage);
            newstage.show();
        } catch (IOException ex) {
            spawnLogin();
        }
    }
    
    private ResponseBase composeAndSendGetMail() throws ClassNotFoundException, IOException{
        //the RequestGetMail will be called with the Date argument from JSON    
        Timestamp lastMail = Model.getModel().getLastMailStored();
        RequestGetMails requestGetMail = null;
        if (lastMail == null){
            requestGetMail = new RequestGetMails(new Timestamp(0));
            Model.getModel().setLastMailStored(new Timestamp(System.currentTimeMillis()));
        }
        else
            requestGetMail = new RequestGetMails(lastMail);
        requestGetMail.SetAuthentication(username.getText(), password.getText());
        return sendRequest(requestGetMail);
    }
    
    
    
    public void init(Stage stage){ 
        this.s = stage;
        Model.getModel().setCurrentMail(new EmptyMail()); //This is the first Model call, it will exectute the Model constructor
    }                                                       //TODO move it to the updateModelReqMail
        

    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    

    
}
