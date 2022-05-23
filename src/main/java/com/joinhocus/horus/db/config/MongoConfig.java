package com.joinhocus.horus.db.config;

import com.joinhocus.horus.config.Config;
import com.mongodb.ServerAddress;

import java.util.ArrayList;
import java.util.List;

@Config(directory = "database", name = "mongo")
public class MongoConfig {

    private final String username, password;
    private final List<String> addresses;

    public MongoConfig(
            String username,
            String password,
            List<String> addresses)
    {
        this.username = username;
        this.password = password;
        this.addresses = addresses;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getAddresses() {
        return addresses;
    }

    public List<ServerAddress> asServerAddresses() {
        List<ServerAddress> addresses = new ArrayList<>(getAddresses().size());
        for (String address : getAddresses()) {
            addresses.add(new ServerAddress(address));
        }

        return addresses;
    }

    @Override
    public String toString() {
        return "MongoConfig{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", addresses=" + addresses +
                '}';
    }
}
