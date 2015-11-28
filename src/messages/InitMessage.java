package messages;

public class InitMessage extends TeacherToAlumnMessage {
    private static final long serialVersionUID = -8903852117924723102L;

    public InitMessage() {
        super(MessageType.INIT);
    }
}
