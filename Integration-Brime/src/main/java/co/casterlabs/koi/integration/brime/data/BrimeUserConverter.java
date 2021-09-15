package co.casterlabs.koi.integration.brime.data;

import co.casterlabs.brimeapijava.realtime.BrimeChatMessage.BrimeMessageSender;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import lombok.NonNull;

public class BrimeUserConverter implements UserConverter<BrimeMessageSender> {
    private static final @Getter BrimeUserConverter instance = new BrimeUserConverter();

    @Override
    public @NonNull User transform(@NonNull BrimeMessageSender user) {
        User asUser = new User(UserPlatform.BRIME);

        asUser.setDisplayname(user.getDisplayname());
        asUser.setUsername(user.getUsername());
        asUser.setId(user.getXid());
//        asUser.setBadges(new HashSet<>(user.getBadges()));
        // asUser.setRoles(roles); // TODO
        asUser.setColor(user.getColor());
        asUser.setImageLink("https://assets.casterlabs.co/brime/default-profile-picture.png");

//        if ((user.getChannels() != null) && !user.getChannels().isEmpty()) {
//            asUser.setChannelId(user.getChannels().get(0));
//        }

        return asUser;
    }

    @Override
    public User get(@NonNull String username) {
        return null;
    }

}
