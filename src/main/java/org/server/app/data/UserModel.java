package org.server.app.data;

import com.mongodb.lang.NonNull;
import dev.morphia.annotations.*;
import org.bson.types.ObjectId;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

@Entity("users")
public class UserModel {
    @Id
    private ObjectId id;
    @NonNull
    private String name;
    @NonNull
    private String lastName;
    @NonNull
    private String email;
    @NonNull
    private String password;

    private boolean isAdmin = false;

    public UserModel(@NonNull String name, @NonNull String lastName, @NonNull String email, @NonNull String password) throws NoSuchAlgorithmException {
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.password = this.hashPassword(password, new byte[16]);

    }

    public UserModel(ObjectId id, @NonNull String name, @NonNull String lastName, @NonNull String email, @NonNull String password) throws NoSuchAlgorithmException {
        this.id = id;
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

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getLastName() {
        return lastName;
    }

    public void setLastName(@NonNull String lastName) {
        this.lastName = lastName;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    public void setEmail(@NonNull String email) {
        this.email = email;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    @NonNull
    public String getPassword() {
        return password;
    }

    public void setPassword(@NonNull String password) {
        this.password = password;
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
}
