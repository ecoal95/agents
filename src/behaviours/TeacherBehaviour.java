package behaviours;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import agents.Availability;
import agents.Teacher;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import messages.FirstAssignationMessage;
import messages.InitMessage;
import messages.Message;
import messages.TeacherGroupChangeMessage;
import messages.TeacherGroupChangeRequestMessage;

/**
 * This is the main teacher behaviour. The behaviour encapsulates the teacher
 * logic, storing the availability days of the alumns.
 */
public class TeacherBehaviour extends CyclicBehaviour {
    private static final long serialVersionUID = 4979147830188132019L;
    private static int MAX_ALUMNS_PER_GROUP = 5;
    public static int EXPECTED_ALUMN_COUNT = 20;
    private static EnumSet<Availability> AVAILABLE_GROUPS = EnumSet
            .of(Availability.MONDAY, Availability.TUESDAY, Availability.THURDSDAY,
                Availability.FRIDAY);

    /**
     * This is the list of registered alumns and the group they're assigned to.
     */
    private final HashMap<AID, Availability> groups;

    private final Teacher teacher;

    private final Random random;

    private int alumnCount;

    public HashMap<AID, Availability> getGroups() {
        return new HashMap<AID, Availability>(this.groups);
    }

    public TeacherBehaviour(Teacher agent) {
        super(agent);
        this.groups = new HashMap<>();
        this.teacher = agent;
        this.random = new Random();
        this.alumnCount = 0;
    }

    private Availability firstAssignation(AID alumn) {
        if (this.groups.containsKey(alumn)) {
            System.err.println("WARN: Requested first assignation for already registered alumn "
                    + alumn);
            return this.groups.get(alumn);
        }

        // TODO: This could be more optimized, for example, having the
        // availabilityCount map cached

        // Get the count of the current availabilities
        final Map<Availability, Long> availabilityCount = this.groups.values().stream()
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()));

        // Get the current available groups
        final List<Availability> availableGroups = TeacherBehaviour.AVAILABLE_GROUPS.stream()
                .filter(a -> availabilityCount.getOrDefault(a,
                                                            0l) < TeacherBehaviour.MAX_ALUMNS_PER_GROUP)
                .collect(Collectors.toList());

        // Pick a random one
        final Availability result = availableGroups
                .get(this.random.nextInt(availableGroups.size()));
        this.groups.put(alumn, result);

        return result;
    }

    private void printStartingStatus() {
        System.out.println("\n\n\n========================================");
        System.out.println("Starting");
        System.out.println("========================================\n\n");

        final SortedSet<AID> keys = new TreeSet<AID>(this.groups.keySet());

        for (final AID key : keys) {
            final Availability group = this.groups.get(key);
            System.out.println(" * " + key.getLocalName() + ": " + group);
        }

        System.out.println("\n\n\n");
    }

    @Override
    public void action() {
        // We have to use a timeout to allow the WakerBehaviour to take place
        final ACLMessage msg = this.teacher.blockingReceive(100);

        if (msg == null) {
            return;
        }

        final AID sender = msg.getSender();
        Message message = null;

        try {
            message = (Message) msg.getContentObject();
        } catch (final UnreadableException e) {
            System.err.println("[" + this.myAgent.getAID() + "] Error receiving message from "
                    + sender);
            e.printStackTrace(System.err);
            return;
        }

        System.err.println("INFO: [" + this.myAgent.getAID() + "] ReceiveMessage ("
                + message.getType() + ")");

        switch (message.getType()) {
            case FIRST_ASSIGNATION_REQUEST:
                // TODO: This should maybe be an INFORM message, with reply
                this.teacher.replyTo(msg,
                                     new FirstAssignationMessage(this.firstAssignation(sender)));

                this.alumnCount += 1;

                assert this.alumnCount <= TeacherBehaviour.EXPECTED_ALUMN_COUNT;

                if (TeacherBehaviour.EXPECTED_ALUMN_COUNT == this.alumnCount) {
                    this.printStartingStatus();
                    this.teacher.sendMessageToType("alumn", new InitMessage());
                }

                return;
            case TEACHER_GROUP_CHANGE_REQUEST:
                final TeacherGroupChangeRequestMessage requestMessage = (TeacherGroupChangeRequestMessage) message;

                assert requestMessage.fromAlumn.equals(sender);

                // Ensure those alumns haven't been changed previously
                assert this.groups.get(requestMessage.fromAlumn) == requestMessage.fromGroup
                        && this.groups.get(requestMessage.toAlumn) == requestMessage.toGroup;

                System.out.println("Group change: " + requestMessage.fromAlumn.getLocalName()
                        + " <-> " + requestMessage.toAlumn.getLocalName());

                this.groups.put(requestMessage.fromAlumn, requestMessage.toGroup);
                this.groups.put(requestMessage.toAlumn, requestMessage.fromGroup);

                // We don't send the message to every alumn, we send it to both
                // implicated alumns, and they'll take care to forward it to
                // everyone else
                final Message content = new TeacherGroupChangeMessage(requestMessage.fromAlumn,
                        requestMessage.toAlumn, requestMessage.fromGroup, requestMessage.toGroup);

                this.teacher.replyTo(msg, content);
                this.teacher.sendMessage(requestMessage.toAlumn, content, ACLMessage.INFORM);
                return;
            default:
                System.err.println("ERROR: Unexpected message of type " + message.getType()
                        + " received in TeacherBehaviour. Sender: " + sender + "; Receiver: "
                        + this.teacher.getAID());
                return;
        }
    }
}
