package org.server.app.data;

import com.mongodb.lang.NonNull;
import dev.morphia.annotations.*;
import org.bson.types.ObjectId;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

@Entity("users")
public class UserModel {
    @Id
    private ObjectId id;
    @NonNull
    public String name;
    @NonNull
    public String lastName;
    @NonNull
    public String email;
    @NonNull
    public String password;

    public boolean isAdmin = false;

    public Date createdAt;
    public Date editedAt;


    public UserModel(@NonNull String name, @NonNull String lastName, @NonNull String email, @NonNull String password) throws NoSuchAlgorithmException {
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.password = this.hashPassword(password, new byte[16]);

    }


    public String getId() {
        return id.toString();
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    private String hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        byte[] hashedPassword = md.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hashedPassword);
    }

    public boolean verifyPassword(String password) throws NoSuchAlgorithmException {
        String newHash = hashPassword(password, new byte[16]);
        return Arrays.equals(this.password.getBytes(), newHash.getBytes());
    }

    @PrePersist
    public void prePersist() {
        createdAt = (createdAt == null) ? new Date() : createdAt;
        editedAt = (editedAt == null) ? createdAt : new Date();
    }


}
