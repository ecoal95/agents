package messages;

public class GroupChangeRequestConfirmationDenegationMessage extends AlumnToAlumnMessage {
    private static final long serialVersionUID = 1561717792141947743L;

    public GroupChangeRequestConfirmationDenegationMessage() {
        super(MessageType.GROUP_CHANGE_REQUEST_CONFIRMATION_DENEGATION);
    }
}
