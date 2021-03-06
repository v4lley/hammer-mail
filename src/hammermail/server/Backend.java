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

import hammermail.core.Globals;
import hammermail.core.Utils;
import hammermail.net.requests.*;
import hammermail.net.responses.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static hammermail.net.responses.ResponseError.ErrorType.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import javafx.beans.property.SimpleStringProperty;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

public class Backend {

    private ExecutorService exec;
    private ServerSocket serverSocket;
    private final SimpleStringProperty logText = new SimpleStringProperty("");

    @SuppressWarnings("empty-statement")
    public void startServer() {
        try {
            logAction("Creating sockets...");
            serverSocket = new ServerSocket(Globals.HAMMERMAIL_SERVER_PORT_NUMBER);
            logAction("Creating Thread pool...");
            exec = Executors.newFixedThreadPool(Globals.HAMMERMAIL_SERVER_NUM_THREAD);
            logAction("Sockets and Threads created. Server starting...");

            while (serverLoop());
        } catch (IOException ex) {
            Logger.getLogger(Backend.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This method will loop until the server is working
     */
    boolean serverLoop() {
        logAction("-------------- Waiting for request... --------------");

        try {
            Socket incoming = serverSocket.accept();
            handleNewRequest(incoming);
        } catch (IOException ex) {
            return false;
        }

        return true;
    }

    void handleNewRequest(Socket clientSocket) {
        Task task = new Task(clientSocket, this);
        exec.execute(task);
    }

    /**
     * Stops threads and socket. Use it to stop the server
     */
    void stopServer() {
        logAction("Stopping server...");

        //Stopping threads
        exec.shutdown();
        try {
            exec.awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logAction(e.getMessage());
        }

        //Closing socket
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                Logger.getLogger(Backend.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public synchronized void logAction(String log) {

        String oldLog = logText.get() + log + "\n";
        if (Utils.countLines(log) > Globals.HAMMERMAIL_SERVER_MAX_LOG_LINES) {
            oldLog = oldLog.substring(oldLog.indexOf('\n'));
        }

        logText.set(oldLog);
    }

    public String GetLog() {
        return logText.get();
    }

    public SimpleStringProperty logProperty() {
        return logText;
    }
}

/**
 * Represents a single task of the HammerMail server
 *
 */
class Task implements Runnable {

    private final Socket clientSocket;
    private final Backend backend;

    public Task(Socket clientSocket, Backend backend) {
        this.clientSocket = clientSocket;
        this.backend = backend;
    }

    @Override
    public void run() {
        if (clientSocket != null) {
            handleClient(clientSocket);
        }
    }

    void handleClient(Socket clientSocket) {
        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        try {
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            Object obj = in.readObject();

            if (!(obj instanceof RequestBase)) {
                logAction("Error: received object of type: " + obj.getClass());
            } else {
                ResponseBase response = handleRequest((RequestBase) obj);
                out.writeObject(response);
            }
        } catch (IOException ex) {
            Logger.getLogger(Backend.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Task.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Backend.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Given a valid request, generates a proper response
     *
     * @param request
     * @return
     */
    ResponseBase handleRequest(RequestBase request) {
        if (!request.isAuthenticationWellFormed()) {
            logAction(request.getUsername() + " - " + request.getPassword() + " has invalid username/password!");
            return new ResponseError(ResponseError.ErrorType.INCORRECT_AUTHENTICATION);
        }

        if (request instanceof RequestSignUp) {
            return handleSignUp((RequestSignUp) request);
        } else if (request instanceof RequestSendMail) {
            return handleSendMail((RequestSendMail) request);
        } else if (request instanceof RequestGetMails) {
            return handleGetMails((RequestGetMails) request);
        } else if (request instanceof RequestDeleteMails) {
            return handleDeleteMails((RequestDeleteMails) request);
        } else {
            return null;
        }
    }

    ResponseBase handleSignUp(RequestSignUp request) {
        Database db = new Database(false);
        try {
            if (db.isUser(request.getUsername())) {
                logAction("New user tried to signup, but username is taken: " + request.getUsername());
                return new ResponseError(SIGNUP_USERNAME_TAKEN);
            } else {
                logAction("New user signed up: " + request.getUsername());
                db.addUser(request.getUsername(), request.getPassword());
            }

        } catch (SQLException ex) {
            if (((SQLiteException) ex).getResultCode().equals(SQLiteErrorCode.SQLITE_BUSY)) {
                return new ResponseRetrieve();
            } else {
                return new ResponseError(INTERNAL_ERROR);
            }
        } finally {
            db.release();
        }
        return new ResponseSuccess();
    }

    ResponseBase handleSendMail(RequestSendMail request) {
        Database db = new Database(false);
        try {
            if (db.checkPassword(request.getUsername(), request.getPassword())) {
                if (request.IsMailWellFormed()) {
                    String rec = (request.getMail().getReceiver());
                    String[] receivers = rec.split(";");

                    if (receivers.length == 1) {
                        if (!db.isUser(receivers[0])) {
                            logAction("Mail sent from " + request.getMail().getSender() + " but " + request.getMail().getReceiver() + " do not exist!");
                            return new ResponseError(SENDING_TO_UNEXISTING_USER);
                        } else {
                            logAction("Mail sent from " + request.getMail().getSender() + " to " + request.getMail().getReceiver());
                            db.addMail(request.getMail());
                            return new ResponseMailSent(receivers[0], "");
                        }
                    }
                    
                    rec = "";
                    String refused = "";

                    for (int i = 0; i < receivers.length; i++) {
                        receivers[i] = receivers[i].trim();
                        if (db.isUser(receivers[i])) {
                            rec = rec + ";" + receivers[i];
                        } else {
                            refused = refused + ";" + receivers[i];
                        }
                    }
                    request.getMail().setReceiver(rec.substring(1));
                    db.addMail(request.getMail());
                    db.release();
                    logAction("Mail sent from " + request.getMail().getSender() + " to " + request.getMail().getReceiver() + " but some refused: " + refused);
                    return new ResponseMailSent(rec, refused);
                } else {
                    db.release();
                    return new ResponseError(SENDING_INVALID_MAIL);
                }

            } else {
                db.release();
                logAction(request.getUsername() + " - " + request.getPassword() + " has invalid username/password!");
                return new ResponseError(INCORRECT_AUTHENTICATION);
            }
        } catch (SQLException ex) {
            if (((SQLiteException) ex).getResultCode().equals(SQLiteErrorCode.SQLITE_BUSY)) {
                return new ResponseRetrieve();
            } else {
                return new ResponseError(INTERNAL_ERROR);
            }
        } finally {
            db.release();
        }
    }

    ResponseBase handleGetMails(RequestGetMails request) {
        Database db = new Database(false);
        Timestamp time = request.getLastMailDate();
        try {
            if (db.checkPassword(request.getUsername(), request.getPassword())) {
                ResponseMails response = new ResponseMails(db.getReceivedMails(request.getUsername(), time),
                        db.getSentMails(request.getUsername(), time));
                db.release();
                logAction(request.getUsername() + " downloaded " + (response.getSentMails().size() + response.getReceivedMails().size()) + " mails.");
                return response;
            } else {
                db.release();
                logAction(request.getUsername() + " - " + request.getPassword() + " has invalid username/password!");
                return new ResponseError(INCORRECT_AUTHENTICATION);
            }
        } catch (SQLException ex) {
            if (((SQLiteException) ex).getResultCode().equals(SQLiteErrorCode.SQLITE_BUSY)) {
                return new ResponseRetrieve();
            } else {
                return new ResponseError(INTERNAL_ERROR);
            }
        }
    }

    private ResponseBase handleDeleteMails(RequestDeleteMails request) {
        Database db = new Database(false);
        try {
            for (Integer mailID : request.getMailsIDsToDelete()) {
                db.removeMail(mailID, request.getUsername());
                logAction(request.getUsername() + " deleted a mail with id: " + mailID);
            }
            db.release();
            return new ResponseSuccess();
        } catch (SQLException ex) {
            if (((SQLiteException) ex).getResultCode().equals(SQLiteErrorCode.SQLITE_BUSY)) {
                return new ResponseRetrieve();
            } else {
                return new ResponseError(INTERNAL_ERROR);
            }
        } finally {
            db.release();
        }
    }

    public synchronized void logAction(String log) {
        backend.logAction(log);
    }
}
