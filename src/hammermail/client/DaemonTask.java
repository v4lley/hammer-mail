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

import hammermail.core.Mail;
import hammermail.core.User;
import static hammermail.core.Utils.sendRequest;
import hammermail.net.requests.RequestGetMails;
import hammermail.net.responses.ResponseBase;
import hammermail.net.responses.ResponseMails;
import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

public class DaemonTask implements Runnable {

    private Socket clientSocket;
    private final User user = Model.getModel().getCurrentUser();

    public DaemonTask() {
        if (clientSocket == null) {
            clientSocket = new Socket();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Timestamp time = Model.getModel().getLastRequestTime();
                RequestGetMails requestGetMail = new RequestGetMails(time);
                Model.getModel().setLastRequestTime(new Timestamp(System.currentTimeMillis()));
                requestGetMail.SetAuthentication(user.getUsername(), user.getPassword());
                ResponseBase response;
                try {
                    response = sendRequest(requestGetMail);
                    List<Mail> received = ((ResponseMails) response).getReceivedMails();
                    List<Mail> sent = ((ResponseMails) response).getSentMails();

                    if (received.size() > 0) {
                        Platform.runLater(() -> Model.getModel().addMultiple(received));
                        Platform.runLater(() -> Model.getModel().addNotify(received));
                        System.out.println("You have " + received.size() + " new mails!");
                    }

                    if (sent.size() > 0) {
                        Platform.runLater(() -> Model.getModel().addMultiple(sent));
                    }

                } catch (IOException ex) {
                    System.out.println("ERROR: Daemon was unable to contact server");
                }

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    System.out.println("ERROR: Daemon task interrupted");
                }
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(UILoginController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
