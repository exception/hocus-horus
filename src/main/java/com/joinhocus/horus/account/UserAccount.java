package com.joinhocus.horus.account;

import com.google.common.base.Preconditions;
import com.joinhocus.horus.http.routes.account.create.model.CreateAccountRequest;
import com.joinhocus.horus.misc.follow.Followable;
import lombok.Getter;
import lombok.ToString;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Getter
@ToString(of = {"id", "name"})
public class UserAccount implements Followable {

    private final Document document;

    private final ObjectId id;
    private final String name;
    private final UserNames usernames;
    private final String email, password;
    private boolean finishedAccountSetup;
    private AccountType accountType;
    private final String avatar;

    private final Document extra;

    public UserAccount(@NotNull Document document) {
        this.document = document;

        this.id = document.getObjectId("_id");
        this.name = document.getString("name");
        this.email = document.getString("email");
        this.password = document.getString("password");
        if (document.containsKey("type")) {
            this.finishedAccountSetup = true;
            this.accountType = AccountType.valueOf(document.getString("type"));
        }
        this.avatar = document.getString("avatar");

        Document userNames = document.get("user", Document.class);
        this.usernames = new UserNames(
                userNames.getString("name"),
                userNames.getString("display")
        );

        this.extra = document.get("extra", new Document());
    }

    public static Document generateAccount(CreateAccountRequest request) {
        char[] password = request.getPassword().toCharArray();
        try {
            String hash = AccountAuth.ARGON_2.hash(
                    22,
                    65536,
                    1,
                    password
            );

            return new Document()
                    .append("name", request.getName())
                    .append("email", request.getEmail().toLowerCase())
                    .append("user", new Document()
                            .append("name", request.getUsername().toLowerCase()) // store it for case matching
                            .append("display", request.getUsername())
                    )
                    .append("password", hash)
                    .append("acceptedCode", request.getInviteCode()); // store it because we need to get info from it during other steps
        } finally {
            AccountAuth.ARGON_2.wipeArray(password);
        }
    }

    public boolean passwordsMatch(@NotNull String remote) {
        Preconditions.checkNotNull(this.password);
        return AccountAuth.ARGON_2.verify(this.password, remote.toCharArray());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAccount account = (UserAccount) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
