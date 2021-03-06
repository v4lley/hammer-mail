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
package hammermail.core;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

public class Mail implements Serializable {

    private Integer id;
    private String sender;
    private String receiver;
    private String title;
    private String text;
    private Timestamp date;

    public Mail(Integer id, String sender, String receiver, String title, String text, Timestamp date) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.title = title;
        this.text = text;
        this.date = date;
    }
    
    @Override
    public String toString(){
        return getId() + ", " + getSender() + ", " + getReceiver() + ", " + getTitle() + ", " + getDate();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.sender);
        hash = 79 * hash + Objects.hashCode(this.receiver);
        hash = 79 * hash + Objects.hashCode(this.title);
        hash = 79 * hash + Objects.hashCode(this.text);
        hash = 79 * hash + Objects.hashCode(this.date);
        return hash;
    }
    
    @Override
    public boolean equals(Object m){
        if (m == null) return false;
        if (!(m instanceof Mail)) return false;
        Mail test = (Mail) m;
        return this.hashCode() == m.hashCode();
    }
    
    public boolean isDraft(){
        return this.date == null;
    }
    
    //Getter and setters
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }
    
}
