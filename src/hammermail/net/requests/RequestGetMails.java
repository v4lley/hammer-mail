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
package hammermail.net.requests;

import java.sql.Timestamp;
import java.util.Date;

/**
 * The client wants a list of new mails.
 */
public class RequestGetMails extends RequestBase {
    
    private Timestamp lastMailDate;
    
    /**
     * Create a Request
     * @param lastMailDate the last mail Date. The server will respond with only more recent mails.
     * If you want all mails, just set this to null.
     */
    public RequestGetMails (Timestamp lastMailDate){
        this.lastMailDate = lastMailDate;
    }

    public Timestamp getLastMailDate() {
        return lastMailDate;
    }

    public void setLastMailDate(Timestamp lastMailDate) {
        this.lastMailDate = lastMailDate;
    }
    
    
    
}
