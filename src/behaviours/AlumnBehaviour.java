package behaviours;

import java.util.EnumSet;

import agents.Alumn;
import agents.Availability;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.GroupChangeRequestConfirmationDenegationMessage;
import messages.GroupChangeRequestConfirmationMessage;
import messages.GroupChangeRequestDenegationMessage;
import messages.GroupChangeRequestMessage;
import messages.Message;
import messages.TeacherGroupChangeMessage;
import messages.TeacherGroupChangeRequestMessage;
import messages.TerminationConfirmationMessage;

public class AlumnBehaviour extends CyclicBehaviour {
    private static final long serialVersionUID = 6681845965215134904L;

    // We will ask for changes at most two times
    private static final int MAX_UNSUCCESFUL_REQUESTS = 2;

    private final Alumn alumn;

    /** To avoid sending multiple times the same request */
    private int pendingRepliesFromAlumns;
    private boolean pendingReplyFromTeacher;
    private AID pendingGroupConfirmationReplyFrom;
    private final int otherAlumns;
    private int batchRequestsAlreadyDone;

    public AlumnBehaviour(Alumn alumn) {
        super(alumn);
        this.alumn = alumn;
        this.batchRequestsAlreadyDone = 0;
        this.pendingRepliesFromAlumns = 0;
        this.pendingReplyFromTeacher = false;
        this.pendingGroupConfirmationReplyFrom = null;

        /// NOTE: The loop below was avoided initially.
        ///
        /// `otherAlumns` was initially set like:
        ///
        /// this.otherAlumns = alumn.findAgentsByType("alumn").length - 1;
        ///
        /// This was fine... sometimes. In other cases, over all when starting
        /// a lot of agents, it returned a bit less when used in
        /// `AlumnBehaviour`, which caused a ton of assertions to throw
        /// (quite reasonably).
        ///
        /// So... This is kind of hacky again (see src/agents/Alumn.java)
        /// but we must ensure the platform has registered all the alumns here
        /// before starting.
        ///
        /// NOTE: Possibly this is no longer needed since we directly use
        /// EXPECTED_ALUMN_COUNT,
        /// but...
        while (alumn.findAgentsByType("alumn").length != TeacherBehaviour.EXPECTED_ALUMN_COUNT) {
            alumn.blockingReceive(MessageTemplate.not(MessageTemplate.MatchAll()), 100);
        }

        this.otherAlumns = TeacherBehaviour.EXPECTED_ALUMN_COUNT - 1;

        System.err.println("INFO: " + alumn.getLocalName() + " got other " + this.otherAlumns
                + " alumns");
    }

    public boolean pendingReplies() {
        assert this.pendingRepliesFromAlumns >= 0 : this.alumn.getAID().toString();
        return this.pendingRepliesFromAlumns > 0 || this.pendingReplyFromTeacher
                || this.pendingGroupConfirmationReplyFrom != null;
    }

    @Override
    public void action() {
        assert this.alumn.getCurrentAssignedGroup() != null;
        // Send a message to the other alumns iff:
        // - We don't have any pending messages
        // - We aren't happy with our curent group
        // - We don't have pending replies, neither from the alumns nor from the
        // teacher
        // - We haven't asked more than MAX_UNSUCCESSFUL_REQUEST times
        if (this.alumn.isMessageQueueEmpty() && !this.alumn.isAvailableForCurrentAssignedGroup()
                && !this.pendingReplies()
                && this.batchRequestsAlreadyDone < AlumnBehaviour.MAX_UNSUCCESFUL_REQUESTS) {
            this.pendingRepliesFromAlumns += this.otherAlumns;
            this.batchRequestsAlreadyDone += 1;

            EnumSet<Availability> desiredAvailability;

            // If we've previously requested changes and no-one
            // replied, we try to change the group independently
            // If not we just try to stick with our groups
            if (this.batchRequestsAlreadyDone > 1) {
                desiredAvailability = Availability.ALL.clone();
                desiredAvailability.remove(this.alumn.getCurrentAssignedGroup());
            } else {
                desiredAvailability = this.alumn.getAvailability();
            }

            System.err.println("INFO: [" + this.myAgent.getLocalName()
                    + "] Requesting group changes to all alumns (try: "
                    + this.batchRequestsAlreadyDone + ")\n\tAvailability: " + desiredAvailability);

            this.alumn.sendMessageToType("alumn", new GroupChangeRequestMessage(
                    this.alumn.getCurrentAssignedGroup(), desiredAvailability));
        } else if (this.pendingReplyFromTeacher) {
            System.err.println("INFO: [" + this.myAgent.getLocalName() + "] Waiting for teacher");
            this.handleIncomingMessage(MessageTemplate
                    .and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                         MessageTemplate.MatchSender(this.alumn.getTeacherService())));
            // Then teacher alumns that might remain
        } else if (this.pendingGroupConfirmationReplyFrom != null) {
            this.handleIncomingMessage(MessageTemplate
                    .and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate
                            .or(MessageTemplate.MatchSender(this.pendingGroupConfirmationReplyFrom),
                                MessageTemplate.MatchSender(this.alumn.getTeacherService()))));
            // Attend then alumn replies
        } else if (this.pendingRepliesFromAlumns > 0) {
            System.err.println("INFO: [" + this.myAgent.getLocalName() + "] Waiting for replies");

            // NOTE: We try to get first the responses, but we can't avoid
            // handling the requests because if not we'll get interlocked
            while (true) {
                final ACLMessage next = this.alumn.blockingReceive(MessageTemplate
                        .and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate
                                .not(MessageTemplate.MatchSender(this.alumn.getTeacherService()))),
                                                                   1);
                if (next == null) {
                    break;
                }

                this.handleIncomingMessage(next);
            }

            if (this.pendingRepliesFromAlumns > 0) {
                this.handleIncomingMessage(MessageTemplate
                        .not(MessageTemplate.MatchSender(this.alumn.getTeacherService())));
                // And then everything else, including replies and all that
                // stuff
            }
        } else {
            System.err
                    .println("INFO: [" + this.myAgent.getLocalName() + "] Listening to everything");
            this.handleIncomingMessage();
        }
    }

    private void handleIncomingMessage() {
        this.handleIncomingMessage(MessageTemplate.MatchAll());
    }

    private void handleIncomingMessage(MessageTemplate template) {
        final ACLMessage msg = this.alumn.blockingReceive(template);
        this.handleIncomingMessage(msg);
    }

    private void handleIncomingMessage(ACLMessage msg) {
        final AID sender = msg.getSender();
        Message message = null;

        // Skip messages sent from myself
        if (sender.equals(this.alumn.getAID())) {
            return;
        }

        try {
            message = (Message) msg.getContentObject();
        } catch (final UnreadableException e) {
            System.err.println("WARNING: [" + this.myAgent.getAID()
                    + "] Error receiving message from " + sender + "; Performative: "
                    + msg.getPerformative());
            e.printStackTrace(System.err);
            return;
        }

        System.err.println("INFO: [" + this.myAgent.getLocalName() + "] ReceiveMessage ("
                + message.getType() + ", " + sender.getLocalName() + ")\n\tPendingReplies: "
                + this.pendingRepliesFromAlumns + "; FromTeacher: " + this.pendingReplyFromTeacher
                + "; GroupReply: "
                + (this.pendingGroupConfirmationReplyFrom == null ? "no"
                        : this.pendingGroupConfirmationReplyFrom.getLocalName())
                + "; RequestsAlreadyDone: " + this.batchRequestsAlreadyDone + "; CurrentGroup: "
                + this.alumn.getCurrentAssignedGroup());

        switch (message.getType()) {
            case TERMINATION_REQUEST:
                assert sender.equals(this.alumn.getTeacherService());
                this.alumn.replyTo(msg, new TerminationConfirmationMessage());
                this.alumn.doDelete();
                return;
            case GROUP_CHANGE_REQUEST:
                final GroupChangeRequestMessage groupChangeMessage = (GroupChangeRequestMessage) message;

                System.err.println("INFO: [" + this.alumn.getLocalName() + "] GROUP_CHANGE_REQUEST("
                        + sender.getLocalName() + ")\n\tDesired: "
                        + groupChangeMessage.getDesiredGroups() + "; Available: "
                        + this.alumn.getCurrentAssignedGroup());

                // If we are waiting for a change, or our group doesn't interest
                // the other alumn just deny the change
                if (this.pendingReplies() || !groupChangeMessage.getDesiredGroups()
                        .contains(this.alumn.getCurrentAssignedGroup())) {
                    this.alumn.replyTo(msg, new GroupChangeRequestDenegationMessage(
                            groupChangeMessage.getGroup()));
                    return;
                }

                // If our situation doesn't get worse, accept it, else reject it
                if (!this.alumn.isAvailableForCurrentAssignedGroup()
                        || this.alumn.getAvailability().contains(groupChangeMessage.getGroup())) {
                    this.alumn.replyTo(msg, new GroupChangeRequestConfirmationMessage(
                            groupChangeMessage.getGroup(), this.alumn.getCurrentAssignedGroup()));
                    this.pendingGroupConfirmationReplyFrom = sender;
                } else {
                    this.alumn.replyTo(msg, new GroupChangeRequestDenegationMessage(
                            groupChangeMessage.getGroup()));
                }
                return;
            // Our confirmation was rejected due to another confirmation
            // arriving before,
            // we just unflag ourselves
            case GROUP_CHANGE_REQUEST_CONFIRMATION_DENEGATION:
                this.pendingGroupConfirmationReplyFrom = null;
                return;
            case GROUP_CHANGE_REQUEST_DENEGATION:
                final GroupChangeRequestDenegationMessage denegationMessage = (GroupChangeRequestDenegationMessage) message;

                this.pendingRepliesFromAlumns -= 1;

                if (denegationMessage.getDeniedGroup() != this.alumn.getCurrentAssignedGroup()) {
                    System.err.println("INFO: [" + this.alumn.getLocalName()
                            + "] Received outdated denegation message, ignoring...");
                }

                return;
            case GROUP_CHANGE_REQUEST_CONFIRMATION:
                final GroupChangeRequestConfirmationMessage confirmationMessage = (GroupChangeRequestConfirmationMessage) message;

                this.pendingRepliesFromAlumns -= 1;

                if (confirmationMessage.getOldGroup() != this.alumn.getCurrentAssignedGroup()) {
                    System.err.println("INFO: [" + this.alumn.getLocalName()
                            + "] Received outdated confirmation message, ignoring...");
                    this.alumn.replyTo(msg, new GroupChangeRequestConfirmationDenegationMessage());
                    return;
                }

                if (this.pendingReplyFromTeacher) {
                    this.alumn.replyTo(msg, new GroupChangeRequestConfirmationDenegationMessage());
                    return;
                }

                // Make the change with the teacher
                this.alumn.sendMessage(this.alumn.getTeacherService(),
                                       new TeacherGroupChangeRequestMessage(this.alumn.getAID(),
                                               sender, confirmationMessage.getOldGroup(),
                                               confirmationMessage.getNewGroup()));
                this.pendingReplyFromTeacher = true;
                return;

            case TEACHER_GROUP_CHANGE:
                final TeacherGroupChangeMessage teacherGroupChangeMessage = (TeacherGroupChangeMessage) message;

                // if the sender isn't the teacher and we're implicated in the
                // change, it's the forwarded message from the other alumn we
                // should ignore
                if (!sender.equals(this.alumn.getTeacherService())
                        && (this.alumn.getAID().equals(teacherGroupChangeMessage.fromAlumn)
                                || this.alumn.getAID().equals(teacherGroupChangeMessage.toAlumn))) {
                    return;
                }

                // If we're some of the two parts involved
                if (this.alumn.getAID().equals(teacherGroupChangeMessage.fromAlumn)) {
                    // Since our group has changed, we might want to ask
                    // everyone again
                    this.batchRequestsAlreadyDone = 0;
                    this.pendingReplyFromTeacher = false;

                    assert this.pendingGroupConfirmationReplyFrom == null
                            || this.pendingGroupConfirmationReplyFrom
                                    .equals(teacherGroupChangeMessage.toAlumn);
                    this.pendingGroupConfirmationReplyFrom = null;

                    assert this.alumn.getCurrentAssignedGroup()
                            .equals(teacherGroupChangeMessage.fromGroup);

                    this.alumn.setCurrentAssignedGroup(teacherGroupChangeMessage.toGroup);

                    // Forward the message for the other alumns, just in case
                    // they're interested
                    this.alumn.sendMessageToType("alumn", teacherGroupChangeMessage);
                } else if (this.alumn.getAID().equals(teacherGroupChangeMessage.toAlumn)) {
                    // Since our group has changed, we might want to ask
                    // everyone again
                    this.batchRequestsAlreadyDone = 0;
                    this.pendingReplyFromTeacher = false;

                    assert this.pendingGroupConfirmationReplyFrom == null
                            || this.pendingGroupConfirmationReplyFrom
                                    .equals(teacherGroupChangeMessage.fromAlumn);
                    this.pendingGroupConfirmationReplyFrom = null;

                    assert this.alumn.getCurrentAssignedGroup()
                            .equals(teacherGroupChangeMessage.toGroup);

                    this.alumn.setCurrentAssignedGroup(teacherGroupChangeMessage.fromGroup);

                    // Forward the message for the other alumns, just in case
                    // they're interested
                    this.alumn.sendMessageToType("alumn", teacherGroupChangeMessage);
                } else {
                    // If we aren't happy with our group and not waiting for
                    // replies, check if the new groups interest us
                    if (!this.alumn.isAvailableForCurrentAssignedGroup()
                            && !this.pendingReplies()) {
                        final boolean isFromGroupInteresting = this.alumn.getAvailability()
                                .contains(teacherGroupChangeMessage.fromGroup);
                        final boolean isToGroupInteresting = this.alumn.getAvailability()
                                .contains(teacherGroupChangeMessage.toGroup);

                        // NOTE: we handle this two messages completely
                        // sequentially
                        // If some of the new groups interest us, send a
                        // concrete request for the new holder of the group.
                        if (isFromGroupInteresting) {
                            this.alumn.sendMessage(teacherGroupChangeMessage.toAlumn,
                                                   new GroupChangeRequestMessage(
                                                           this.alumn.getCurrentAssignedGroup(),
                                                           this.alumn.getAvailability()));
                            this.pendingRepliesFromAlumns += 1;
                            this.handleIncomingMessage(MessageTemplate
                                    .and(MessageTemplate
                                            .MatchSender(teacherGroupChangeMessage.toAlumn),
                                         MessageTemplate.MatchPerformative(ACLMessage.INFORM)));
                        }

                        if (isToGroupInteresting) {
                            this.alumn.sendMessage(teacherGroupChangeMessage.fromAlumn,
                                                   new GroupChangeRequestMessage(
                                                           this.alumn.getCurrentAssignedGroup(),
                                                           this.alumn.getAvailability()));
                            this.pendingRepliesFromAlumns += 1;
                            this.handleIncomingMessage(MessageTemplate
                                    .and(MessageTemplate
                                            .MatchSender(teacherGroupChangeMessage.fromAlumn),
                                         MessageTemplate.MatchPerformative(ACLMessage.INFORM)));
                        }

                        // If we don't have any pending reply from the teacher,
                        // that means all rejected us.
                        //
                        // Then we must ask them to change any group with us,
                        // just in case.
                        if (!this.pendingReplyFromTeacher) {
                            final EnumSet<Availability> anyAvailability = Availability.ALL.clone();
                            anyAvailability.remove(this.alumn.getCurrentAssignedGroup());

                            this.alumn.sendMessage(teacherGroupChangeMessage.toAlumn,
                                                   new GroupChangeRequestMessage(
                                                           this.alumn.getCurrentAssignedGroup(),
                                                           anyAvailability));
                            this.pendingRepliesFromAlumns += 1;

                            this.alumn.sendMessage(teacherGroupChangeMessage.fromAlumn,
                                                   new GroupChangeRequestMessage(
                                                           this.alumn.getCurrentAssignedGroup(),
                                                           anyAvailability));
                            this.pendingRepliesFromAlumns += 1;
                        }
                    }
                }

                return;
            default:
                System.err.println("ERROR: Unexpected message " + message.getType()
                        + " received in AlumnBehavior. Sender: " + sender + "; Receiver: "
                        + this.alumn.getAID());
                return;
        }
    }
}
